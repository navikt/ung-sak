package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;

@ApplicationScoped
@ProsessTask(value = AvbrytEtterlysningTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class AvbrytEtterlysningTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "etterlysning.avbryt";
    private EtterlysningProssesseringTjeneste etterlysningProssesseringTjeneste;

    public AvbrytEtterlysningTask() {
        // CDI
    }

    @Inject
    public AvbrytEtterlysningTask(EtterlysningProssesseringTjeneste etterlysningProssesseringTjeneste) {
        this.etterlysningProssesseringTjeneste = etterlysningProssesseringTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        etterlysningProssesseringTjeneste.settTilAvbrutt(Long.parseLong(prosessTaskData.getBehandlingId()));
    }

}
