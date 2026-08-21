package no.nav.ung.sak.behandling.revurdering.sats;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.prosesstask.api.TaskType;
import no.nav.k9.prosesstask.impl.cron.CronExpression;
import no.nav.ung.sak.behandling.prosessering.DuplikatbeskyttetBatchTask;
import no.nav.ung.sak.behandling.revurdering.inntektskontroll.OpprettRevurderingForInntektskontrollTask;


/**
 * Batchtask som oppretter revurderinger på saker der brukere fyller 25 år.
 * <p>
 * Kjører hver dag kl 07:15.
 */
@ApplicationScoped
@ProsessTask(value = OpprettRevurderingHøySatsBatchTask.TASKNAME, maxFailedRuns = 1)
public class OpprettRevurderingHøySatsBatchTask extends DuplikatbeskyttetBatchTask {

    public static final String TASKNAME = "batch.opprettRevurderingHøySats";

    OpprettRevurderingHøySatsBatchTask() {
        // for CDI proxy
    }

    @Inject
    public OpprettRevurderingHøySatsBatchTask(ProsessTaskTjeneste prosessTaskTjeneste) {
        super(prosessTaskTjeneste);
    }

    @Override
    protected TaskType getTaskType() {
        return new TaskType(OpprettRevurderingHøySatsTask.TASKNAME);
    }

    @Override
    public CronExpression getCron() {
        return CronExpression.create("0 15 7 * * *");
    }
}
