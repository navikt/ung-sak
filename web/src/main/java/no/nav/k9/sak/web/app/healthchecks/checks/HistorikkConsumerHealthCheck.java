package no.nav.k9.sak.web.app.healthchecks.checks;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.streams.KafkaStreams;

import no.nav.k9.sak.historikk.kafka.HistorikkConsumer;

@ApplicationScoped
public class HistorikkConsumerHealthCheck extends ExtHealthCheck {

    private HistorikkConsumer consumer;

    HistorikkConsumerHealthCheck() {
    }
    
    @Override
    public boolean isSkipped() {
        // FIXME K9 avgjør om vi skal beholde denne kafka topicen for å skape en lineage av historikk eventer på tvers av tjenester for visning i gui
        return true;
    }

    @Inject
    public HistorikkConsumerHealthCheck(HistorikkConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    protected String getDescription() {
        return "Test av consumering av historikk fra kafka";
    }

    @Override
    protected String getEndpoint() {
        return consumer.getTopic();
    }

    @Override
    protected InternalResult performCheck() {
        InternalResult intTestRes = new InternalResult();

        KafkaStreams.State tilstand = consumer.getTilstand();
        intTestRes.setMessage("Consumer is in state [" + tilstand.name() + "].");
        intTestRes.setOk(holderPåÅKonsumere(tilstand));
        intTestRes.noteResponseTime();

        return intTestRes;
    }

    private boolean holderPåÅKonsumere(KafkaStreams.State tilstand) {
        return tilstand.isRunning() || KafkaStreams.State.CREATED.equals(tilstand);
    }
}
