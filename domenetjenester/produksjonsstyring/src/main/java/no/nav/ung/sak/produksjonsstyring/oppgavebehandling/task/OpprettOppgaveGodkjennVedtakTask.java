package no.nav.ung.sak.produksjonsstyring.oppgavebehandling.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ApplicationScoped
@ProsessTask(OpprettOppgaveGodkjennVedtakTask.TASKTYPE)
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
        String oppgaveId = oppgaveTjeneste.opprettBasertPåBehandlingId(prosessTaskData.getBehandlingId(), OppgaveÅrsak.GODKJENN_VEDTAK_VL);
        if (oppgaveId != null) {
            log.info("Oppgave opprettet for å godkjenne vedtak. Oppgavenummer: {}", oppgaveId);
        }
    }
}
