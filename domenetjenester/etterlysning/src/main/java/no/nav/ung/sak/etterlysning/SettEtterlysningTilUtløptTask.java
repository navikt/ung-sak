package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;

@ApplicationScoped
@ProsessTask(value = SettEtterlysningTilUtløptTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class SettEtterlysningTilUtløptTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "etterlysning.utlopt";
    private EtterlysningProssesseringTjeneste etterlysningProssesseringTjeneste;

    public SettEtterlysningTilUtløptTask() {
        // CDI
    }

    @Inject
    public SettEtterlysningTilUtløptTask(EtterlysningProssesseringTjeneste etterlysningProssesseringTjeneste) {
        this.etterlysningProssesseringTjeneste = etterlysningProssesseringTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        etterlysningProssesseringTjeneste.settTilUtløpt(Long.parseLong(prosessTaskData.getBehandlingId()));
    }

}
