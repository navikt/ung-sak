package no.nav.k9.sak.web.app.tjenester;

import no.nav.foreldrepenger.historikk.kafka.HistorikkConsumer;
import no.nav.k9.sak.domene.risikoklassifisering.konsument.RisikoklassifiseringConsumer;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Triggers start of Kafka consum
 */
@WebListener
public class KafkaConsumerStarter implements ServletContextListener {

    @Inject //NOSONAR
    private HistorikkConsumer historikkConsumer; //NOSONAR
    @Inject //NOSONAR
    private RisikoklassifiseringConsumer risikoklassifiseringConsumer; //NOSONAR

    public KafkaConsumerStarter() { //NOSONAR
        // For CDI
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        historikkConsumer.start();
        risikoklassifiseringConsumer.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        historikkConsumer.stop();
        risikoklassifiseringConsumer.stop();
    }
}
