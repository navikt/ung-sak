package no.nav.k9.sak.produksjonsstyring.oppgavebehandling.task;

import static no.nav.k9.sak.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveForBehandlingSendtTilbakeTask.TASKTYPE;

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
        String oppgaveId = oppgaveTjeneste.opprettBehandleOppgaveForBehandlingMedPrioritetOgFrist(prosessTaskData.getBehandlingId(),
            beskrivelse, true, 0);
        if (oppgaveId != null) {
            log.info("Oppgave opprettet i GSAK for å behandle sak sendt tilbake. Oppgavenummer: {}", oppgaveId); //NOSONAR
        }
    }
}
