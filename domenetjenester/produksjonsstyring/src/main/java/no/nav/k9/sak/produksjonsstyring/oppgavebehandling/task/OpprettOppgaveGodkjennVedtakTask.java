package no.nav.k9.sak.produksjonsstyring.oppgavebehandling.task;

import static no.nav.k9.kodeverk.produksjonsstyring.OppgaveÅrsak.GODKJENNE_VEDTAK;
import static no.nav.k9.sak.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveGodkjennVedtakTask.TASKTYPE;

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
@ProsessTask(TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class OpprettOppgaveGodkjennVedtakTask extends BehandlingProsessTask {
    public static final String TASKTYPE = "oppgavebehandling.opprettOppgaveGodkjennVedtak";
    private static final Logger log = LoggerFactory.getLogger(OpprettOppgaveGodkjennVedtakTask.class);
    private OppgaveTjeneste oppgaveTjeneste;

    OpprettOppgaveGodkjennVedtakTask() {
        // for CDI proxy
    }

    @Inject
    public OpprettOppgaveGodkjennVedtakTask(OppgaveTjeneste oppgaveTjeneste, BehandlingRepositoryProvider repositoryProvider) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        String oppgaveId = oppgaveTjeneste.opprettBasertPåBehandlingId(prosessTaskData.getBehandlingId(), GODKJENNE_VEDTAK);
        if (oppgaveId != null) {
            log.info("Oppgave opprettet i GSAK for å godkjenne vedtak. Oppgavenummer: {}", oppgaveId); //NOSONAR
        }
    }
}
