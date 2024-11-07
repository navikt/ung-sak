package no.nav.k9.sak.innsyn.hendelse;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;

/**
 * For å sende en enkel behandling til innsyn
 */
@ApplicationScoped
@ProsessTask(PubliserInnsynEventTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class PubliserInnsynEventTask implements ProsessTaskHandler {
    private static final Logger log = LoggerFactory.getLogger(PubliserInnsynEventTask.class);
    public static final String TASKTYPE = "innsyn.PubliserInnsynEvent";

    private InnsynEventTjeneste innsynEventTjeneste;
    private BehandlingRepository behandlingRepository;

    PubliserInnsynEventTask() {
        // for CDI proxy
    }

    @Inject
    public PubliserInnsynEventTask(
            InnsynEventTjeneste innsynEventTjeneste, BehandlingRepository behandlingRepository) {
        this.innsynEventTjeneste = innsynEventTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        Objects.requireNonNull(prosessTaskData.getBehandlingId());
        Behandling behandling = behandlingRepository.hentBehandling(Long.valueOf(prosessTaskData.getBehandlingId()));
        BehandlingProsessTask.logContext(behandling);
        innsynEventTjeneste.publiserBehandling(behandling);
    }

}
