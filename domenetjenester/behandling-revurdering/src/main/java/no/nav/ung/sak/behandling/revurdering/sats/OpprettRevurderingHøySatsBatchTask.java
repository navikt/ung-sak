package no.nav.ung.sak.behandling.revurdering.sats;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.*;
import no.nav.k9.prosesstask.impl.cron.CronExpression;

import java.util.List;


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

        List<ProsessTaskData> feiletTask = prosessTaskTjeneste.finnAlle(OpprettRevurderingHøySatsTask.TASKNAME, ProsessTaskStatus.FEILET).stream().filter(it -> it.getFagsakId() == null).toList();
        List<ProsessTaskData> klarTask = prosessTaskTjeneste.finnAlle(OpprettRevurderingHøySatsTask.TASKNAME, ProsessTaskStatus.KLAR).stream().filter(it -> it.getFagsakId() == null).toList();
        List<ProsessTaskData> vetoTask = prosessTaskTjeneste.finnAlle(OpprettRevurderingHøySatsTask.TASKNAME, ProsessTaskStatus.VETO).stream().filter(it -> it.getFagsakId() == null).toList();
        if (!feiletTask.isEmpty() || !klarTask.isEmpty() || !vetoTask.isEmpty()) {
            // Hvis det finnes noen task i noen av disse statusene, så betyr det at de enten kjører, eller skal kjøres.
            // Vi ønsker ikke å opprette duplikater av disse.
            return;
        }

        ProsessTaskData taskData = ProsessTaskData.forProsessTask(OpprettRevurderingHøySatsTask.class);
        prosessTaskTjeneste.lagre(taskData);
    }

    @Override
    public CronExpression getCron() {
        return CronExpression.create("0 15 7 * * *");
    }
}
