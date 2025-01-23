package no.nav.ung.sak.web.app.selftest.checks;

import org.apache.kafka.streams.KafkaStreams;

import jakarta.inject.Inject;
import no.nav.ung.domenetjenester.arkiv.JournalHendelseStream;

//@ApplicationScoped
public class JournalføringHendelseStreamHealthCheck extends ExtHealthCheck {

    private JournalHendelseStream consumer;

    JournalføringHendelseStreamHealthCheck() {
    }

    @Inject
    public JournalføringHendelseStreamHealthCheck(JournalHendelseStream consumer) {
        this.consumer = consumer;
    }

    @Override
    protected String getDescription() {
        return "Consumer journalførings hendelser fra joark.";
    }

    @Override
    protected String getEndpoint() {
        return consumer.getTopicName();
    }

    @Override
    protected InternalResult performCheck() {
        InternalResult intTestRes = new InternalResult();

        KafkaStreams.State tilstand = consumer.getTilstand();
        intTestRes.setMessage("Consumer is in state [" + tilstand.name() + "].");
        if (tilstand.isRunningOrRebalancing() || KafkaStreams.State.CREATED.equals(tilstand)) {
            intTestRes.setOk(true);
        } else {
            intTestRes.setOk(false);
        }
        intTestRes.noteResponseTime();

        return intTestRes;
    }
}
