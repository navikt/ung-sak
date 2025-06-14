package no.nav.ung.sak.domene.behandling.steg.ungdomsprogramkontroll;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.AvbrytEtterlysningTask;
import no.nav.ung.sak.etterlysning.EtterlysningTjeneste;
import no.nav.ung.sak.etterlysning.OpprettEtterlysningTask;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@Dependent
public class ProgramperiodeendringEtterlysningTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(ProgramperiodeendringEtterlysningTjeneste.class);
    public static final Set<EtterlysningStatus> VENTER_STATUSER = Set.of(EtterlysningStatus.VENTER, EtterlysningStatus.OPPRETTET);

    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private EtterlysningRepository etterlysningRepository;
    private EtterlysningTjeneste etterlysningTjeneste;
    private BehandlingRepository behandlingRepository;

    @Inject
    public ProgramperiodeendringEtterlysningTjeneste(UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste,
                                                     UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
                                                     ProsessTaskTjeneste prosessTaskTjeneste,
                                                     EtterlysningRepository etterlysningRepository,
                                                     EtterlysningTjeneste EtterlysningTjeneste, BehandlingRepository behandlingRepository) {
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.etterlysningRepository = etterlysningRepository;
        this.etterlysningTjeneste = EtterlysningTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    public void opprettEtterlysningerForProgramperiodeEndring(BehandlingReferanse behandlingReferanse) {
        final var behandlingId = behandlingReferanse.getBehandlingId();
        final var fagsakId = behandlingReferanse.getFagsakId();
        final var gjeldendePeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandlingId).orElseThrow(() -> new IllegalStateException("Skal ha innhentet perioder"));

        // Finner etterlysninger som skal opprettes og avbrytes for endring av programperiode
        final var resultatEndretProgramperiode = finnEndretProgramperiodeResultat(gjeldendePeriodeGrunnlag, behandlingReferanse);

        final var prosessTaskGruppe = new ProsessTaskGruppe();


        if (!resultatEndretProgramperiode.etterlysningSomSkalAvbrytes().isEmpty()) {
            logger.info("Avbryter etterlysning {}", resultatEndretProgramperiode.etterlysningSomSkalAvbrytes());
            etterlysningRepository.lagre(resultatEndretProgramperiode.etterlysningSomSkalAvbrytes());
            prosessTaskGruppe.addNesteSekvensiell(lagTaskForAvbrytelseAvEtterlysning(behandlingId, fagsakId));
        }
        if (!resultatEndretProgramperiode.etterlysningSomSkalOpprettes().isEmpty()) {
            logger.info("Oppretter etterlysning {}", resultatEndretProgramperiode.etterlysningSomSkalOpprettes());
            etterlysningRepository.lagre(resultatEndretProgramperiode.etterlysningSomSkalOpprettes());
            // TODO: Vurder om opprettelse kan skje i en felles task på tvers av typer slik som avbrytelse
            var unikeTyper = resultatEndretProgramperiode.etterlysningSomSkalOpprettes().stream().map(Etterlysning::getType).collect(Collectors.toSet());
            unikeTyper.forEach(type -> prosessTaskGruppe.addNesteSekvensiell(lagTaskForOpprettingAvEtterlysning(behandlingId, fagsakId, type)));
        }
        if (!prosessTaskGruppe.getTasks().isEmpty()) {
            prosessTaskTjeneste.lagre(prosessTaskGruppe);
        }
    }


    private Resultat finnEndretProgramperiodeResultat(UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag,
                                                      BehandlingReferanse behandlingReferanse) {
        // Ekstra validering for å sjekke at det kun er én programperiode i grunnlaget.
        final var programperioder = gjeldendePeriodeGrunnlag.getUngdomsprogramPerioder().getPerioder();
        if (programperioder.size() > 1) {
            throw new IllegalStateException("Støtter ikke flere programperioder");
        }
        if (programperioder.isEmpty()) {
            throw new IllegalStateException("Kan ikke håndtere endring i ungdomsprogramperiode uten at det finnes programperioder");
        }
        Resultat resultat = Resultat.tomtResultat();
        resultat.leggTil(håndterTriggerForEndretStartdato(gjeldendePeriodeGrunnlag, behandlingReferanse));
        resultat.leggTil(håndterTriggerForEndretSluttdato(gjeldendePeriodeGrunnlag, behandlingReferanse));
        return resultat;
    }

    private Resultat håndterTriggerForEndretStartdato(UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag, BehandlingReferanse behandlingReferanse) {
        return håndterForType(gjeldendePeriodeGrunnlag, behandlingReferanse, EtterlysningType.UTTALELSE_ENDRET_STARTDATO);
    }

    private Resultat håndterForType(UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag, BehandlingReferanse behandlingReferanse, EtterlysningType etterlysningType) {
        final var gjeldendeEtterlysning = finnGjeldendeEtterlysning(behandlingReferanse, etterlysningType);
        if (gjeldendeEtterlysning.isPresent()) {
            if (VENTER_STATUSER.contains(gjeldendeEtterlysning.get().getStatus())) {
                return erstattDersomEndret(
                    behandlingReferanse,
                    gjeldendePeriodeGrunnlag,
                    gjeldendeEtterlysning.get()
                );
            } else if (gjeldendeEtterlysning.get().getStatus() == EtterlysningStatus.MOTTATT_SVAR) {
                if (!erSisteMottatteGyldig(gjeldendePeriodeGrunnlag, gjeldendeEtterlysning.get())) {
                    return lagResultatForNyEtterlysningUtenAvbrutt(gjeldendePeriodeGrunnlag, behandlingReferanse.getBehandlingId(), etterlysningType);
                }
            }
        } else if (harEndretPeriodeSidenInitiell(gjeldendePeriodeGrunnlag, behandlingReferanse, etterlysningType)) {
            return lagResultatForNyEtterlysningUtenAvbrutt(gjeldendePeriodeGrunnlag, behandlingReferanse.getBehandlingId(), etterlysningType);
        }
        return Resultat.tomtResultat();
    }

    private boolean harEndretPeriodeSidenInitiell(UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag, BehandlingReferanse behandlingReferanse, EtterlysningType etterlysningType) {
        var erEndringSidenInitiell = !finnEndretDatoer(etterlysningType, ungdomsprogramPeriodeRepository.hentInitiell(behandlingReferanse.getBehandlingId())
            .orElseThrow(() -> new IllegalStateException("Skal ha innhentet initiell periode")).getGrunnlagsreferanse(), gjeldendePeriodeGrunnlag.getGrunnlagsreferanse()).isEmpty();

        if (erEndringSidenInitiell) {
            return true;
        }

        if (behandlingReferanse.getBehandlingType() == BehandlingType.FØRSTEGANGSSØKNAD) {
            // Dersom det er førstegangssøknad må vi også sjekke om det er endringer i start dato fra det som ble oppgitt da bruker sendte inn søknaden.
            if (etterlysningType == EtterlysningType.UTTALELSE_ENDRET_STARTDATO) {
                var endringFraOppgitt = ungdomsprogramPeriodeTjeneste.finnEndretStartdatoFraOppgitteStartdatoer(behandlingReferanse.getBehandlingId());
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

    private Resultat håndterTriggerForEndretSluttdato(UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag, BehandlingReferanse behandlingReferanse) {
        return håndterForType(gjeldendePeriodeGrunnlag, behandlingReferanse, EtterlysningType.UTTALELSE_ENDRET_SLUTTDATO);
    }


    private Optional<Etterlysning> finnGjeldendeEtterlysning(BehandlingReferanse behandlingReferanse, EtterlysningType etterlysningType) {
        final var gjeldendeEtterlysninger = etterlysningTjeneste.hentGjeldendeEtterlysninger(behandlingReferanse.getBehandlingId(), behandlingReferanse.getFagsakId(), etterlysningType);
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

    private Resultat erstattDersomEndret(BehandlingReferanse behandlingReferanse,
                                         UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag,
                                         Etterlysning ventendeEtterlysning) {
        var etterlysningType = ventendeEtterlysning.getType();
        final var endretDatoer = finnEndretDatoer(etterlysningType, ventendeEtterlysning.getGrunnlagsreferanse(), gjeldendePeriodeGrunnlag.getGrunnlagsreferanse());
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
            ventendeEtterlysning.skalAvbrytes();
            return new Resultat(
                List.of(ventendeEtterlysning),
                List.of(skalOpprettes)
            );
        }
        return Resultat.tomtResultat();
    }

    private List<UngdomsprogramPeriodeTjeneste.EndretDato> finnEndretDatoer(EtterlysningType etterlysningType, UUID førsteReferanse, UUID andreReferanse) {
        return etterlysningType.equals(EtterlysningType.UTTALELSE_ENDRET_STARTDATO) ?
            ungdomsprogramPeriodeTjeneste.finnEndretStartdatoer(førsteReferanse, andreReferanse) :
            ungdomsprogramPeriodeTjeneste.finnEndretSluttdatoer(førsteReferanse, andreReferanse);
    }

    private boolean erSisteMottatteGyldig(UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag,
                                          Etterlysning sisteMottatte) {
        final var endringTidslinje = ungdomsprogramPeriodeTjeneste.finnEndretPeriodeTidslinje(sisteMottatte.getGrunnlagsreferanse(), gjeldendePeriodeGrunnlag.getGrunnlagsreferanse());
        return endringTidslinje.isEmpty();
    }


    private ProsessTaskData lagTaskForAvbrytelseAvEtterlysning(Long behandlingId, Long fagsakId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(AvbrytEtterlysningTask.class);
        prosessTaskData.setBehandling(fagsakId, behandlingId);
        return prosessTaskData;
    }

    private ProsessTaskData lagTaskForOpprettingAvEtterlysning(Long behandlingId, Long fagsakId, EtterlysningType type) {
        var prosessTaskData = ProsessTaskData.forProsessTask(OpprettEtterlysningTask.class);
        prosessTaskData.setProperty(OpprettEtterlysningTask.ETTERLYSNING_TYPE, type.getKode());
        prosessTaskData.setBehandling(fagsakId, behandlingId);
        return prosessTaskData;
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
