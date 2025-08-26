package no.nav.ung.sak.produksjonsstyring.oppgavebehandling.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@ProsessTask(OpprettOppgaveForBehandlingSendtTilbakeTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class OpprettOppgaveForBehandlingSendtTilbakeTask extends BehandlingProsessTask {
    public static final String TASKTYPE = "oppgavebehandling.opprettOppgaveSakSendtTilbake";
    private static final Logger log = LoggerFactory.getLogger(OpprettOppgaveForBehandlingSendtTilbakeTask.class);
    private OppgaveTjeneste oppgaveTjeneste;

    OpprettOppgaveForBehandlingSendtTilbakeTask() {
        // for CDI proxy
    }

    @Inject
    public OpprettOppgaveForBehandlingSendtTilbakeTask(BehandlingRepositoryProvider repositoryProvider, OppgaveTjeneste oppgaveTjeneste) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        String beskrivelse = "Sak har blitt sendt tilbake fra beslutter";
        String oppgaveId = oppgaveTjeneste.opprettBehandleOppgaveForBehandlingMedPrioritetOgFrist(
            prosessTaskData.getBehandlingId(),
            beskrivelse,
            true,
            0
        );

        if (oppgaveId != null) {
            log.info("Oppgave opprettet for å behandle sak sendt tilbake. Oppgavenummer: {}", oppgaveId);
        }
    }
}
