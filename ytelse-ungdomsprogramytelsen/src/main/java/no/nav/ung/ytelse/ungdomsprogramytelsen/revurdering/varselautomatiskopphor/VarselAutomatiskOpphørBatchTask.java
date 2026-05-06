package no.nav.ung.ytelse.ungdomsprogramytelsen.revurdering.varselautomatiskopphor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.BatchProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskStatus;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.prosesstask.impl.cron.CronExpression;

import java.util.List;

/**
 * Batchtask som varsler deltakere om automatisk opphør 4 uker før maksdato.
 * <p>
 * Kjører hver dag kl 07:30.
 */
@ApplicationScoped
@ProsessTask(value = VarselAutomatiskOpphørBatchTask.TASKNAME, maxFailedRuns = 1)
public class VarselAutomatiskOpphørBatchTask implements BatchProsessTaskHandler {

    public static final String TASKNAME = "batch.varselAutomatiskOpphor";

    private ProsessTaskTjeneste prosessTaskTjeneste;

    VarselAutomatiskOpphørBatchTask() {
        // for CDI proxy
    }

    @Inject
    public VarselAutomatiskOpphørBatchTask(ProsessTaskTjeneste prosessTaskTjeneste) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        List<ProsessTaskData> feiletTask = prosessTaskTjeneste.finnAlle(VarselAutomatiskOpphørTask.TASKNAME, ProsessTaskStatus.FEILET).stream().filter(it -> it.getSaksnummer() == null).toList();
        List<ProsessTaskData> klarTask = prosessTaskTjeneste.finnAlle(VarselAutomatiskOpphørTask.TASKNAME, ProsessTaskStatus.KLAR).stream().filter(it -> it.getSaksnummer() == null).toList();
        List<ProsessTaskData> vetoTask = prosessTaskTjeneste.finnAlle(VarselAutomatiskOpphørTask.TASKNAME, ProsessTaskStatus.VETO).stream().filter(it -> it.getSaksnummer() == null).toList();
        if (!feiletTask.isEmpty() || !klarTask.isEmpty() || !vetoTask.isEmpty()) {
            return;
        }

        ProsessTaskData taskData = ProsessTaskData.forProsessTask(VarselAutomatiskOpphørTask.class);
        prosessTaskTjeneste.lagre(taskData);
    }

    @Override
    public CronExpression getCron() {
        return CronExpression.create("0 30 7 * * *");
    }
}

