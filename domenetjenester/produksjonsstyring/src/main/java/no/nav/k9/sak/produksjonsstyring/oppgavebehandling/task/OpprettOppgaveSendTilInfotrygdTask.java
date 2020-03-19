package no.nav.k9.sak.produksjonsstyring.oppgavebehandling.task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(OpprettOppgaveSendTilInfotrygdTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class OpprettOppgaveSendTilInfotrygdTask extends BehandlingProsessTask {
    public static final String TASKTYPE = "oppgavebehandling.opprettOppgaveSakTilInfotrygd";
    private static final Logger log = LoggerFactory.getLogger(OpprettOppgaveSendTilInfotrygdTask.class);

    private OppgaveTjeneste oppgaveTjeneste;

    OpprettOppgaveSendTilInfotrygdTask() {
        // for CDI proxy
    }

    @Inject
    public OpprettOppgaveSendTilInfotrygdTask(OppgaveTjeneste oppgaveTjeneste, BehandlingRepositoryProvider repositoryProvider) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        String oppgaveId = oppgaveTjeneste.opprettOppgaveSakSkalTilInfotrygd(prosessTaskData.getBehandlingId());
        log.info("Oppgave opprettet i GSAK slik at Infotrygd kan behandle saken videre. Oppgavenummer: {}", oppgaveId);
    }
}
