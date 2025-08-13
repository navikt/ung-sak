package no.nav.ung.sak.domene.behandling.steg.ungdomsprogramkontroll;

import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste.*;

public class EtterlysningForEndretProgramperiodeResultatUtleder {

    public static final Set<EtterlysningStatus> VENTER_STATUSER = Set.of(EtterlysningStatus.VENTER, EtterlysningStatus.OPPRETTET);


    /**
     * Utleder resultat som beskriver behov for etterlysning i forbindelse med endring av ungdomsprogramperiode.
     * Denne metoden utleder kun hva som skal gjøres i forhold til eksisterende etterlysning, men gjør ikke selve endringen.
     *
     * @param input               Input til utledning, inkluderer eksisterende programperiode, etterlysninger og initielle grunnlag
     * @param behandlingReferanse Behandlingreferanse
     * @return
     */
    static ResultatType finnResultat(EndretUngdomsprogramEtterlysningInput input, BehandlingReferanse behandlingReferanse) {
        validerNøyaktigEnProgramperiode(input);
        return håndterForType(input, behandlingReferanse, input.etterlysningType());
    }

    private static ResultatType håndterForType(EndretUngdomsprogramEtterlysningInput input, BehandlingReferanse behandlingReferanse, EtterlysningType etterlysningType) {
        final var gjeldendeEtterlysning = input.gjeldendeEtterlysningOgGrunnlag();
        if (gjeldendeEtterlysning.isPresent()) {
            if (VENTER_STATUSER.contains(gjeldendeEtterlysning.get().etterlysningData().status())) {
                return erstattDersomEndret(
                    input.gjeldendePeriodeGrunnlag(),
                    gjeldendeEtterlysning.get()
                );
            } else if (gjeldendeEtterlysning.get().etterlysningData().status() == EtterlysningStatus.MOTTATT_SVAR) {
                if (!erSisteMottatteGyldig(input.gjeldendePeriodeGrunnlag(), gjeldendeEtterlysning.get().grunnlag())) {
                    return ResultatType.OPPRETT_ETTERLYSNING;
                }
            }
        } else if (harEndretPeriodeSidenInitiell(input, input.gjeldendePeriodeGrunnlag(), behandlingReferanse, etterlysningType)) {
            return ResultatType.OPPRETT_ETTERLYSNING;
        }
        return ResultatType.INGEN_ENDRING;
    }

    private static boolean harEndretPeriodeSidenInitiell(EndretUngdomsprogramEtterlysningInput input,
                                                         UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag,
                                                         BehandlingReferanse behandlingReferanse,
                                                         EtterlysningType etterlysningType) {
        var erEndringSidenInitiell = !finnEndretDatoer(etterlysningType,
            input.initiellPeriodegrunnlag(),
            input.gjeldendePeriodeGrunnlag()).isEmpty();

        if (erEndringSidenInitiell) {
            return true;
        }

        if (behandlingReferanse.getBehandlingType() == BehandlingType.FØRSTEGANGSSØKNAD) {
            // Dersom det er førstegangssøknad må vi også sjekke om det er endringer i start dato fra det som ble oppgitt da bruker sendte inn søknaden.
            return switch (etterlysningType) {
                case UTTALELSE_ENDRET_STARTDATO -> harEndretStartdato(input);
                case UTTALELSE_ENDRET_SLUTTDATO -> harEndretSluttdato(gjeldendePeriodeGrunnlag);
                default ->
                    throw new IllegalArgumentException("Ugyldig etterlysningstype for endring i programperiode: " + etterlysningType);
            };
        }
        return false;
    }

    private static boolean harEndretSluttdato(UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag) {
        // For å hindre at sluttdato kan endres uten at bruker får varsel oppretter vi alltid en etterlysning for endret sluttdato dersom den er satt i førstegangssøknad.
        var gjeldendeSluttdato = gjeldendePeriodeGrunnlag.hentForEksaktEnPeriode().getTomDato();
        var harSattSluttdato = gjeldendeSluttdato != null && !gjeldendeSluttdato.equals(AbstractLocalDateInterval.TIDENES_ENDE);
        return harSattSluttdato;
    }

    private static boolean harEndretStartdato(EndretUngdomsprogramEtterlysningInput input) {
        var endringFraOppgitt = finnEndretStartdatoFraOppgittStartdatoer(input.gjeldendePeriodeGrunnlag(), input.ungdomsytelseStartdatoGrunnlag());
        var harEndretStartdato = !endringFraOppgitt.isEmpty();
        return harEndretStartdato;
    }

    private static ResultatType erstattDersomEndret(UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag,
                                                    EtterlysningOgGrunnlag ventendeEtterlysningOgGrunnlag) {
        var etterlysningType = ventendeEtterlysningOgGrunnlag.etterlysningData().type();
        final var endretDatoer = finnEndretDatoer(etterlysningType, ventendeEtterlysningOgGrunnlag.grunnlag(), gjeldendePeriodeGrunnlag);
        if (!endretDatoer.isEmpty()) {
            if (endretDatoer.size() > 1) {
                throw new IllegalStateException("Forventet å finne maksimalt en endring i datoer, fant " + endretDatoer.size());
            }
            return ResultatType.ERSTATT_EKSISTERENDE_ETTERLYSNING;
        }
        return ResultatType.INGEN_ENDRING;
    }

    private static List<EndretDato> finnEndretDatoer(EtterlysningType etterlysningType, UngdomsprogramPeriodeGrunnlag førsteGrunnlag, UngdomsprogramPeriodeGrunnlag andreGrunnlag) {
        return switch (etterlysningType) {
            case UTTALELSE_ENDRET_STARTDATO -> finnEndretStartdatoer(førsteGrunnlag, andreGrunnlag);
            case UTTALELSE_ENDRET_SLUTTDATO -> finnEndretSluttdatoer(førsteGrunnlag, andreGrunnlag);
            default ->
                throw new IllegalArgumentException("Ikke gyldig etterlysningstype for endring i programperiode: " + etterlysningType);
        };
    }

    private static boolean erSisteMottatteGyldig(UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag,
                                                 UngdomsprogramPeriodeGrunnlag sisteMottatte) {
        final var endringTidslinje = finnEndretTidslinje(Optional.of(sisteMottatte), Optional.of(gjeldendePeriodeGrunnlag));
        return endringTidslinje.isEmpty();
    }

    private static void validerNøyaktigEnProgramperiode(EndretUngdomsprogramEtterlysningInput input) {
        // Ekstra validering for å sjekke at det kun er én programperiode i grunnlaget.
        final var programperioder = input.gjeldendePeriodeGrunnlag().getUngdomsprogramPerioder().getPerioder();
        if (programperioder.size() > 1) {
            throw new IllegalStateException("Støtter ikke flere programperioder");
        }
        if (programperioder.isEmpty()) {
            throw new IllegalStateException("Kan ikke håndtere endring i ungdomsprogramperiode uten at det finnes programperioder");
        }
    }

}
