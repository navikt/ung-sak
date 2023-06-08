package no.nav.k9.sak.behandling.revurdering.etterkontroll.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.tjeneste.UtførKontrollTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.k9.sak.behandlingslager.task.FagsakProsessTask;

/**
 * @Dependent scope for å hente konfig ved hver kjøring.
 */
@Dependent
@ProsessTask(AutomatiskEtterkontrollTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class AutomatiskEtterkontrollTask extends FagsakProsessTask {
    public static final String TASKTYPE = "behandlingsprosess.etterkontroll";
    public static final String ETTERKONTROLL_ID = "etterkontrollId";

    //TODO denne blir aldri satt fra BatchTask'en når skal den brukes?
    public static final String KUN_AKTUELL_BEHANDLING = "etterkontrollBehandling";
    private static final Logger log = LoggerFactory.getLogger(AutomatiskEtterkontrollTask.class);
    private BehandlingRepository behandlingRepository;
    private UtførKontrollTjeneste utførKontrollTjeneste;


    AutomatiskEtterkontrollTask() {
        // for CDI proxy
    }

    @Inject
    public AutomatiskEtterkontrollTask(BehandlingRepositoryProvider repositoryProvider,// NOSONAR
                                       UtførKontrollTjeneste utførKontrollTjeneste) {
        super(repositoryProvider.getFagsakLåsRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.utførKontrollTjeneste = utførKontrollTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        Behandling behandlingForEtterkontroll = behandlingRepository
            .hentBehandling(prosessTaskData.getBehandlingId());

        BehandlingProsessTask.logContext(behandlingForEtterkontroll);
        utførKontrollTjeneste.utfør(behandlingForEtterkontroll,
            prosessTaskData.getPropertyValue(AutomatiskEtterkontrollTask.ETTERKONTROLL_ID));

    }

}
