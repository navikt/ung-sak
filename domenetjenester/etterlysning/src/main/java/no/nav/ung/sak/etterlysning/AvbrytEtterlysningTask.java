package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.task.UnderBehandlingProsessTask;

@ApplicationScoped
@ProsessTask(value = AvbrytEtterlysningTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class AvbrytEtterlysningTask extends UnderBehandlingProsessTask {

    public static final String TASKTYPE = "etterlysning.avbryt";
    private EtterlysningProssesseringTjeneste etterlysningProssesseringTjeneste;

    public AvbrytEtterlysningTask() {
        // CDI
    }

    @Inject
    public AvbrytEtterlysningTask(BehandlingRepository behandlingRepository,
                                  BehandlingLåsRepository behandlingLåsRepository,
                                  EtterlysningProssesseringTjeneste etterlysningProssesseringTjeneste) {
        super(behandlingRepository, behandlingLåsRepository);
        this.etterlysningProssesseringTjeneste = etterlysningProssesseringTjeneste;
    }

    @Override
    protected void doProsesser(ProsessTaskData prosessTaskData, Behandling behandling) {
        etterlysningProssesseringTjeneste.settTilAvbrutt(behandling);

    }


}
