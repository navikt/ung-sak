package no.nav.ung.sak.metrikker.bigquery;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.util.Tuple;
import no.nav.k9.prosesstask.api.*;
import no.nav.ung.sak.metrikker.bigquery.tabeller.BigQueryTabell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static no.nav.ung.kodeverk.uttak.Tid.TIDENES_BEGYNNELSE;

/**
 * Task for publisering av metrikker til BigQuery.
 * Denne tasken henter hyppig rapporterte metrikker fra BigQueryStatistikkRepository og publiserer dem til BigQuery.
 * Det er satt opp en cron-jobb som kjører denne tasken en gang i timen.
 * Det er også satt en grense for maksimalt antall mislykkede kjøringer til 20.
 */
@ApplicationScoped
@ProsessTask(value = BigQueryMetrikkTask.TASKTYPE, cronExpression = "0 1 * * * *", maxFailedRuns = 20, firstDelay = 60)
public class BigQueryMetrikkTask implements ProsessTaskHandler {

    private static final int LOG_THRESHOLD = 5000;

    static final String TASKTYPE = "bigquery.metrikk.task";

    private static final Logger log = LoggerFactory.getLogger(BigQueryMetrikkTask.class);
    private boolean bigQueryEnabled;

    private BigQueryKlient bigQueryKlient;

    private BigQueryStatistikkRepository statistikkRepository;

    private ProsessTaskTjeneste prosessTaskTjeneste;

    BigQueryMetrikkTask() {
        // for proxyd
    }

    @Inject
    public BigQueryMetrikkTask(@KonfigVerdi(value = "BIGQUERY_ENABLED", required = false, defaultVerdi = "false") boolean bigQueryEnabled, BigQueryKlient bigQueryKlient, BigQueryStatistikkRepository statistikkRepository) {
        this.bigQueryKlient = bigQueryKlient;
        this.statistikkRepository = statistikkRepository;
        this.bigQueryEnabled = bigQueryEnabled;
    }

    @Override
    public void doTask(ProsessTaskData data) {
        long startTime = System.nanoTime();

        var sistKjørtTidspunkt = prosessTaskTjeneste.finnAlle(BigQueryMetrikkTask.TASKTYPE, ProsessTaskStatus.FERDIG).stream()
            .map(ProsessTaskData::getSistKjørt)
            .max(Comparator.naturalOrder()).orElse(TIDENES_BEGYNNELSE.atStartOfDay());
        try {
            List<Tuple<BigQueryTabell<?>, Collection<?>>> metrikker = statistikkRepository.hentHyppigRapporterte(sistKjørtTidspunkt);

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
