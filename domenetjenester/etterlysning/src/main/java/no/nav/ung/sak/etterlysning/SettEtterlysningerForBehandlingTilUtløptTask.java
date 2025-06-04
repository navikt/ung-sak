package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.task.UnderBehandlingProsessTask;

@ApplicationScoped
@ProsessTask(value = SettEtterlysningerForBehandlingTilUtløptTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class SettEtterlysningerForBehandlingTilUtløptTask extends UnderBehandlingProsessTask {

    public static final String TASKTYPE = "etterlysning.utlopt";
    private EtterlysningProssesseringTjeneste etterlysningProssesseringTjeneste;

    public SettEtterlysningerForBehandlingTilUtløptTask() {
        // CDI
    }


    @Inject
    public SettEtterlysningerForBehandlingTilUtløptTask(EtterlysningProssesseringTjeneste etterlysningProssesseringTjeneste) {
        this.etterlysningProssesseringTjeneste = etterlysningProssesseringTjeneste;
    }

    @Override
    protected void doProsesser(ProsessTaskData prosessTaskData, Behandling behandling) {
        etterlysningProssesseringTjeneste.settTilUtløpt(behandling.getId());
    }

}
