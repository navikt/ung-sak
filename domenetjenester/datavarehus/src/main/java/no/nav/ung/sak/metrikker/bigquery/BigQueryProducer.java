package no.nav.ung.sak.metrikker.bigquery;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CDI producer for BigQuery instanse.
 * Denne klassen produserer en BigQuery instanse for å kunne injectes i andre beans.
 */
@ApplicationScoped
public class BigQueryProducer {

    private static final Logger LOG = LoggerFactory.getLogger(BigQueryProducer.class);

    private final boolean bigQueryEnabled;

    @Inject
    public BigQueryProducer(@KonfigVerdi(value = "BIGQUERY_ENABLED", required = false, defaultVerdi = "false") boolean bigQueryEnabled) {
        this.bigQueryEnabled = bigQueryEnabled;
    }

    /**
     * Produserer en BigQuery instanse for injection.
     * Hvis BigQuery er disablet, returner null.
     *
     * @return BigQuery instansen eller null hvis disablet.
     */
    @Produces
    @Default
    public BigQuery produceBigQuery() {
        if (!bigQueryEnabled) {
            LOG.info("BigQuery er ikke aktivert, returnerer null");
            return null;
        }

        LOG.info("Oppretter BigQuery instanse");
        return BigQueryOptions.getDefaultInstance().getService();
    }
}
