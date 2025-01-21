package no.nav.ung.domenetjenester.personhendelser;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.ung.fordel.kafka.AivenKafkaSettings;
import no.nav.ung.fordel.kafka.KafkaIntegration;
import no.nav.ung.fordel.kafka.KafkaSettings;
import no.nav.ung.fordel.kafka.Topic;
import no.nav.ung.fordel.kafka.utils.KafkaUtils;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static no.nav.k9.felles.sikkerhet.abac.PepImpl.ENV;

@ApplicationScoped
public class PdlLeesahHendelseStream implements KafkaIntegration {

    private static final Logger log = LoggerFactory.getLogger(PdlLeesahHendelseStream.class);

    private KafkaStreams stream;
    private Topic<String, Personhendelse> topic;
    private final boolean isDeployment = ENV.isProd() || ENV.isDev();

    PdlLeesahHendelseStream() {
    }

    @Inject
    public PdlLeesahHendelseStream(PdlLeesahHendelseHåndterer hendelseHåndterer,
                                   PdlLeesahHendelseFiltrerer hendelseFiltrerer,
                                   AivenKafkaSettings kafkaSettings,
                                   @KonfigVerdi(value = "hendelse.person.leesah.topic") String topicName,
                                   @KonfigVerdi(value = "kafka.avro.serde.class", required = false) String kafkaAvroSerdeClass) {
        this.topic = KafkaUtils.configureAvroTopic(topicName, kafkaSettings, Serdes.String(), KafkaUtils.getValueSerde(isDeployment));
        this.stream = createKafkaStreams(topic, hendelseFiltrerer, hendelseHåndterer, kafkaSettings);

        log.info("isDeployment={}, kafkaAvroSerdeClass={}", isDeployment, kafkaAvroSerdeClass);
    }

    @SuppressWarnings("resource")
    private static KafkaStreams createKafkaStreams(
        Topic<String, Personhendelse> topic,
        PdlLeesahHendelseFiltrerer hendelseFiltrerer,
        PdlLeesahHendelseHåndterer hendelseHåndterer,
        KafkaSettings kafkaSettings) {

        Serde<String> serdeKey = topic.getSerdeKey();
        Serde<Personhendelse> serdeValue = topic.getSerdeValue();
        final Consumed<String, Personhendelse> consumed = Consumed
            .<String, Personhendelse>with(Topology.AutoOffsetReset.EARLIEST)
            .withKeySerde(serdeKey)
            .withValueSerde(serdeValue);

        final StreamsBuilder builder = new StreamsBuilder();
        builder.stream(topic.getTopic(), consumed)
            .peek(PdlLeesahHendelseStream::loggHendelse)
            .filter(hendelseFiltrerer::erHendelseTypeViSkalHåndtere)
            .filter(hendelseFiltrerer::harPåvirketUngFagsakForHendelse)
            .foreach(hendelseHåndterer::håndterHendelse);


        final Topology topology = builder.build();

        var kafkaProperties = kafkaSettings.toStreamPropertiesWith(topic.getConsumerClientId(), serdeKey, serdeValue);
        return new KafkaStreams(topology, kafkaProperties);
    }

    private static void loggHendelse(String key, Personhendelse personhendelse) {
        log.info("Mottok en hendelse fra PDL: hendelseId={} opplysningstype={} endringstype={} master={} opprettet={} tidligereHendelseId={}",
            personhendelse.getHendelseId(), personhendelse.getOpplysningstype(), personhendelse.getEndringstype(),
            personhendelse.getMaster(), personhendelse.getOpprettet(), personhendelse.getTidligereHendelseId());
    }

    private void addShutdownHooks() {
        stream.setStateListener((newState, oldState) -> {
            log.info("{} :: From state={} to state={}", getTopicName(), oldState, newState);

            if (newState == KafkaStreams.State.ERROR) {
                // if the stream has died there is no reason to keep spinning
                log.warn("{} :: No reason to keep living, closing stream", getTopicName());
                stop();
            }
        });
        stream.setUncaughtExceptionHandler((t, e) -> {
            log.error(getTopicName() + " :: Caught exception in stream, exiting", e);
            stop();
        });
    }

    @Override
    public void start() {
        addShutdownHooks();

        stream.start();
        log.info("Starter konsumering av topic={}, tilstand={}", getTopicName(), stream.state());
    }

    public KafkaStreams.State getTilstand() {
        return stream.state();
    }

    @Override
    public boolean isAlive() {
        if (stream == null) {
            return false;
        }
        return stream.state().isRunningOrRebalancing();
    }

    public String getTopicName() {
        return topic.getTopic();
    }

    @Override
    public void stop() {
        log.info("Starter shutdown av topic={}, tilstand={} med 10 sekunder timeout", getTopicName(), stream.state());
        stream.close(Duration.of(60, ChronoUnit.SECONDS));
        log.info("Shutdown av topic={}, tilstand={} med 10 sekunder timeout", getTopicName(), stream.state());
    }
}
