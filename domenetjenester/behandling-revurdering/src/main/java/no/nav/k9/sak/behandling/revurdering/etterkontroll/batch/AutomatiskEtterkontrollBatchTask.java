package no.nav.k9.sak.behandling.revurdering.etterkontroll.batch;

import java.time.Period;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.log.mdc.MDCOperations;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.Etterkontroll;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.task.AutomatiskEtterkontrollTask;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;

/**
 * Henter ut behandlinger som har fått innvilget engangsstønad på bakgrunn av terminbekreftelsen,
 * for å etterkontrollere om rett antall barn har blitt født.
 * <p>
 * Vedtak er innvilget og fattet med bakgrunn i bekreftet terminbekreftelse
 * Det har gått minst 60 dager siden termin
 * Det er ikke registrert fødselsdato på barnet/barna
 * Det ikke allerede er opprettet revurderingsbehandling med en av disse årsakene:
 * Manglende fødsel i TPS
 * Manglende fødsel i TPS mellom uke 26 og 29
 * Avvik i antall barn
 * <p>
 * Ved avvik så opprettes det, hvis det ikke allerede finnes, revurderingsbehandling på saken
 */

@ApplicationScoped
@ProsessTask(value = AutomatiskEtterkontrollBatchTask.TASKTYPE, cronExpression = "0 15 7 * * *", maxFailedRuns = 1)
public class AutomatiskEtterkontrollBatchTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "batch.etterkontroll";
    private static final Logger log = LoggerFactory.getLogger(AutomatiskEtterkontrollBatchTask.class);
    private BehandlingRepository behandlingRepository;
    private EtterkontrollRepository repository;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    public AutomatiskEtterkontrollBatchTask(BehandlingRepositoryProvider repositoryProvider,
                                            EtterkontrollRepository repository,
                                            ProsessTaskTjeneste prosessTaskTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.repository = repository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var etterkontroller = repository.finnKandidaterForAutomatiskEtterkontroll(Period.ZERO);

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
        var behandling = utledBehandling(kandidat);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");
        prosessTaskData.setPrioritet(100);

        // unik per task da det er ulike tasks for hver behandling
        prosessTaskData.setCallId(callId);

        prosessTaskTjeneste.lagre(prosessTaskData);
    }

    private Behandling utledBehandling(Etterkontroll kandidat) {
        if (kandidat.getBehandlingId() != null) {
            return behandlingRepository.hentBehandling(kandidat.getBehandlingId());
        }
        return behandlingRepository.finnSisteIkkeHenlagteBehandling(kandidat.getFagsakId()).orElseThrow();
    }
}
