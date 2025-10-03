package no.nav.ung.sak.behandling.revurdering.sats;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.*;
import no.nav.k9.prosesstask.impl.cron.CronExpression;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


/**
 * Batchtask som oppretter revurderinger på saker der brukere fyller 25 år.
 * <p>
 * Kjører hver dag kl 07:15.
 */
@ApplicationScoped
@ProsessTask(value = OpprettRevurderingHøySatsBatchTask.TASKNAME, maxFailedRuns = 1)
public class OpprettRevurderingHøySatsBatchTask implements BatchProsessTaskHandler {

    public static final String TASKNAME = "batch.opprettRevurderingHøySats";
    private ProsessTaskTjeneste prosessTaskTjeneste;

    OpprettRevurderingHøySatsBatchTask() {
        // for CDI proxy
    }

    @Inject
    public OpprettRevurderingHøySatsBatchTask(ProsessTaskTjeneste prosessTaskTjeneste) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        LocalDate datoForKjøring = LocalDate.now();
        ProsessTaskData taskData = ProsessTaskData.forProsessTask(OpprettRevurderingHøySatsTask.class);
        taskData.setProperty(OpprettRevurderingHøySatsTask.DATO, datoForKjøring.format(DateTimeFormatter.ISO_LOCAL_DATE));
        prosessTaskTjeneste.lagre(taskData);
    }

    @Override
    public CronExpression getCron() {
        return CronExpression.create("0 15 7 * * *");
    }
}
