package no.nav.k9.sak.innsyn.hendelse;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.innsyn.BrukerdialoginnsynMeldingProducer;

/**
 * For å sende en enkel behandling til innsyn
 */
@ApplicationScoped
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
@ProsessTask(PubliserInnsynEventTask.TASKTYPE)
public class PubliserInnsynEventTask implements ProsessTaskHandler {
    private static final Logger log = LoggerFactory.getLogger(PubliserInnsynEventTask.class);
    public static final String TASKTYPE = "innsyn.PubliserInnsynEvent";

    private InnsynEventTjeneste innsynEventTjeneste;

    PubliserInnsynEventTask() {
        // for CDI proxy
    }

    @Inject
    public PubliserInnsynEventTask(InnsynEventTjeneste innsynEventTjeneste, BrukerdialoginnsynMeldingProducer producer) {
        this.innsynEventTjeneste = innsynEventTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        Objects.requireNonNull(prosessTaskData.getBehandlingId());
        innsynEventTjeneste.publiserBehandling(Long.valueOf(prosessTaskData.getBehandlingId()));
    }

}
