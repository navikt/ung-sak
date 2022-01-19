package no.nav.k9.sak.produksjonsstyring.oppgavebehandling.task;

import static no.nav.k9.sak.produksjonsstyring.oppgavebehandling.task.AvsluttOppgaveTaskProperties.TASKTYPE;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class AvsluttOppgaveTask extends BehandlingProsessTask {
    private OppgaveTjeneste oppgaveTjeneste;
    private BehandlingRepository behandlingRepository;

    AvsluttOppgaveTask() {
        // for CDI proxy
    }

    @Inject
    public AvsluttOppgaveTask(OppgaveTjeneste oppgaveTjeneste, BehandlingRepositoryProvider repositoryProvider) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        var behandlingId = prosessTaskData.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        logContext(behandling);

        String oppgaveId = prosessTaskData.getOppgaveId()
            .orElseThrow(() -> new IllegalStateException("Mangler oppgaveId"));

        oppgaveTjeneste.avslutt(Long.valueOf(prosessTaskData.getBehandlingId()), oppgaveId);
    }
}
