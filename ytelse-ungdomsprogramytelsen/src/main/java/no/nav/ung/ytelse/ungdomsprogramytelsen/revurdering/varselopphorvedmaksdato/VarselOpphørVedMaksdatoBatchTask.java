package no.nav.ung.ytelse.ungdomsprogramytelsen.revurdering.varselopphorvedmaksdato;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.prosesstask.api.TaskType;
import no.nav.k9.prosesstask.impl.cron.CronExpression;
import no.nav.ung.sak.behandling.prosessering.DuplikatbeskyttetBatchTask;

/**
 * Batchtask som varsler deltakere om opphør ved maksdato 3 uker før maksdato.
 * <p>
 * Kjører hver dag kl 07:30.
 */
@ApplicationScoped
@ProsessTask(value = VarselOpphørVedMaksdatoBatchTask.TASKNAME, maxFailedRuns = 1)
public class VarselOpphørVedMaksdatoBatchTask extends DuplikatbeskyttetBatchTask {

    public static final String TASKNAME = "batch.varselOpphorVedMaksdato";

    VarselOpphørVedMaksdatoBatchTask() {
        // for CDI proxy
    }

    @Inject
    public VarselOpphørVedMaksdatoBatchTask(ProsessTaskTjeneste prosessTaskTjeneste) {
        super(prosessTaskTjeneste);
    }

    @Override
    protected TaskType getTaskType() {
        return new TaskType(VarselOpphørVedMaksdatoTask.TASKNAME);
    }

    @Override
    public CronExpression getCron() {
        return CronExpression.create("0 30 7 * * *");
    }
}
