package no.nav.ung.sak.web.app.selftest.checks;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.domenetjenester.personhendelser.PdlLeesahHendelseStream;
import org.apache.kafka.streams.KafkaStreams;

@ApplicationScoped
public class PDLHendelseStreamHealthCheck extends ExtHealthCheck {

    private PdlLeesahHendelseStream consumer;

    PDLHendelseStreamHealthCheck() {
    }

    @Inject
    public PDLHendelseStreamHealthCheck(PdlLeesahHendelseStream consumer) {
        this.consumer = consumer;
    }

    @Override
    protected String getDescription() {
        return "Consumer leesah hendelser fra PDL.";
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
