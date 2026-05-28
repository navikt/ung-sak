package no.nav.ung.ytelse.ungdomsprogramytelsen.revurdering.varselopphorvedmaksdato;

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
 * Batchtask som varsler deltakere om opphør ved maksdato 4 uker før maksdato.
 * <p>
 * Kjører hver dag kl 07:30.
 */
@ApplicationScoped
@ProsessTask(value = VarselOpphørVedMaksdatoBatchTask.TASKNAME, maxFailedRuns = 1)
public class VarselOpphørVedMaksdatoBatchTask implements BatchProsessTaskHandler {

    public static final String TASKNAME = "batch.varselOpphorVedMaksdato";

    private ProsessTaskTjeneste prosessTaskTjeneste;

    VarselOpphørVedMaksdatoBatchTask() {
        // for CDI proxy
    }

    @Inject
    public VarselOpphørVedMaksdatoBatchTask(ProsessTaskTjeneste prosessTaskTjeneste) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        List<ProsessTaskData> feiletTask = prosessTaskTjeneste.finnAlle(VarselOpphørVedMaksdatoTask.TASKNAME, ProsessTaskStatus.FEILET).stream().filter(it -> it.getSaksnummer() == null).toList();
        List<ProsessTaskData> klarTask = prosessTaskTjeneste.finnAlle(VarselOpphørVedMaksdatoTask.TASKNAME, ProsessTaskStatus.KLAR).stream().filter(it -> it.getSaksnummer() == null).toList();
        List<ProsessTaskData> vetoTask = prosessTaskTjeneste.finnAlle(VarselOpphørVedMaksdatoTask.TASKNAME, ProsessTaskStatus.VETO).stream().filter(it -> it.getSaksnummer() == null).toList();
        if (!feiletTask.isEmpty() || !klarTask.isEmpty() || !vetoTask.isEmpty()) {
            return;
        }

        ProsessTaskData taskData = ProsessTaskData.forProsessTask(VarselOpphørVedMaksdatoTask.class);
        prosessTaskTjeneste.lagre(taskData);
    }

    @Override
    public CronExpression getCron() {
        return CronExpression.create("0 30 7 * * *");
    }
}

