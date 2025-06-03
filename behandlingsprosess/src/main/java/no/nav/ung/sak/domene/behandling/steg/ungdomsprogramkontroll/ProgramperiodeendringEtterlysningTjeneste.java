package no.nav.ung.sak.domene.behandling.steg.ungdomsprogramkontroll;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.AvbrytEtterlysningTask;
import no.nav.ung.sak.etterlysning.EtterlysningTjeneste;
import no.nav.ung.sak.etterlysning.OpprettEtterlysningTask;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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

        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        List<BehandlingÅrsakType> behandlingÅrsakerTyper = behandling.getBehandlingÅrsakerTyper();

        boolean skalOppretteEtterlysning = behandlingÅrsakerTyper.stream()
            .anyMatch(årsak ->
                BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM == årsak ||
                    BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM == årsak
            );

        if (!skalOppretteEtterlysning) {
            return;
        }

        final var gjeldendePeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandlingId).orElseThrow(() -> new IllegalStateException("Skal ha innhentet perioder"));

        // Finner etterlysninger som skal opprettes og avbrytes for endring av programperiode
        final var resultatEndretProgramperiode = finnEndretProgramperiodeResultat(gjeldendePeriodeGrunnlag, behandlingÅrsakerTyper, behandlingId, fagsakId);

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


    private Resultat finnEndretProgramperiodeResultat(UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag, List<BehandlingÅrsakType> behandlingÅrsakerTyper, Long behandlingId, Long fagsakId) {
        final var programperioder = gjeldendePeriodeGrunnlag.getUngdomsprogramPerioder().getPerioder();
        if (programperioder.size() > 1) {
            throw new IllegalStateException("Støtter ikke flere programperioder");
        }
        if (programperioder.isEmpty()) {
            throw new IllegalStateException("Kan ikke håndtere endring i ungdomsprogramperiode uten at det finnes programperioder");
        }
        Resultat resultat = Resultat.tom();
        if (behandlingÅrsakerTyper.contains(BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM)) {
            resultat.leggTil(håndterTriggerForEndretStartdato(gjeldendePeriodeGrunnlag, behandlingId, fagsakId));
        }
        if (behandlingÅrsakerTyper.contains(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM)) {
            resultat.leggTil(håndterTriggerForEndretSluttdato(gjeldendePeriodeGrunnlag, behandlingId, fagsakId));
        }
        return resultat;
    }

    private Resultat håndterTriggerForEndretStartdato(UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag, Long behandlingId, Long fagsakId) {
        return håndterForType(gjeldendePeriodeGrunnlag, behandlingId, fagsakId, EtterlysningType.UTTALELSE_ENDRET_STARTDATO);
    }

    private Resultat håndterForType(UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag, Long behandlingId, Long fagsakId, EtterlysningType etterlysningType) {
        final var gjeldendeEtterlysning = finnGjeldendeEtterlysning(behandlingId, fagsakId, etterlysningType);
        if (gjeldendeEtterlysning.isPresent()) {
            if (VENTER_STATUSER.contains(gjeldendeEtterlysning.get().getStatus())) {
                return erstattDersomEndret(
                    behandlingId,
                    gjeldendePeriodeGrunnlag,
                    gjeldendeEtterlysning.get()
                );
            } else if (gjeldendeEtterlysning.get().getStatus().equals(EtterlysningStatus.MOTTATT_SVAR)) {
                if (!erSisteMottatteGyldig(gjeldendePeriodeGrunnlag, gjeldendeEtterlysning.get())) {
                    return lagResultatForNyEtterlysningUtenAvbrutt(gjeldendePeriodeGrunnlag, behandlingId, etterlysningType);
                }
            }
        } else {
            return lagResultatForNyEtterlysningUtenAvbrutt(gjeldendePeriodeGrunnlag, behandlingId, etterlysningType);
        }
        return Resultat.tom();
    }

    private Resultat håndterTriggerForEndretSluttdato(UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag, Long behandlingId, Long fagsakId) {
        return håndterForType(gjeldendePeriodeGrunnlag, behandlingId, fagsakId, EtterlysningType.UTTALELSE_ENDRET_SLUTTDATO);
    }


    private Optional<Etterlysning> finnGjeldendeEtterlysning(Long behandlingId, Long fagsakId, EtterlysningType etterlysningType) {
        final var gjeldendeEtterlysninger = etterlysningTjeneste.hentGjeldendeEtterlysninger(behandlingId, fagsakId, etterlysningType);
        if (gjeldendeEtterlysninger.size() > 1) {
            throw new IllegalStateException("Forventet å finne maksimalt en etterlysning for endret programperiode, fant " + gjeldendeEtterlysninger.size());
        }
        return gjeldendeEtterlysninger.isEmpty() ? Optional.empty() : Optional.of(gjeldendeEtterlysninger.get(0));
    }

    private static Resultat lagResultatForNyEtterlysningUtenAvbrutt(UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag, Long behandlingId, EtterlysningType etterlysningType) {
        final var nyEtterlysning = Etterlysning.opprettForType(
            behandlingId,
            gjeldendePeriodeGrunnlag.getGrunnlagsreferanse(),
            UUID.randomUUID(),
            gjeldendePeriodeGrunnlag.getUngdomsprogramPerioder().getPerioder().iterator().next().getPeriode(),
            etterlysningType
        );
        return new Resultat(null, List.of(nyEtterlysning));
    }

    private Resultat erstattDersomEndret(Long behandlingId,
                                         UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag,
                                         Etterlysning ventendeEtterlysning) {
        var etterlysningType = ventendeEtterlysning.getType();
        final var endretDatoer = etterlysningType.equals(EtterlysningType.UTTALELSE_ENDRET_STARTDATO) ?
            ungdomsprogramPeriodeTjeneste.finnEndretStartdatoer(ventendeEtterlysning.getGrunnlagsreferanse(), gjeldendePeriodeGrunnlag.getGrunnlagsreferanse()) :
            ungdomsprogramPeriodeTjeneste.finnEndretSluttdatoer(ventendeEtterlysning.getGrunnlagsreferanse(), gjeldendePeriodeGrunnlag.getGrunnlagsreferanse());
        if (!endretDatoer.isEmpty()) {
            if (endretDatoer.size() > 1) {
                throw new IllegalStateException("Forventet å finne maksimalt en endring i datoer, fant " + endretDatoer.size());
            }
            final var skalOpprettes = Etterlysning.opprettForType(
                behandlingId,
                gjeldendePeriodeGrunnlag.getGrunnlagsreferanse(),
                UUID.randomUUID(),
                mapTilPeriode(endretDatoer.getFirst()),
                etterlysningType
            );
            ventendeEtterlysning.skalAvbrytes();
            return new Resultat(
                List.of(ventendeEtterlysning),
                List.of(skalOpprettes)
            );
        }
        return Resultat.tom();
    }

    private DatoIntervallEntitet mapTilPeriode(UngdomsprogramPeriodeTjeneste.EndretDato endretDato) {
        return endretDato.nyDato().isBefore(endretDato.forrigeDato()) ? DatoIntervallEntitet.fraOgMedTilOgMed(endretDato.nyDato(), endretDato.forrigeDato().minusDays(1)) :
            DatoIntervallEntitet.fraOgMedTilOgMed(endretDato.forrigeDato(), endretDato.nyDato().minusDays(1));
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
        static Resultat tom() {
            return new Resultat(List.of(), List.of());
        }


        void leggTil(Resultat resultat) {
            this.etterlysningSomSkalAvbrytes.addAll(resultat.etterlysningSomSkalAvbrytes);
            this.etterlysningSomSkalOpprettes.addAll(resultat.etterlysningSomSkalOpprettes);

        }
    }

}
