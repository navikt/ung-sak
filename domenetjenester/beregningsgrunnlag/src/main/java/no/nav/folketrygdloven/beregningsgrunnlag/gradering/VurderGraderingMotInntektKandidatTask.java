package no.nav.folketrygdloven.beregningsgrunnlag.gradering;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.prosesstask.api.TaskType;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.FagsakProsessTask;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@FagsakProsesstaskRekkefølge(gruppeSekvens = false) // Trenger ikke blokkere andre tasks på saken
@ProsessTask(VurderGraderingMotInntektKandidatTask.TASKTYPE)
@ApplicationScoped
public class VurderGraderingMotInntektKandidatTask extends FagsakProsessTask {

    public static final String TASKTYPE = "gradering.kandidatUtprøving";
    public static final String BEHANDLINGSKONTROLL_OPPRETT_REVURDERING_ELLER_DIFF_TASK = "behandlingskontroll.opprettRevurderingEllerDiff";
    public static final String BEHANDLING_ARSAK = "behandlingArsak";
    private static final Logger log = LoggerFactory.getLogger(VurderGraderingMotInntektKandidatTask.class);
    public static final String PERIODER = "perioder";
    private KandidaterForInntektgraderingTjeneste kandidaterForInntektgraderingTjeneste;
    private ProsessTaskTjeneste taskRepository;

    VurderGraderingMotInntektKandidatTask() {
        // CDI
    }

    @Inject
    public VurderGraderingMotInntektKandidatTask(FagsakLåsRepository fagsakLåsRepository,
                                                 KandidaterForInntektgraderingTjeneste kandidaterForInntektgraderingTjeneste,
                                                 ProsessTaskTjeneste taskRepository) {
        super(fagsakLåsRepository, null);
        this.kandidaterForInntektgraderingTjeneste = kandidaterForInntektgraderingTjeneste;
        this.taskRepository = taskRepository;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        var vurderingsperioder = kandidaterForInntektgraderingTjeneste.finnGraderingMotInntektPerioder(prosessTaskData.getFagsakId(), LocalDate.of(2023, 4, 1));
        if (!vurderingsperioder.isEmpty()) {
            log.info("Fagsaken skal vurderes for tilkommet inntekt");
            var data = ProsessTaskData.forTaskType(new TaskType(BEHANDLINGSKONTROLL_OPPRETT_REVURDERING_ELLER_DIFF_TASK));
            data.setFagsakId(prosessTaskData.getFagsakId());
            data.setProperty(BEHANDLING_ARSAK, BehandlingÅrsakType.RE_ENDRET_FORDELING.getKode());
            data.setProperty(PERIODER, utledPerioder(vurderingsperioder));
            taskRepository.lagre(data);
        } else {
            log.info("Fagsaken trenger IKKE g-reguleres");
        }
    }

    private String utledPerioder(Set<DatoIntervallEntitet> perioder) {
        return perioder.stream().map(it -> it.getFomDato() + "/" + it.getTomDato())
            .collect(Collectors.joining("|"));
    }
}
