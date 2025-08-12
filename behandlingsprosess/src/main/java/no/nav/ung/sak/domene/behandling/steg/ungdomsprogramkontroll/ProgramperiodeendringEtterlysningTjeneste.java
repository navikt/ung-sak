package no.nav.ung.sak.domene.behandling.steg.ungdomsprogramkontroll;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.sporing.BehandingprosessSporingRepository;
import no.nav.ung.sak.behandlingslager.behandling.sporing.BehandlingprosessSporing;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.ung.sak.etterlysning.AvbrytEtterlysningTask;
import no.nav.ung.sak.etterlysning.EtterlysningTjeneste;
import no.nav.ung.sak.etterlysning.OpprettEtterlysningTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Dependent
public class ProgramperiodeendringEtterlysningTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(ProgramperiodeendringEtterlysningTjeneste.class);

    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private EtterlysningRepository etterlysningRepository;
    private EtterlysningTjeneste etterlysningTjeneste;
    private UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository;
    private BehandingprosessSporingRepository behandingprosessSporingRepository;

    @Inject
    public ProgramperiodeendringEtterlysningTjeneste(UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
                                                     ProsessTaskTjeneste prosessTaskTjeneste,
                                                     EtterlysningRepository etterlysningRepository,
                                                     EtterlysningTjeneste EtterlysningTjeneste,
                                                     UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository, BehandingprosessSporingRepository behandingprosessSporingRepository) {
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.etterlysningRepository = etterlysningRepository;
        this.etterlysningTjeneste = EtterlysningTjeneste;
        this.ungdomsytelseStartdatoRepository = ungdomsytelseStartdatoRepository;
        this.behandingprosessSporingRepository = behandingprosessSporingRepository;
    }

    public void opprettEtterlysningerForProgramperiodeEndring(BehandlingReferanse behandlingReferanse) {
        var input = lagInput(behandlingReferanse);
        final var resultatEndretProgramperiode = EtterlysningForEndretProgramperiodeUtleder.finnEndretProgramperiodeResultat(input, behandlingReferanse);

        try {
            behandingprosessSporingRepository.lagreSporing(new BehandlingprosessSporing(behandlingReferanse.getBehandlingId(),
                JsonObjectMapper.getJson(input),
                JsonObjectMapper.getJson(resultatEndretProgramperiode),
                "ProgramperiodeendringEtterlysningTjeneste"));
        } catch (IOException e) {
            // Ikke kritisk å lagre sporing for prosess
            logger.warn("Kunne ikke lagre prosessporing for behandling {}: {}", behandlingReferanse.getBehandlingId(), e.getMessage(), e);
        }

        opprettTaskerForOpprettelseAvEtterlysning(behandlingReferanse, resultatEndretProgramperiode);
    }

    private void opprettTaskerForOpprettelseAvEtterlysning(BehandlingReferanse behandlingReferanse, EtterlysningForEndretProgramperiodeUtleder.Resultat resultatEndretProgramperiode) {
        final var prosessTaskGruppe = new ProsessTaskGruppe();

        if (!resultatEndretProgramperiode.etterlysningSomSkalAvbrytes().isEmpty()) {
            logger.info("Avbryter etterlysning {}", resultatEndretProgramperiode.etterlysningSomSkalAvbrytes());
            etterlysningRepository.lagre(resultatEndretProgramperiode.etterlysningSomSkalAvbrytes());
            prosessTaskGruppe.addNesteSekvensiell(lagTaskForAvbrytelseAvEtterlysning(behandlingReferanse.getBehandlingId(), behandlingReferanse.getFagsakId()));
        }
        if (!resultatEndretProgramperiode.etterlysningSomSkalOpprettes().isEmpty()) {
            logger.info("Oppretter etterlysning {}", resultatEndretProgramperiode.etterlysningSomSkalOpprettes());
            etterlysningRepository.lagre(resultatEndretProgramperiode.etterlysningSomSkalOpprettes());
            // TODO: Vurder om opprettelse kan skje i en felles task på tvers av typer slik som avbrytelse
            var unikeTyper = resultatEndretProgramperiode.etterlysningSomSkalOpprettes().stream().map(Etterlysning::getType).collect(Collectors.toSet());
            unikeTyper.forEach(type -> prosessTaskGruppe.addNesteSekvensiell(lagTaskForOpprettingAvEtterlysning(behandlingReferanse.getBehandlingId(), behandlingReferanse.getFagsakId(), type)));
        }
        if (!prosessTaskGruppe.getTasks().isEmpty()) {
            prosessTaskTjeneste.lagre(prosessTaskGruppe);
        }
    }

    private EndretUngdomsprogramEtterlysningInput lagInput(BehandlingReferanse behandlingReferanse) {
        final var gjeldendePeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandlingReferanse.getBehandlingId()).orElseThrow(() -> new IllegalStateException("Skal ha innhentet perioder"));

        // Finner etterlysninger som skal opprettes og avbrytes for endring av programperiode
        var gjeldendeStartdatoEtterlysning = etterlysningTjeneste.hentGjeldendeEtterlysninger(behandlingReferanse.getBehandlingId(), behandlingReferanse.getFagsakId(), EtterlysningType.UTTALELSE_ENDRET_STARTDATO);
        var gjeldendeSluttdatoEtterlysning = etterlysningTjeneste.hentGjeldendeEtterlysninger(behandlingReferanse.getBehandlingId(), behandlingReferanse.getFagsakId(), EtterlysningType.UTTALELSE_ENDRET_SLUTTDATO);
        var initiellPeriodegrunnlag = ungdomsprogramPeriodeRepository.hentInitiell(behandlingReferanse.getBehandlingId());

        var grunnlagsmap = Stream.concat(gjeldendeStartdatoEtterlysning.stream(), gjeldendeSluttdatoEtterlysning.stream())
            .collect(Collectors.toMap(Etterlysning::getGrunnlagsreferanse, it -> ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(it.getGrunnlagsreferanse()).orElseThrow(() -> new IllegalStateException("Forventer å finne grunnlag for referanse " + it.getGrunnlagsreferanse())), (e1, e2) -> e1));
        var mutableMap = new HashMap<>(grunnlagsmap);
        initiellPeriodegrunnlag.ifPresent(it -> mutableMap.putIfAbsent(it.getGrunnlagsreferanse(), it));
        mutableMap.putIfAbsent(gjeldendePeriodeGrunnlag.getGrunnlagsreferanse(), gjeldendePeriodeGrunnlag);

        var input = new EndretUngdomsprogramEtterlysningInput(
            gjeldendeStartdatoEtterlysning.stream().map(it -> new EtterlysningOgGrunnlag(it, grunnlagsmap.get(it.getGrunnlagsreferanse()))).toList(),
            gjeldendeSluttdatoEtterlysning.stream().map(it -> new EtterlysningOgGrunnlag(it, grunnlagsmap.get(it.getGrunnlagsreferanse()))).toList(),
            gjeldendePeriodeGrunnlag,
            initiellPeriodegrunnlag,
            mutableMap,
            ungdomsytelseStartdatoRepository.hentGrunnlag(behandlingReferanse.getBehandlingId())
        );
        return input;
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
