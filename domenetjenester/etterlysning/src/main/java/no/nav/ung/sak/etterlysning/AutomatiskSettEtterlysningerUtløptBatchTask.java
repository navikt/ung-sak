package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.BatchProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.impl.cron.CronExpression;

/**
 * Batchservice som finner alle etterlysninger som skal settes til utløpt, og setter alle til utløpt og kaller oppgave API for å avbryte tilhørende oppgave.
 */
@ApplicationScoped
@ProsessTask(value = AutomatiskSettEtterlysningerUtløptBatchTask.TASKTYPE)
public class AutomatiskSettEtterlysningerUtløptBatchTask implements BatchProsessTaskHandler {

    public static final String TASKTYPE = "batch.automatiskSettEtterlysningUtlopt";
    private AutomatiskUtløptEtterlysningTjeneste automatiskUtløptEtterlysningTjeneste;

    public AutomatiskSettEtterlysningerUtløptBatchTask() {
        // CDI
    }

    @Inject
    public AutomatiskSettEtterlysningerUtløptBatchTask(AutomatiskUtløptEtterlysningTjeneste automatiskUtløptEtterlysningTjeneste) {
        this.automatiskUtløptEtterlysningTjeneste = automatiskUtløptEtterlysningTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        automatiskUtløptEtterlysningTjeneste.settEtterlysningerUtløpt();
    }

    @Override
    public CronExpression getCron() {
        return CronExpression.create("0 5 7 * * *");
    }

}
