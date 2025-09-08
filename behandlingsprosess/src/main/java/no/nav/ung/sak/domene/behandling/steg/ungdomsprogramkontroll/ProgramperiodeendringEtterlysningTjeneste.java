package no.nav.ung.sak.domene.behandling.steg.ungdomsprogramkontroll;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.forhåndsvarsel.EtterlysningType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.sporing.BehandingprosessSporingRepository;
import no.nav.ung.sak.behandlingslager.behandling.sporing.BehandlingprosessSporing;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.ung.sak.etterlysning.AvbrytEtterlysningTask;
import no.nav.ung.sak.etterlysning.EtterlysningTjeneste;
import no.nav.ung.sak.etterlysning.OpprettEtterlysningTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;

@Dependent
public class ProgramperiodeendringEtterlysningTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(ProgramperiodeendringEtterlysningTjeneste.class);

    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private EtterlysningRepository etterlysningRepository;
    private EtterlysningTjeneste etterlysningTjeneste;
    private UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository;
    private BehandingprosessSporingRepository behandingprosessSporingRepository;
    private EtterlysningForEndretProgramperiodeResultatHåndterer resultatHåndterer;

    @Inject
    public ProgramperiodeendringEtterlysningTjeneste(UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
                                                     ProsessTaskTjeneste prosessTaskTjeneste,
                                                     EtterlysningRepository etterlysningRepository,
                                                     EtterlysningTjeneste EtterlysningTjeneste,
                                                     UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository,
                                                     BehandingprosessSporingRepository behandingprosessSporingRepository,
                                                     EtterlysningForEndretProgramperiodeResultatHåndterer resultatHåndterer) {
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.etterlysningRepository = etterlysningRepository;
        this.etterlysningTjeneste = EtterlysningTjeneste;
        this.ungdomsytelseStartdatoRepository = ungdomsytelseStartdatoRepository;
        this.behandingprosessSporingRepository = behandingprosessSporingRepository;
        this.resultatHåndterer = resultatHåndterer;
    }

    public void opprettEtterlysningerForProgramperiodeEndring(BehandlingReferanse behandlingReferanse) {
        var ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandlingReferanse.getBehandlingId()).orElseThrow(() -> new IllegalStateException("Skal ha innhentet perioder"));
        var initiellPeriodegrunnlag = ungdomsprogramPeriodeRepository.hentInitiell(behandlingReferanse.getBehandlingId()).orElseThrow(() -> new IllegalStateException("Skal ha innhentet initiell programperiodegrunnlag for behandling " + behandlingReferanse.getBehandlingId()));
        opprettEtterlysningDersomRelevantEndringForType(behandlingReferanse, EtterlysningType.UTTALELSE_ENDRET_SLUTTDATO, ungdomsprogramPeriodeGrunnlag, initiellPeriodegrunnlag);
        opprettEtterlysningDersomRelevantEndringForType(behandlingReferanse, EtterlysningType.UTTALELSE_ENDRET_STARTDATO, ungdomsprogramPeriodeGrunnlag, initiellPeriodegrunnlag);
        opprettTaskerForOpprettelseAvEtterlysning(behandlingReferanse);
    }

    private void opprettEtterlysningDersomRelevantEndringForType(BehandlingReferanse behandlingReferanse, EtterlysningType etterlysningType, UngdomsprogramPeriodeGrunnlag ungdomsprogramPeriodeGrunnlag, UngdomsprogramPeriodeGrunnlag initiellPeriodegrunnlag) {
        var gjeldendeEtterlysning = finnGjeldendeEtterlysning(behandlingReferanse, etterlysningType);

        var input = new EndretUngdomsprogramEtterlysningInput(
            etterlysningType,
            gjeldendeEtterlysning.map(it -> new EtterlysningOgGrunnlag(new EtterlysningStatusOgType(it.getStatus(), it.getType()), ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(it.getGrunnlagsreferanse()))),
            ungdomsprogramPeriodeGrunnlag,
            initiellPeriodegrunnlag,
            ungdomsytelseStartdatoRepository.hentGrunnlag(behandlingReferanse.getBehandlingId())
        );

        // Utled resultat
        final var resultatEndretProgramperiode = EtterlysningForEndretProgramperiodeResultatUtleder.finnResultat(input, behandlingReferanse);

        // Sporing
        lagreSporing(behandlingReferanse, etterlysningType, input, resultatEndretProgramperiode);

        // Håndter resultat, opprett etterlysning dersom det er relevant
        resultatHåndterer.håndterResultat(resultatEndretProgramperiode, behandlingReferanse, etterlysningType, gjeldendeEtterlysning, input.gjeldendePeriodeGrunnlag());
    }

    private void lagreSporing(BehandlingReferanse behandlingReferanse, EtterlysningType etterlysningType, EndretUngdomsprogramEtterlysningInput input, ResultatType resultatEndretProgramperiode) {
        try {
            behandingprosessSporingRepository.lagreSporing(new BehandlingprosessSporing(behandlingReferanse.getBehandlingId(),
                JsonObjectMapper.getJson(input),
                JsonObjectMapper.getJson(resultatEndretProgramperiode),
                etterlysningType.getKode() + "_UTLEDER"));
        } catch (IOException e) {
            // Ikke kritisk å lagre sporing for prosess
            logger.warn("Kunne ikke lagre prosessporing for behandling {}: {}", behandlingReferanse.getBehandlingId(), e.getMessage(), e);
        }
    }

    private Optional<Etterlysning> finnGjeldendeEtterlysning(BehandlingReferanse behandlingReferanse, EtterlysningType etterlysningType) {
        var gjeldendeEtterlysninger = etterlysningTjeneste.hentGjeldendeEtterlysninger(behandlingReferanse.getBehandlingId(), behandlingReferanse.getFagsakId(), etterlysningType);
        if (gjeldendeEtterlysninger.size() > 1) {
            throw new IllegalStateException("Forventet å finne maksimalt en etterlysning for type " + etterlysningType + " , fant " + gjeldendeEtterlysninger.size());
        }
        return gjeldendeEtterlysninger.isEmpty() ? Optional.empty() : Optional.of(gjeldendeEtterlysninger.get(0));
    }

    private void opprettTaskerForOpprettelseAvEtterlysning(BehandlingReferanse behandlingReferanse) {
        final var prosessTaskGruppe = new ProsessTaskGruppe();

        var etterlysningerSomSkalAvbrytes = etterlysningRepository.hentEtterlysningerSomSkalAvbrytes(behandlingReferanse.getBehandlingId());

        if (!etterlysningerSomSkalAvbrytes.isEmpty()) {
            logger.info("Avbryter etterlysning {}", etterlysningerSomSkalAvbrytes);
            prosessTaskGruppe.addNesteSekvensiell(lagTaskForAvbrytelseAvEtterlysning(behandlingReferanse.getBehandlingId(), behandlingReferanse.getFagsakId()));
        }
        var etterlysningerSomSkalOpprettes = etterlysningRepository.hentOpprettetEtterlysninger(behandlingReferanse.getBehandlingId(), EtterlysningType.UTTALELSE_ENDRET_STARTDATO, EtterlysningType.UTTALELSE_ENDRET_SLUTTDATO);

        if (!etterlysningerSomSkalOpprettes.isEmpty()) {
            logger.info("Oppretter etterlysning {}", etterlysningerSomSkalOpprettes);
            // TODO: Vurder om opprettelse kan skje i en felles task på tvers av typer slik som avbrytelse
            var unikeTyper = etterlysningerSomSkalOpprettes.stream().map(Etterlysning::getType).collect(Collectors.toSet());
            unikeTyper.forEach(type -> prosessTaskGruppe.addNesteSekvensiell(lagTaskForOpprettingAvEtterlysning(behandlingReferanse.getBehandlingId(), behandlingReferanse.getFagsakId(), type)));
        }
        if (!prosessTaskGruppe.getTasks().isEmpty()) {
            prosessTaskTjeneste.lagre(prosessTaskGruppe);
        }
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

}
