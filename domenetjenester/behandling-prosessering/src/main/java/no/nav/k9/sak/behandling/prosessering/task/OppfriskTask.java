package no.nav.k9.sak.behandling.prosessering.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandling.prosessering.BehandlingsprosessApplikasjonTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;

@ApplicationScoped
@ProsessTask(OppfriskTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class OppfriskTask extends BehandlingProsessTask {

    private static final String PROPERTY_FORCE = "force.innhent";

    private static final Logger log = LoggerFactory.getLogger(OppfriskTask.class);

    public static final String TASKTYPE = "behandling.oppfrisk";

    private BehandlingRepository repository;
    private BehandlingsprosessApplikasjonTjeneste behandlingsprosessApplikasjonTjeneste;

    OppfriskTask() {
        // for CDI proxy
    }

    @Inject
    public OppfriskTask(BehandlingLåsRepository behandlingLåsRepository, BehandlingRepository repository, BehandlingsprosessApplikasjonTjeneste behandlingsprosessApplikasjonTjeneste) {
        super(behandlingLåsRepository);
        this.repository = repository;
        this.behandlingsprosessApplikasjonTjeneste = behandlingsprosessApplikasjonTjeneste;
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData) {
        try {
            var behandling = repository.hentBehandling(prosessTaskData.getBehandlingId());
            logContext(behandling);
            boolean forceInnhent = Boolean.parseBoolean(prosessTaskData.getPropertyValue(PROPERTY_FORCE));
            behandlingsprosessApplikasjonTjeneste.asynkInnhentingAvRegisteropplysningerOgKjørProsess(behandling, forceInnhent);
        } catch (RuntimeException e) {
            log.info("Uventet feil ved oppfrisking av behandling.", e);
        }
    }

    public static ProsessTaskData create(Behandling behandling, boolean force) {
        final ProsessTaskData taskData =  ProsessTaskData.forProsessTask(OppfriskTask.class);
        taskData.setProperty(PROPERTY_FORCE, Boolean.toString(force));
        taskData.setCallIdFraEksisterende();
        taskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        return taskData;
    }
}
