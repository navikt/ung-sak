package no.nav.ung.sak.behandling.revurdering.inntektskontroll;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;


/**
 * Batchtask som starter kontroll av inntekt fra a-inntekt
 * <p>
 * Kjører hver dag kl 07:15.
 */
@ApplicationScoped
@ProsessTask(value = OpprettRevurderingForInntektskontrollBatchTask.TASKNAME, cronExpression = "0 7 7 * *", maxFailedRuns = 1)
public class OpprettRevurderingForInntektskontrollBatchTask implements ProsessTaskHandler {

    public static final String TASKNAME = "batch.opprettRevurderingForInntektskontroll";

    private static final Logger log = LoggerFactory.getLogger(OpprettRevurderingForInntektskontrollBatchTask.class);


    OpprettRevurderingForInntektskontrollBatchTask() {
        // for CDI proxy
    }


    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        // Revurdere alle saker der man har løpende ytelse


    }


}
