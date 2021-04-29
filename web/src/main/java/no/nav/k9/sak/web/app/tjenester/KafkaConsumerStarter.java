package no.nav.k9.sak.web.app.tjenester;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import no.nav.k9.sak.domene.risikoklassifisering.konsument.RisikoklassifiseringConsumer;
import no.nav.k9.sak.historikk.kafka.HistorikkConsumer;
import no.nav.k9.sak.hendelse.vedtak.VedtakConsumer;

/**
 * Triggers start of Kafka consumere
 */
@WebListener
public class KafkaConsumerStarter implements ServletContextListener {

    @SuppressWarnings("unused")
    @Inject // NOSONAR
    private HistorikkConsumer historikkConsumer; // NOSONAR

    @SuppressWarnings("unused")
    @Inject // NOSONAR
    private RisikoklassifiseringConsumer risikoklassifiseringConsumer; // NOSONAR

    @Inject
    private VedtakConsumer vedtakConsumer;

    public KafkaConsumerStarter() { // NOSONAR
        // For CDI
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // FIXME K9 : avgjør om vi beholder denne for å skape lineage av historikk på tvers av tjenester
        // historikkConsumer.start();
        // risikoklassifiseringConsumer.start();
        vedtakConsumer.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // historikkConsumer.stop();
        // risikoklassifiseringConsumer.stop();
        vedtakConsumer.stop();
    }
}
