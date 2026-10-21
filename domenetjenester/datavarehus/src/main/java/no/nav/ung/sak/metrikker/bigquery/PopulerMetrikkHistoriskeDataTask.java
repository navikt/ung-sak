package no.nav.ung.sak.metrikker.bigquery;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.metrikker.bigquery.tabeller.YtelsesTabell;
import no.nav.ung.sak.metrikker.bigquery.tabeller.uttalelse.UttalelseRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;

/**
 * Task for publisering av historiske metrikker til BigQuery.
 * Denne tasken henter rapporterte metrikker fra BigQueryStatistikkRepository og publiserer dem til BigQuery.
 * Property {@value #YTELSE_TYPE} styrer hvilken ytelse det publiseres for (standard UNG).
 */
@ApplicationScoped
@ProsessTask(value = PopulerMetrikkHistoriskeDataTask.TASKTYPE)
public class PopulerMetrikkHistoriskeDataTask implements ProsessTaskHandler {

    static final String TASKTYPE = "bigquery.historiske.data.task";
    static final String YTELSE_TYPE = "YTELSE_TYPE";

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
        String ytelseKode = data.getPropertyValue(YTELSE_TYPE);
        FagsakYtelseType ytelse = YtelsesTabell.validerStøttet(ytelseKode == null || ytelseKode.isBlank()
            ? FagsakYtelseType.UNGDOMSYTELSE
            : FagsakYtelseType.fraKode(ytelseKode));
        if (bigQueryEnabled) {
            switch (metrikkVerdi) {
                case "UTTALELSE_METRIKK":
                    var uttalelseData = statistikkRepository.uttalelseData(ytelse, TIDENES_ENDE.atStartOfDay(), førsteKjøreTidspunkt);
                    bigQueryKlient.publish(BigQueryDataset.UNG_SAK_STATISTIKK_DATASET, UttalelseRecord.UTTALELSE_TABELLER.forYtelse(ytelse), uttalelseData);
                    log.info("Publisert {} metrikker for ytelse {} til BigQuery", uttalelseData.size(), ytelse.getKode());
                    break;
                default:
                    throw new IllegalArgumentException("Ukjent metrikk: " + metrikkVerdi);
            }
        }
    }

}
