package no.nav.k9.sak.produksjonsstyring.batch;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingKandidaterRepository;
import no.nav.k9.sak.produksjonsstyring.behandlingenhet.OppdaterBehandlendeEnhetTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

/**
 * Flytting iht PFP-4662
 */
@ApplicationScoped
@ProsessTask(FlyttEnhetBatchTjeneste.TASKTYPE)
public class FlyttEnhetBatchTjeneste implements ProsessTaskHandler {

    public static final String KEY_ENHET = "enhet";
    public static final String TASKTYPE = "batch.flyttTilEnhet";
    private BehandlingKandidaterRepository behandlingKandidaterRepository;
    private ProsessTaskRepository prosessTaskRepository;

    FlyttEnhetBatchTjeneste() {
        // for CDI proxy
    }

    @Inject
    public FlyttEnhetBatchTjeneste(BehandlingKandidaterRepository behandlingKandidaterRepository,
                                   ProsessTaskRepository prosessTaskRepository) {
        this.behandlingKandidaterRepository = behandlingKandidaterRepository;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        final var enhetValue = prosessTaskData.getPropertyValue(KEY_ENHET);
        if (enhetValue == null || enhetValue.isEmpty()) {
            return;
        }

        List<Behandling> kandidater = behandlingKandidaterRepository.finnBehandlingerIkkeAvsluttetPåAngittEnhet(enhetValue);
        kandidater.forEach(beh -> {
            ProsessTaskData taskData = new ProsessTaskData(OppdaterBehandlendeEnhetTask.TASKTYPE);
            taskData.setBehandling(beh.getFagsakId(), beh.getId(), beh.getAktørId().getId());
            taskData.setCallIdFraEksisterende();
            prosessTaskRepository.lagre(taskData);
        });
    }
}
