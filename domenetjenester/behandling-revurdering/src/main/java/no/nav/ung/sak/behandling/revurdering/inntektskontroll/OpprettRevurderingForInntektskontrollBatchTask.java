package no.nav.ung.sak.behandling.revurdering.inntektskontroll;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.prosesstask.api.TaskType;
import no.nav.k9.prosesstask.impl.cron.CronExpression;
import no.nav.ung.sak.behandling.prosessering.DuplikatbeskyttetBatchTask;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Objects;


/**
 * Batchtask som starter kontroll av inntekt fra a-inntekt
 * <p>
 * Kjører den åttende i måneden kl 07:00.
 */
@ApplicationScoped
@ProsessTask(value = OpprettRevurderingForInntektskontrollBatchTask.TASKNAME, maxFailedRuns = 1)
public class OpprettRevurderingForInntektskontrollBatchTask extends DuplikatbeskyttetBatchTask {

    public static final String TASKNAME = "batch.opprettRevurderingForInntektskontrollBatch";


    private String inntetskontrollCronString;

    OpprettRevurderingForInntektskontrollBatchTask() {
    }

    @Inject
    public OpprettRevurderingForInntektskontrollBatchTask(ProsessTaskTjeneste prosessTaskTjeneste,
                                                          @KonfigVerdi(value = "INNTEKTSKONTROLL_CRON_EXPRESSION", defaultVerdi = "0 0 7 8 * *") String inntetskontrollCronString) {
        super(prosessTaskTjeneste);
        this.inntetskontrollCronString = inntetskontrollCronString;
    }

    @Override
    public CronExpression getCron() {
        return CronExpression.create(inntetskontrollCronString);
    }


    @Override
    protected TaskType getTaskType() {
        return new TaskType(OpprettRevurderingForInntektskontrollTask.TASKNAME);
    }

    @Override
    protected void leggTilProperties(ProsessTaskData childTask) {
        var forrigeMåned = YearMonth.now().minusMonths(1);
        childTask.setProperty(OpprettRevurderingForInntektskontrollTask.PERIODE_FOM, forrigeMåned.atDay(1).format(DateTimeFormatter.ISO_LOCAL_DATE));
        childTask.setProperty(OpprettRevurderingForInntektskontrollTask.PERIODE_TOM, forrigeMåned.atEndOfMonth().format(DateTimeFormatter.ISO_LOCAL_DATE));
    }

    @Override
    protected boolean erDuplikat(ProsessTaskData data) {
        var forrigeMåned = YearMonth.now().minusMonths(1);
        return Objects.equals(
            data.getPropertyValue(OpprettRevurderingForInntektskontrollTask.PERIODE_FOM),
            forrigeMåned.atDay(1).format(DateTimeFormatter.ISO_LOCAL_DATE));
    }
}
