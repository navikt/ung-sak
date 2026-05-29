package no.nav.ung.ytelse.aktivitetspenger.revurdering.sats;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.prosesstask.impl.cron.CronExpression;
import no.nav.ung.sak.behandling.prosessering.DuplikatbeskyttetBatchTask;

@ApplicationScoped
@ProsessTask(value = AktivitetspengerOpprettRevurderingHøySatsBatchTask.TASKNAME, maxFailedRuns = 1)
public class AktivitetspengerOpprettRevurderingHøySatsBatchTask extends DuplikatbeskyttetBatchTask {

    public static final String TASKNAME = "batch.opprettRevurderingHøySatsAktivitetspenger";

    AktivitetspengerOpprettRevurderingHøySatsBatchTask() {
    }

    @Inject
    public AktivitetspengerOpprettRevurderingHøySatsBatchTask(ProsessTaskTjeneste prosessTaskTjeneste) {
        super(prosessTaskTjeneste);
    }

    @Override
    protected String childTaskName() {
        return AktivitetspengerOpprettRevurderingHøySatsTask.TASKNAME;
    }

    @Override
    protected ProsessTaskData createChildTaskData() {
        return ProsessTaskData.forProsessTask(AktivitetspengerOpprettRevurderingHøySatsTask.class);
    }

    @Override
    public CronExpression getCron() {
        return CronExpression.create("0 20 7 * * *");
    }
}
