package no.nav.ung.sak.behandling.revurdering.sats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;


/**
 * Batchtask som oppretter revurderinger på saker der brukere fyller 25 år.
 * <p>
 * Kjører hver dag kl 07:15.
 */
@ApplicationScoped
@ProsessTask(value = OpprettRevurderingHøySatsBatchTask.TASKNAME, cronExpression = "0 15 7 * * *", maxFailedRuns = 1)
public class OpprettRevurderingHøySatsBatchTask implements ProsessTaskHandler {

    public static final String TASKNAME = "batch.opprettRevurderingHøySats";

    private static final Logger log = LoggerFactory.getLogger(OpprettRevurderingHøySatsBatchTask.class);
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
        var task = ProsessTaskData.forProsessTask(OpprettRevurderingHøySatsBatchTask.class);
        prosessTaskTjeneste.lagre(task);
    }

}
