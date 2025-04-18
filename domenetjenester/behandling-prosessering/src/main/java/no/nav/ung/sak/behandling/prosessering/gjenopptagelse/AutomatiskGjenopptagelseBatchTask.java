package no.nav.ung.sak.behandling.prosessering.gjenopptagelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;

/**
 * Batchservice som finner alle behandlinger som skal gjenopptas, og lager en ditto prosess task for hver.
 * Kriterier for gjenopptagelse: Behandlingen har et åpent aksjonspunkt som er et autopunkt og
 * har en frist som er passert.
 */
@ApplicationScoped
@ProsessTask(value = AutomatiskGjenopptagelseBatchTask.TASKTYPE, cronExpression = "0 0 7 * * *")
public class AutomatiskGjenopptagelseBatchTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "batch.automatiskGjenopptaglese";
    private AutomatiskGjenopptagelseTjeneste automatiskGjenopptagelseTjeneste;

    AutomatiskGjenopptagelseBatchTask() {
        // CDI
    }

    @Inject
    public AutomatiskGjenopptagelseBatchTask(AutomatiskGjenopptagelseTjeneste automatiskGjenopptagelseTjeneste) {
        this.automatiskGjenopptagelseTjeneste = automatiskGjenopptagelseTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        automatiskGjenopptagelseTjeneste.gjenopptaBehandlinger();
    }
}
