package no.nav.ung.sak.metrikker.bigquery;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.util.Tuple;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.sak.metrikker.bigquery.tabeller.BigQueryTabell;
import no.nav.ung.sak.metrikker.bigquery.tabeller.uttalelse.UttalelseRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;

/**
 * Task for publisering av historiske metrikker til BigQuery.
 * Denne tasken henter rapporterte metrikker fra BigQueryStatistikkRepository og publiserer dem til BigQuery.
 */
@ApplicationScoped
@ProsessTask(value = PopulerMetrikkHistoriskeDataTask.TASKTYPE)
public class PopulerMetrikkHistoriskeDataTask implements ProsessTaskHandler {

    static final String TASKTYPE = "bigquery.historiske.data.task";

    private static final Logger log = LoggerFactory.getLogger(PopulerMetrikkHistoriskeDataTask.class);
    private boolean bigQueryEnabled;

    private BigQueryKlient bigQueryKlient;

    private BigQueryStatistikkRepository statistikkRepository;

    PopulerMetrikkHistoriskeDataTask() {
        // for proxyd
    }

    @Inject
    public PopulerMetrikkHistoriskeDataTask(@KonfigVerdi(value = "BIGQUERY_ENABLED", required = false, defaultVerdi = "false") boolean bigQueryEnabled, BigQueryKlient bigQueryKlient, BigQueryStatistikkRepository statistikkRepository) {
        this.bigQueryKlient = bigQueryKlient;
        this.statistikkRepository = statistikkRepository;
        this.bigQueryEnabled = bigQueryEnabled;
    }

    @Override
    public void doTask(ProsessTaskData data) {

        String metrikkVerdi = data.getPropertyValue("METRIKK_IDENTIFIKATOR");
        LocalDateTime førsteKjøreTidspunkt = LocalDateTime.parse(data.getPropertyValue("FORSTE_KJORE_TIDSPUNKT"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        if (!bigQueryEnabled) {
            switch (metrikkVerdi) {
                case "UTTALELSE_METRIKK":
                    var uttalelseData = statistikkRepository.uttalelseData(TIDENES_ENDE.atStartOfDay(), førsteKjøreTidspunkt);
                    publiserMetrikker(BigQueryDataset.UNG_SAK_STATISTIKK_DATASET, List.of(new Tuple<>(UttalelseRecord.UTTALELSE_TABELL, uttalelseData)));
                    log.info("Publisert {} metrikker til BigQuery", uttalelseData.size());
                    break;
                default:
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
