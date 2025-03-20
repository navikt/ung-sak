package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;

/**
 * Batchservice som finner alle etterlysninger som skal settes til utløpt, og setter alle til utløpt og kaller oppgave API for å avbryte tilhørende oppgave.
 */
@ApplicationScoped
@ProsessTask(value = AutomatiskSettEtterlysningerUtløptBatchTask.TASKTYPE, cronExpression = "0 5 7 * * *")
public class AutomatiskSettEtterlysningerUtløptBatchTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "batch.automatiskSettEtterlysningUtlopt";
    private AutomatiskUtløptEtterlysningTjeneste automatiskUtløptEtterlysningTjeneste;

    public AutomatiskSettEtterlysningerUtløptBatchTask() {
        // CDI
    }

    public AutomatiskSettEtterlysningerUtløptBatchTask(AutomatiskUtløptEtterlysningTjeneste automatiskUtløptEtterlysningTjeneste) {
        this.automatiskUtløptEtterlysningTjeneste = automatiskUtløptEtterlysningTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        automatiskUtløptEtterlysningTjeneste.settEtterlysningerUtløpt();
    }

}
