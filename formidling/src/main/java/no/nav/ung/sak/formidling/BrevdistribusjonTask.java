package no.nav.ung.sak.formidling;


import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;

/**
 *
 */
//@ApplicationScoped
@ProsessTask(value = BrevdistribusjonTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class BrevdistribusjonTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "formidling.brevdistribusjon";

    static final String BREVBESTILLING_ID_PARAM = "brevbestillingId";

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {

    }
}
