package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskStatus;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.task.BehandlingProsessTask;
import org.slf4j.Logger;

import java.util.Set;

@ApplicationScoped
@ProsessTask(value = SettEtterlysningTilUtløptDersomVenterTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class SettEtterlysningTilUtløptDersomVenterTask extends BehandlingProsessTask {

    public static final String TASKTYPE = "etterlysning.planlagt.settUtlopt";

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(SettEtterlysningTilUtløptDersomVenterTask.class);

    private BehandlingRepository behandlingRepository;
    private ProsessTaskTjeneste taskTjeneste;

    public SettEtterlysningTilUtløptDersomVenterTask() {
        // CDI
    }

    @Inject
    public SettEtterlysningTilUtløptDersomVenterTask(BehandlingRepository behandlingRepository,
                                                     BehandlingLåsRepository behandlingLåsRepository,
                                                     ProsessTaskTjeneste taskTjeneste) {
        super(behandlingLåsRepository);
        this.behandlingRepository = behandlingRepository;
        this.taskTjeneste = taskTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        var behandlingId = prosessTaskData.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (behandling.erStatusFerdigbehandlet()) {
            LOG.info("Behandling var ferdigbehandlet, gjør ingenting");
            return;
        }
        var harKlarTask = taskTjeneste.finnAlle(SettEtterlysningerForBehandlingTilUtløptTask.TASKTYPE, ProsessTaskStatus.KLAR).stream().anyMatch(it -> it.getBehandlingId().equals(behandlingId));
        var harVetoTask = taskTjeneste.finnAlle(SettEtterlysningerForBehandlingTilUtløptTask.TASKTYPE, ProsessTaskStatus.VETO).stream().anyMatch(it -> it.getBehandlingId().equals(behandlingId));
        if (harKlarTask || harVetoTask) {
            LOG.info("Det finnes allerede en task for å sette etterlysninger til utløpt for behandling {}, hopper over opprettelse av ny task", behandling);
            return;
        }
        var nyTaskData = ProsessTaskData.forProsessTask(SettEtterlysningerForBehandlingTilUtløptTask.class);
        nyTaskData.setBehandling(behandling.getFagsakId(), behandling.getId());
        taskTjeneste.lagre(nyTaskData);
    }

}
