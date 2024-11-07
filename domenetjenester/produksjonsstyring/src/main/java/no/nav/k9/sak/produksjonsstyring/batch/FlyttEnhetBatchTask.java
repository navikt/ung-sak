package no.nav.k9.sak.produksjonsstyring.batch;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingKandidaterRepository;
import no.nav.k9.sak.produksjonsstyring.behandlingenhet.OppdaterBehandlendeEnhetTask;

/**
 * Flytting iht PFP-4662
 */
@ApplicationScoped
@ProsessTask(FlyttEnhetBatchTask.TASKTYPE)
public class FlyttEnhetBatchTask implements ProsessTaskHandler {

    public static final String KEY_ENHET = "enhet";
    public static final String TASKTYPE = "batch.flyttTilEnhet";
    private BehandlingKandidaterRepository behandlingKandidaterRepository;
    private ProsessTaskTjeneste taskTjeneste;

    FlyttEnhetBatchTask() {
        // for CDI proxy
    }

    @Inject
    public FlyttEnhetBatchTask(BehandlingKandidaterRepository behandlingKandidaterRepository,
                               ProsessTaskTjeneste taskTjeneste) {
        this.behandlingKandidaterRepository = behandlingKandidaterRepository;
        this.taskTjeneste = taskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        final var enhetValue = prosessTaskData.getPropertyValue(KEY_ENHET);
        if (enhetValue == null || enhetValue.isEmpty()) {
            return;
        }

        List<Behandling> kandidater = behandlingKandidaterRepository.finnBehandlingerIkkeAvsluttetPåAngittEnhet(enhetValue);
        kandidater.forEach(beh -> {
            ProsessTaskData taskData = ProsessTaskData.forProsessTask(OppdaterBehandlendeEnhetTask.class);
            taskData.setBehandling(beh.getFagsakId(), beh.getId(), beh.getAktørId().getId());
            taskData.setCallIdFraEksisterende();
            taskTjeneste.lagre(taskData);
        });
    }
}
