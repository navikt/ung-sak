package no.nav.ung.domenetjenester.arkiv;

import java.time.Duration;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord;
import no.nav.ung.fordel.kafka.KafkaIntegration;
import no.nav.ung.fordel.kafka.Topic;
import no.nav.ung.fordel.kodeverdi.OmrådeTema;

/*
 * Dokumentasjon https://confluence.adeo.no/pages/viewpage.action?pageId=432217859
 */
@ApplicationScoped
public class JournalHendelseStream implements KafkaIntegration {

    private static final Logger LOG = LoggerFactory.getLogger(JournalHendelseStream.class);
    private static final String HENDELSE_MOTTATT = "JournalpostMottatt";
    private static final String HENDELSE_ENDRET = "TemaEndret";
    private static final String TEMA_OMS = OmrådeTema.OMS.getOffisiellKode();
    private static final String TEMA_UNG = OmrådeTema.UNG.getOffisiellKode();

    private KafkaStreams stream;
    private Topic<String, JournalfoeringHendelseRecord> topic;

    JournalHendelseStream() {
    }

    @Inject
    public JournalHendelseStream(JournalføringHendelseHåndterer journalføringHendelseHåndterer,
                                 JournalHendelseProperties journalHendelseProperties) {
        this.topic = journalHendelseProperties.getTopic();
        this.stream = createKafkaStreams(topic, journalføringHendelseHåndterer, journalHendelseProperties);
    }



    @SuppressWarnings("resource")
    private static KafkaStreams createKafkaStreams(Topic<String, JournalfoeringHendelseRecord> topic,
                                                   JournalføringHendelseHåndterer journalføringHendelseHåndterer,
                                                   JournalHendelseProperties properties) {

        final Consumed<String, JournalfoeringHendelseRecord> consumed = Consumed
                // Ved tap av offset spilles på nytt, 7 dager retention - anses som akseptabelt
            .<String, JournalfoeringHendelseRecord>with(Topology.AutoOffsetReset.EARLIEST)
            .withKeySerde(topic.getSerdeKey())
            .withValueSerde(topic.getSerdeValue());

        final StreamsBuilder builder = new StreamsBuilder();
        builder.stream(topic.getTopic(), consumed)
                // TODO: Bytt til tema UNG før lansering av ungdomsytelsen
            .filter((key, value) -> TEMA_OMS.equals(value.getTemaNytt()))
            .filter((key, value) -> hendelseSkalHåndteres(value))
            .foreach(journalføringHendelseHåndterer::handleMessage);

        return new KafkaStreams(builder.build(), properties.getProperties());
    }

    private static boolean hendelseSkalHåndteres(JournalfoeringHendelseRecord payload) {
        var hendelse = payload.getHendelsesType();
        return HENDELSE_MOTTATT.equalsIgnoreCase(hendelse) || HENDELSE_ENDRET.equalsIgnoreCase(hendelse);
    }

    private void addShutdownHooks() {
        stream.setStateListener((newState, oldState) -> {
            LOG.info("{} :: From state={} to state={}", getTopicName(), oldState, newState);

            if (newState == KafkaStreams.State.ERROR) {
                // if the stream has died there is no reason to keep spinning
                LOG.warn("{} :: No reason to keep living, closing stream", getTopicName());
                stop();
            }
        });
        stream.setUncaughtExceptionHandler((t, e) -> {
            LOG.error("{} :: Caught exception in stream, exiting", getTopicName(), e);
            stop();
        });
    }

    @Override
    public void start() {
        addShutdownHooks();
        stream.start();
        LOG.info("Starter konsumering av topic={}, tilstand={}", getTopicName(), stream.state());
    }

    public String getTopicName() {
        return topic.getTopic();
    }

    public KafkaStreams.State getTilstand() {
        return stream.state();
    }

    @Override
    public boolean isAlive() {
        return (stream != null) && stream.state().isRunningOrRebalancing();
    }

    @Override
    public void stop() {
        LOG.info("Starter shutdown av topic={}, tilstand={} med 15 sekunder timeout", getTopicName(), stream.state());
        stream.close(Duration.ofSeconds(15));
        LOG.info("Shutdown av topic={}, tilstand={} med 15 sekunder timeout", getTopicName(), stream.state());
    }
}
