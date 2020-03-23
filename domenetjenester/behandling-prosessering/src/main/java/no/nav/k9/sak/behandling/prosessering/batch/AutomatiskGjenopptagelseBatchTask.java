package no.nav.k9.sak.behandling.prosessering.batch;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

/**
 * Batchservice som finner alle behandlinger som skal gjenopptas, og lager en ditto prosess task for hver.
 * Kriterier for gjenopptagelse: Behandlingen har et åpent aksjonspunkt som er et autopunkt og
 * har en frist som er passert.
 */
@ApplicationScoped
@ProsessTask(AutomatiskGjenopptagelseBatchTask.TASKTYPE)
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
