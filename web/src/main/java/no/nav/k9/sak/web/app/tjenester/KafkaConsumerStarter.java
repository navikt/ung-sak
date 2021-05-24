package no.nav.k9.sak.web.app.tjenester;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import no.nav.k9.sak.domene.risikoklassifisering.konsument.RisikoklassifiseringConsumer;
import no.nav.k9.sak.hendelse.vedtak.VedtakConsumer;
import no.nav.k9.sak.hendelsemottak.k9fordel.kafka.PdlLeesahHendelseConsumer;
import no.nav.k9.sak.historikk.kafka.HistorikkConsumer;

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

    @Inject
    private PdlLeesahHendelseConsumer pdlLeesahHendelseConsumer;

    public KafkaConsumerStarter() { // NOSONAR
        // For CDI
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // FIXME K9 : avgjør om vi beholder denne for å skape lineage av historikk på tvers av tjenester
        // historikkConsumer.start();
        // risikoklassifiseringConsumer.start();
        vedtakConsumer.start();
        pdlLeesahHendelseConsumer.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // historikkConsumer.stop();
        // risikoklassifiseringConsumer.stop();
        vedtakConsumer.stop();
    }
}
