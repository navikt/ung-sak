package no.nav.k9.sak.fagsak.prosessering.avsluttning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.FagsakProsessTask;

@ApplicationScoped
@ProsessTask(value = "fagsak.avsluttFagsak", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class AvsluttFagsakTask extends FagsakProsessTask {

    private FagsakAvsluttningTjeneste fagsakAvsluttningTjeneste;

    AvsluttFagsakTask() {
        // CDI
    }

    @Inject
    public AvsluttFagsakTask(FagsakLåsRepository fagsakLåsRepository, BehandlingLåsRepository behandlingLåsRepository, FagsakAvsluttningTjeneste fagsakAvsluttningTjeneste) {
        super(fagsakLåsRepository, behandlingLåsRepository);
        this.fagsakAvsluttningTjeneste = fagsakAvsluttningTjeneste;
    }

    static ProsessTaskData opprettTask(Fagsak fagsak) {
        var taskData = ProsessTaskData.forProsessTask(AvsluttFagsakTask.class);
        taskData.setFagsak(fagsak.getId(), fagsak.getAktørId().getAktørId());

        return taskData;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        fagsakAvsluttningTjeneste.avsluttFagsak(prosessTaskData.getFagsakId());
    }
}
