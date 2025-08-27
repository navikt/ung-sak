package no.nav.ung.sak.metrikker.bigquery;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.util.Tuple;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.sak.metrikker.bigquery.tabeller.BigQueryTabell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

/**
 * Task for publisering av metrikker til BigQuery.
 * Denne tasken henter daglig rapporterte metrikker fra BigQueryStatistikkRepository og publiserer dem til BigQuery.
 * Det er satt opp en cron-jobb som kjører denne tasken kl. 07:00 hver dag.
 * Det er også satt en grense for maksimalt antall mislykkede kjøringer til 20.
 */
@ApplicationScoped
@ProsessTask(value = BigQueryMetrikkDagligTask.TASKTYPE, cronExpression = "0 0 7 * * *", maxFailedRuns = 20, firstDelay = 60)
public class BigQueryMetrikkDagligTask implements ProsessTaskHandler {

    static final String TASKTYPE = "bigquery.metrikk.daglig.task";

    private static final Logger log = LoggerFactory.getLogger(BigQueryMetrikkDagligTask.class);
    private boolean bigQueryEnabled;

    private BigQueryKlient bigQueryKlient;

    private BigQueryStatistikkRepository statistikkRepository;

    BigQueryMetrikkDagligTask() {
        // for proxyd
    }

    @Inject
    public BigQueryMetrikkDagligTask(@KonfigVerdi(value = "BIGQUERY_ENABLED", required = false, defaultVerdi = "false") boolean bigQueryEnabled, BigQueryKlient bigQueryKlient, BigQueryStatistikkRepository statistikkRepository) {
        this.bigQueryKlient = bigQueryKlient;
        this.statistikkRepository = statistikkRepository;
        this.bigQueryEnabled = bigQueryEnabled;
    }

    @Override
    public void doTask(ProsessTaskData data) {
        long startTime = System.nanoTime();

        try {
            List<Tuple<BigQueryTabell<?>, Collection<?>>> metrikker = statistikkRepository.hentDagligRapporterte();

            if (bigQueryEnabled && !metrikker.isEmpty()) {
                publiserMetrikker(BigQueryDataset.UNG_SAK_STATISTIKK_DATASET, metrikker);
            } else {
                log.info("Ingen metrikker å publisere eller BigQuery er ikke aktivert.");
            }

        } finally {
            var varighet = Duration.ofNanos(System.nanoTime() - startTime);
            if (Duration.ofSeconds(20).minus(varighet).isNegative()) {
                // bruker for lang tid på logging av metrikker.
                log.warn("Generering av BigQuery metrikker tok : " + varighet);
            }
        }
    }

    private void publiserMetrikker(BigQueryDataset dataset, List<Tuple<BigQueryTabell<?>, Collection<?>>> metrikker) {
        metrikker.forEach(tuple -> {
            BigQueryTabell<BigQueryRecord> tabell = (BigQueryTabell<BigQueryRecord>) tuple.getElement1();
            Collection<BigQueryRecord> records = (Collection<BigQueryRecord>) tuple.getElement2();

            bigQueryKlient.publish(dataset, tabell, records);
        });
    }
}
