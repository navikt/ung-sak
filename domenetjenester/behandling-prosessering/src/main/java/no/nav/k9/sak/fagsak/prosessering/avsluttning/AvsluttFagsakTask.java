package no.nav.k9.sak.fagsak.prosessering.avsluttning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.k9.sak.behandlingslager.task.FagsakProsessTask;

@ApplicationScoped
@ProsessTask(AvsluttFagsakTask.TASKTYPE)
public class AvsluttFagsakTask extends FagsakProsessTask {

    public static final String TASKTYPE = "fagsak.avsluttFagsak";

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
        var taskData = new ProsessTaskData(TASKTYPE);
        taskData.setFagsak(fagsak.getId(), fagsak.getAktørId().getAktørId());

        return taskData;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        fagsakAvsluttningTjeneste.avsluttFagsak(prosessTaskData.getFagsakId());
    }
}
