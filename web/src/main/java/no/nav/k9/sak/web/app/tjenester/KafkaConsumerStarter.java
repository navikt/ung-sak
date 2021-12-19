package no.nav.k9.sak.web.app.tjenester;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import no.nav.k9.sak.hendelse.vedtak.VedtakConsumer;

/**
 * Triggers start of Kafka consumere
 */
@WebListener
public class KafkaConsumerStarter implements ServletContextListener {

    @Inject
    private VedtakConsumer vedtakConsumer;

    public KafkaConsumerStarter() { // NOSONAR
        // For CDI
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        vedtakConsumer.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        vedtakConsumer.stop();
    }
}
