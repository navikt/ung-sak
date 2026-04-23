package no.nav.ung.ytelse.aktivitetspenger.revurdering.sats;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.*;
import no.nav.k9.prosesstask.impl.cron.CronExpression;

import java.util.List;

@ApplicationScoped
@ProsessTask(value = AktivitetspengerOpprettRevurderingHøySatsBatchTask.TASKNAME, maxFailedRuns = 1)
public class AktivitetspengerOpprettRevurderingHøySatsBatchTask implements BatchProsessTaskHandler {

    public static final String TASKNAME = "batch.opprettRevurderingHøySatsAktivitetspenger";
    private ProsessTaskTjeneste prosessTaskTjeneste;

    AktivitetspengerOpprettRevurderingHøySatsBatchTask() {
    }

    @Inject
    public AktivitetspengerOpprettRevurderingHøySatsBatchTask(ProsessTaskTjeneste prosessTaskTjeneste) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        List<ProsessTaskData> feiletTask = prosessTaskTjeneste.finnAlle(AktivitetspengerOpprettRevurderingHøySatsTask.TASKNAME, ProsessTaskStatus.FEILET).stream().filter(it -> it.getSaksnummer() == null).toList();
        List<ProsessTaskData> klarTask = prosessTaskTjeneste.finnAlle(AktivitetspengerOpprettRevurderingHøySatsTask.TASKNAME, ProsessTaskStatus.KLAR).stream().filter(it -> it.getSaksnummer() == null).toList();
        List<ProsessTaskData> vetoTask = prosessTaskTjeneste.finnAlle(AktivitetspengerOpprettRevurderingHøySatsTask.TASKNAME, ProsessTaskStatus.VETO).stream().filter(it -> it.getSaksnummer() == null).toList();
        if (!feiletTask.isEmpty() || !klarTask.isEmpty() || !vetoTask.isEmpty()) {
            return;
        }

        ProsessTaskData taskData = ProsessTaskData.forProsessTask(AktivitetspengerOpprettRevurderingHøySatsTask.class);
        prosessTaskTjeneste.lagre(taskData);
    }

    @Override
    public CronExpression getCron() {
        return CronExpression.create("0 20 7 * * *");
    }
}

