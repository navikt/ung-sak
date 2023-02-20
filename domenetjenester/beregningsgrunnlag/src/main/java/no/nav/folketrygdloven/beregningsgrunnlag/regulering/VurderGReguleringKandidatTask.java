package no.nav.folketrygdloven.beregningsgrunnlag.regulering;

import java.time.LocalDate;

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
@ProsessTask(VurderGReguleringKandidatTask.TASKTYPE)
@ApplicationScoped
public class VurderGReguleringKandidatTask extends FagsakProsessTask {

    public static final String TASKTYPE = "gregulering.kandidatUtprøving";
    public static final String PERIODE_FOM = "fom";
    public static final String PERIODE_TOM = "tom";
    public static final String BEHANDLINGSKONTROLL_OPPRETT_REVURDERING_ELLER_DIFF_TASK = "behandlingskontroll.opprettRevurderingEllerDiff";
    public static final String BEHANDLING_ARSAK = "behandlingArsak";
    private static final Logger log = LoggerFactory.getLogger(VurderGReguleringKandidatTask.class);
    private KandidaterForGReguleringTjeneste kandidaterForGReguleringTjeneste;
    private ProsessTaskTjeneste taskRepository;

    VurderGReguleringKandidatTask() {
        // CDI
    }

    @Inject
    public VurderGReguleringKandidatTask(FagsakLåsRepository fagsakLåsRepository,
                                         KandidaterForGReguleringTjeneste kandidaterForGReguleringTjeneste,
                                         ProsessTaskTjeneste taskRepository) {
        super(fagsakLåsRepository, null);
        this.kandidaterForGReguleringTjeneste = kandidaterForGReguleringTjeneste;
        this.taskRepository = taskRepository;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        var fom = LocalDate.parse(prosessTaskData.getPropertyValue(PERIODE_FOM));
        var tom = LocalDate.parse(prosessTaskData.getPropertyValue(PERIODE_TOM));
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);

        var skalGReguleres = kandidaterForGReguleringTjeneste.skalGReguleres(prosessTaskData.getFagsakId(), periode);

        if (skalGReguleres) {
            log.info("Fagsaken skal g-reguleres");
            var data =  ProsessTaskData.forTaskType(new TaskType(BEHANDLINGSKONTROLL_OPPRETT_REVURDERING_ELLER_DIFF_TASK));
            data.setFagsakId(prosessTaskData.getFagsakId());
            data.setProperty(BEHANDLING_ARSAK, BehandlingÅrsakType.RE_SATS_REGULERING.getKode());
            data.setProperty(PERIODE_FOM, prosessTaskData.getPropertyValue(PERIODE_FOM));
            data.setProperty(PERIODE_TOM, prosessTaskData.getPropertyValue(PERIODE_TOM));

            taskRepository.lagre(data);
        } else {
            log.info("Fagsaken trenger IKKE g-reguleres");
        }
    }
}
