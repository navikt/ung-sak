package no.nav.ung.sak.domene.behandling.steg.ungdomsprogramkontroll;

import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;

import java.util.*;

public class EtterlysningForEndretProgramperiodeUtleder {

    public static final Set<EtterlysningStatus> VENTER_STATUSER = Set.of(EtterlysningStatus.VENTER, EtterlysningStatus.OPPRETTET);


    static Resultat finnEndretProgramperiodeResultat(EndretUngdomsprogramEtterlysningInput endretUngdomsprogramEtterlysningInput, BehandlingReferanse behandlingReferanse) {
        // Ekstra validering for å sjekke at det kun er én programperiode i grunnlaget.
        final var programperioder = endretUngdomsprogramEtterlysningInput.gjeldendePeriodeGrunnlag().getUngdomsprogramPerioder().getPerioder();
        if (programperioder.size() > 1) {
            throw new IllegalStateException("Støtter ikke flere programperioder");
        }
        if (programperioder.isEmpty()) {
            throw new IllegalStateException("Kan ikke håndtere endring i ungdomsprogramperiode uten at det finnes programperioder");
        }
        Resultat resultat = Resultat.tomtResultat();
        resultat.leggTil(håndterTriggerForEndretStartdato(endretUngdomsprogramEtterlysningInput, behandlingReferanse));
        resultat.leggTil(håndterTriggerForEndretSluttdato(endretUngdomsprogramEtterlysningInput, behandlingReferanse));
        return resultat;
    }

    private static Resultat håndterTriggerForEndretStartdato(EndretUngdomsprogramEtterlysningInput endretUngdomsprogramEtterlysningInput, BehandlingReferanse behandlingReferanse) {
        return håndterForType(endretUngdomsprogramEtterlysningInput, behandlingReferanse, EtterlysningType.UTTALELSE_ENDRET_STARTDATO);
    }

    private static Resultat håndterForType(EndretUngdomsprogramEtterlysningInput input, BehandlingReferanse behandlingReferanse, EtterlysningType etterlysningType) {
        final var gjeldendeEtterlysning = finnGjeldendeEtterlysning(input, etterlysningType);
        if (gjeldendeEtterlysning.isPresent()) {
            if (VENTER_STATUSER.contains(gjeldendeEtterlysning.get().etterlysning().getStatus())) {
                return erstattDersomEndret(
                    behandlingReferanse,
                    input.gjeldendePeriodeGrunnlag(),
                    gjeldendeEtterlysning.get()
                );
            } else if (gjeldendeEtterlysning.get().etterlysning().getStatus() == EtterlysningStatus.MOTTATT_SVAR) {
                if (!erSisteMottatteGyldig(input.gjeldendePeriodeGrunnlag(), gjeldendeEtterlysning.get().grunnlag())) {
                    return lagResultatForNyEtterlysningUtenAvbrutt(input.gjeldendePeriodeGrunnlag(), behandlingReferanse.getBehandlingId(), etterlysningType);
                }
            }
        } else if (harEndretPeriodeSidenInitiell(input, input.gjeldendePeriodeGrunnlag(), behandlingReferanse, etterlysningType)) {
            return lagResultatForNyEtterlysningUtenAvbrutt(input.gjeldendePeriodeGrunnlag(), behandlingReferanse.getBehandlingId(), etterlysningType);
        }
        return Resultat.tomtResultat();
    }

    private static boolean harEndretPeriodeSidenInitiell(EndretUngdomsprogramEtterlysningInput endretUngdomsprogramEtterlysningInput, UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag, BehandlingReferanse behandlingReferanse, EtterlysningType etterlysningType) {
        var erEndringSidenInitiell = !finnEndretDatoer(endretUngdomsprogramEtterlysningInput, etterlysningType, endretUngdomsprogramEtterlysningInput.initiellPeriodegrunnlag()
            .orElseThrow(() -> new IllegalStateException("Skal ha innhentet initiell periode")).getGrunnlagsreferanse(), endretUngdomsprogramEtterlysningInput.gjeldendePeriodeGrunnlag().getGrunnlagsreferanse()).isEmpty();

        if (erEndringSidenInitiell) {
            return true;
        }

        if (behandlingReferanse.getBehandlingType() == BehandlingType.FØRSTEGANGSSØKNAD) {
            // Dersom det er førstegangssøknad må vi også sjekke om det er endringer i start dato fra det som ble oppgitt da bruker sendte inn søknaden.
            if (etterlysningType == EtterlysningType.UTTALELSE_ENDRET_STARTDATO) {
                var endringFraOppgitt = UngdomsprogramPeriodeTjeneste.finnEndretStartdatoFraOppgittStartdatoer(endretUngdomsprogramEtterlysningInput.gjeldendePeriodeGrunnlag(), endretUngdomsprogramEtterlysningInput.ungdomsytelseStartdatoGrunnlag());
                var harEndretStartdato = !endringFraOppgitt.isEmpty();
                return harEndretStartdato;
            } else if (etterlysningType == EtterlysningType.UTTALELSE_ENDRET_SLUTTDATO) {
                // For å hindre at sluttdato kan endres uten at bruker får varsel oppretter vi alltid en etterlysning for endret sluttdato dersom den er satt i førstegangssøknad.
                var gjeldendeSluttdato = gjeldendePeriodeGrunnlag.hentForEksaktEnPeriode().getTomDato();
                var harSattSluttdato = gjeldendeSluttdato != null && !gjeldendeSluttdato.equals(AbstractLocalDateInterval.TIDENES_ENDE);
                return harSattSluttdato;
            }
        }
        return false;
    }

    private static Resultat håndterTriggerForEndretSluttdato(EndretUngdomsprogramEtterlysningInput endretUngdomsprogramEtterlysningInput, BehandlingReferanse behandlingReferanse) {
        return håndterForType(endretUngdomsprogramEtterlysningInput, behandlingReferanse, EtterlysningType.UTTALELSE_ENDRET_SLUTTDATO);
    }


    private static Optional<EtterlysningOgGrunnlag> finnGjeldendeEtterlysning(EndretUngdomsprogramEtterlysningInput endretUngdomsprogramEtterlysningInput, EtterlysningType etterlysningType) {
        final var gjeldendeEtterlysninger = etterlysningType.equals(EtterlysningType.UTTALELSE_ENDRET_SLUTTDATO) ? endretUngdomsprogramEtterlysningInput.gjeldendeSluttdatoEtterlysning() : endretUngdomsprogramEtterlysningInput.gjeldendeStartdatoEtterlysning();
        if (gjeldendeEtterlysninger.size() > 1) {
            throw new IllegalStateException("Forventet å finne maksimalt en etterlysning for type " + etterlysningType + " , fant " + gjeldendeEtterlysninger.size());
        }
        return gjeldendeEtterlysninger.isEmpty() ? Optional.empty() : Optional.of(gjeldendeEtterlysninger.get(0));
    }

    private static Resultat lagResultatForNyEtterlysningUtenAvbrutt(UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag, Long behandlingId, EtterlysningType etterlysningType) {
        var gjeldendePeriode = gjeldendePeriodeGrunnlag.hentForEksaktEnPeriode();
        final var nyEtterlysning = Etterlysning.opprettForType(
            behandlingId,
            gjeldendePeriodeGrunnlag.getGrunnlagsreferanse(),
            UUID.randomUUID(),
            gjeldendePeriode,
            etterlysningType
        );
        return new Resultat(List.of(), List.of(nyEtterlysning));
    }

    private static Resultat erstattDersomEndret(BehandlingReferanse behandlingReferanse,
                                                UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag,
                                                EtterlysningOgGrunnlag ventendeEtterlysningOgGrunnlag) {
        var etterlysningType = ventendeEtterlysningOgGrunnlag.etterlysning().getType();
        final var endretDatoer = finnEndretDatoer(etterlysningType, ventendeEtterlysningOgGrunnlag.grunnlag(), gjeldendePeriodeGrunnlag);
        if (!endretDatoer.isEmpty()) {
            if (endretDatoer.size() > 1) {
                throw new IllegalStateException("Forventet å finne maksimalt en endring i datoer, fant " + endretDatoer.size());
            }
            var gjeldendePeriode = gjeldendePeriodeGrunnlag.hentForEksaktEnPeriode();
            final var skalOpprettes = Etterlysning.opprettForType(
                behandlingReferanse.getBehandlingId(),
                gjeldendePeriodeGrunnlag.getGrunnlagsreferanse(),
                UUID.randomUUID(),
                gjeldendePeriode,
                etterlysningType
            );
            ventendeEtterlysningOgGrunnlag.etterlysning().skalAvbrytes();
            return new Resultat(
                List.of(ventendeEtterlysningOgGrunnlag.etterlysning()),
                List.of(skalOpprettes)
            );
        }
        return Resultat.tomtResultat();
    }

    private static List<UngdomsprogramPeriodeTjeneste.EndretDato> finnEndretDatoer(EndretUngdomsprogramEtterlysningInput endretUngdomsprogramEtterlysningInput, EtterlysningType etterlysningType, UUID førsteReferanse, UUID andreReferanse) {
        var førsteGrunnlag = endretUngdomsprogramEtterlysningInput.finnGrunnlag(førsteReferanse);
        var andreGrunnlag = endretUngdomsprogramEtterlysningInput.finnGrunnlag(andreReferanse);
        return finnEndretDatoer(etterlysningType, førsteGrunnlag, andreGrunnlag);
    }

    private static List<UngdomsprogramPeriodeTjeneste.EndretDato> finnEndretDatoer(EtterlysningType etterlysningType, UngdomsprogramPeriodeGrunnlag førsteGrunnlag, UngdomsprogramPeriodeGrunnlag andreGrunnlag) {
        return etterlysningType.equals(EtterlysningType.UTTALELSE_ENDRET_STARTDATO) ?
            UngdomsprogramPeriodeTjeneste.finnEndretStartdatoer(førsteGrunnlag, andreGrunnlag) :
            UngdomsprogramPeriodeTjeneste.finnEndretSluttdatoer(førsteGrunnlag, andreGrunnlag);
    }

    private static boolean erSisteMottatteGyldig(UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag,
                                                 UngdomsprogramPeriodeGrunnlag sisteMottatte) {
        final var endringTidslinje = UngdomsprogramPeriodeTjeneste.finnEndretTidslinje(Optional.of(sisteMottatte), Optional.of(gjeldendePeriodeGrunnlag));
        return endringTidslinje.isEmpty();
    }


    record Resultat(List<Etterlysning> etterlysningSomSkalAvbrytes,
                    List<Etterlysning> etterlysningSomSkalOpprettes) {

        Resultat(List<Etterlysning> etterlysningSomSkalAvbrytes, List<Etterlysning> etterlysningSomSkalOpprettes) {
            Objects.requireNonNull(etterlysningSomSkalAvbrytes);
            Objects.requireNonNull(etterlysningSomSkalOpprettes);
            this.etterlysningSomSkalAvbrytes = new ArrayList<>(etterlysningSomSkalAvbrytes);
            this.etterlysningSomSkalOpprettes = new ArrayList<>(etterlysningSomSkalOpprettes);
        }

        static Resultat tomtResultat() {
            return new Resultat(new ArrayList<>(), new ArrayList<>());
        }

        void leggTil(Resultat resultat) {
            this.etterlysningSomSkalAvbrytes.addAll(resultat.etterlysningSomSkalAvbrytes);
            this.etterlysningSomSkalOpprettes.addAll(resultat.etterlysningSomSkalOpprettes);

        }
    }
}
