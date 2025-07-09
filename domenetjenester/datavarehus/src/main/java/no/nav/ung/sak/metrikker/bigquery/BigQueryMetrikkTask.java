package no.nav.ung.sak.metrikker.bigquery;

import com.google.cloud.bigquery.InsertAllRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;

/**
 * Task for publisering av metrikker til BigQuery.
 * Denne tasken henter hyppig rapporterte metrikker fra BigQueryStatistikkRepository og publiserer dem til BigQuery.
 * Det er satt opp en cron-jobb som kjører denne tasken hvert 5. minutt.
 * Det er også satt en grense for maksimalt antall mislykkede kjøringer til 20.
 */
@ApplicationScoped
@ProsessTask(value = BigQueryMetrikkTask.TASKTYPE, cronExpression = "0 */5 * * * *", maxFailedRuns = 20, firstDelay = 60)
public class BigQueryMetrikkTask implements ProsessTaskHandler {

    private static final int LOG_THRESHOLD = 5000;

    static final String TASKTYPE = "bigquery.metrikk.task";

    private static final Logger log = LoggerFactory.getLogger(BigQueryMetrikkTask.class);

    private BigQueryKlient bigQueryKlient;

    private BigQueryStatistikkRepository statistikkRepository;

    BigQueryMetrikkTask() {
        // for proxyd
    }

    @Inject
    public BigQueryMetrikkTask(BigQueryKlient bigQueryKlient, BigQueryStatistikkRepository statistikkRepository) {
        this.bigQueryKlient = bigQueryKlient;
        this.statistikkRepository = statistikkRepository;
    }

    @Override
    public void doTask(ProsessTaskData data) {
        long startTime = System.nanoTime();

        try {
            Map<BigQueryTable, JSONObject> metrikker = statistikkRepository.hentHyppigRapporterte();

            publiserMetrikker(BigQueryDataset.UNG_SAK_STATISTIKK_DATASET, metrikker);

        } finally {
            var varighet = Duration.ofNanos(System.nanoTime() - startTime);
            if (Duration.ofSeconds(20).minus(varighet).isNegative()) {
                // bruker for lang tid på logging av metrikker.
                log.warn("Generering av BigQuery metrikker tok : " + varighet);
            }
        }
    }

    private void publiserMetrikker(BigQueryDataset dataset, Map<BigQueryTable, JSONObject> metrikker) {
        metrikker.forEach((bigQueryTable, data) -> bigQueryKlient.publish(dataset, bigQueryTable, tilRowInsert(data)));
    }

    private InsertAllRequest.RowToInsert tilRowInsert(JSONObject jsonObject) {
        return InsertAllRequest.RowToInsert.of(jsonObject.toMap());
    }
}
