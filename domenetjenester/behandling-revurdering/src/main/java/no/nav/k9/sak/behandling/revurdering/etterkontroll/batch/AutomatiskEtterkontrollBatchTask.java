package no.nav.k9.sak.behandling.revurdering.etterkontroll.batch;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.log.mdc.MDCOperations;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.Etterkontroll;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.task.AutomatiskEtterkontrollTask;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;

@ApplicationScoped
@ProsessTask(value = AutomatiskEtterkontrollBatchTask.TASKTYPE, cronExpression = "0 15 7 * * *", maxFailedRuns = 1)
public class AutomatiskEtterkontrollBatchTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "batch.etterkontroll";
    private static final Logger log = LoggerFactory.getLogger(AutomatiskEtterkontrollBatchTask.class);
    private BehandlingRepository behandlingRepository;
    private EtterkontrollRepository repository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private boolean aktiverAutomatiskEtterkontrollBatch;

    public AutomatiskEtterkontrollBatchTask() {}

    @Inject
    public AutomatiskEtterkontrollBatchTask(BehandlingRepositoryProvider repositoryProvider,
                                            EtterkontrollRepository repository,
                                            ProsessTaskTjeneste prosessTaskTjeneste,
                                            @KonfigVerdi(value = "AKTIVER_AUTOMATISK_ETTERKONTROLL", defaultVerdi = "true") boolean aktiverAutomatiskEtterkontrollBatch) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.repository = repository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.aktiverAutomatiskEtterkontrollBatch = aktiverAutomatiskEtterkontrollBatch;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        if (!aktiverAutomatiskEtterkontrollBatch) {
            return;
        }

        var etterkontroller = repository.finnKandidaterForAutomatiskEtterkontroll();

        String callId = MDCOperations.getCallId();
        callId = (callId == null ? MDCOperations.generateCallId() : callId) + "_";

        for (Etterkontroll etterkontroll : etterkontroller) {
            String nyCallId = callId + etterkontroll.getId();
            log.info("{} oppretter task med ny callId: {} ", getClass().getSimpleName(), nyCallId);
            opprettEtterkontrollTask(etterkontroll, nyCallId);
        }

    }

    private void opprettEtterkontrollTask(Etterkontroll kandidat, String callId) {
        ProsessTaskData prosessTaskData = ProsessTaskData.forProsessTask(AutomatiskEtterkontrollTask.class);

        Objects.requireNonNull(kandidat.getBehandlingId(),
            "Utviklerfeil: Behandlingsid må være satt. Kun gamle ferdige etterkontroller manlger behandlingid");

        var behandling = behandlingRepository.hentBehandling(kandidat.getBehandlingId());
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");
        prosessTaskData.setPrioritet(100);
        prosessTaskData.setProperty(AutomatiskEtterkontrollTask.ETTERKONTROLL_ID, kandidat.getId().toString());
        // unik per task da det er ulike tasks for hver behandling
        prosessTaskData.setCallId(callId);

        prosessTaskTjeneste.lagre(prosessTaskData);
    }

}
