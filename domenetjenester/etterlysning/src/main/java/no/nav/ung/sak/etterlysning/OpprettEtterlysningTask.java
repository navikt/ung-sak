package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;

@ApplicationScoped
@ProsessTask(value = OpprettEtterlysningTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class OpprettEtterlysningTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "etterlysning.opprett";
    public static final String ETTERLYSNING_ID = "etterlysning_id";


    @Override
    public void doTask(ProsessTaskData prosessTaskData) {

    }
}
