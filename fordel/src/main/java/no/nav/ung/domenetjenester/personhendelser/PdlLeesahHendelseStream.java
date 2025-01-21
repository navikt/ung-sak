package no.nav.ung.domenetjenester.personhendelser;

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.ung.domenetjenester.personhendelser.test.VtpKafkaAvroSerde;
import no.nav.ung.fordel.kafka.AivenKafkaSettings;
import no.nav.ung.fordel.kafka.KafkaIntegration;
import no.nav.ung.fordel.kafka.KafkaSettings;
import no.nav.ung.fordel.kafka.Topic;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
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
import java.util.HashMap;
import java.util.Properties;

import static no.nav.k9.felles.sikkerhet.abac.PepImpl.ENV;

@ApplicationScoped
public class PdlLeesahHendelseStream implements KafkaIntegration {

    private static final Logger log = LoggerFactory.getLogger(PdlLeesahHendelseStream.class);

    private KafkaStreams stream;
    private PdlLeesahHendelseFiltrerer hendelseFiltrerer;
    private String kafkaAvroSerdeClass;
    private Topic<String, Personhendelse> topic;
    private PdlLeesahHendelseHåndterer hendelseHåndterer;
    private final boolean isDeployment = ENV.isProd() || ENV.isDev();

    PdlLeesahHendelseStream() {
    }

    @Inject
    public PdlLeesahHendelseStream(PdlLeesahHendelseHåndterer hendelseHåndterer,
                                   PdlLeesahHendelseFiltrerer hendelseFiltrerer,
                                   AivenKafkaSettings kafkaSettings,
                                   @KonfigVerdi(value = "hendelse.person.leesah.topic") String topicName,
                                   @KonfigVerdi(value = "kafka.avro.serde.class", required = false) String kafkaAvroSerdeClass) {
        this.hendelseFiltrerer = hendelseFiltrerer;
        this.kafkaAvroSerdeClass = kafkaAvroSerdeClass;
        this.hendelseHåndterer = hendelseHåndterer;
        this.topic = new Topic<>(topicName, Serdes.String(), getSerdeValue(kafkaAvroSerdeClass));
        this.stream = createKafkaStreams(kafkaSettings);
    }

    @SuppressWarnings("resource")
    private KafkaStreams createKafkaStreams(KafkaSettings kafkaSettings) {
        final Serde<String> keySerde = configureSchemaRegistry(kafkaSettings, topic.getSerdeKey(), true);
        final Serde<Personhendelse> valueSerde = configureSchemaRegistry(kafkaSettings, topic.getSerdeValue(), false);

        final Consumed<String, Personhendelse> consumed = Consumed.with(keySerde, valueSerde);

        final StreamsBuilder builder = new StreamsBuilder();
        builder.stream(topic.getTopic(), consumed)
            .peek(this::loggHendelse)
            .filter(hendelseFiltrerer::erHendelseTypeViSkalHåndtere)
            .filter(hendelseFiltrerer::harPåvirketUngFagsakForHendelse)
            .foreach(hendelseHåndterer::håndterHendelse);


        final Topology topology = builder.build();

        var kafkaProperties = kafkaSettings.toStreamPropertiesWith(topic.getConsumerClientId(), keySerde, valueSerde);
        if (!isDeployment) {
            overrideMedVtpProps(kafkaProperties);

        }
        kafkaProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // TODO: Fjern denne, bare nødvendig ved førstegangsoppstart (default = NONE)
        return new KafkaStreams(topology, kafkaProperties);
    }

    private void overrideMedVtpProps(Properties props) {
        props.setProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SASL_SSL.name);
        props.setProperty(SaslConfigs.SASL_MECHANISM, "PLAIN");
        String jaasTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";";
        String jaasCfg = String.format(jaasTemplate, "vtp", "vtp");
        props.setProperty(SaslConfigs.SASL_JAAS_CONFIG, jaasCfg);

    }

    private Serde<Personhendelse> getSerdeValue(String kafkaAvroSerdeClass) {
        if (!isDeployment) {
            return new VtpKafkaAvroSerde<>();
        }
        if (kafkaAvroSerdeClass != null && !kafkaAvroSerdeClass.isBlank()) {
            try {
                return (Serde<Personhendelse>) Class.forName(kafkaAvroSerdeClass).getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                log.error("Feilet ved instansiereing av klasse={} for serialisering", kafkaAvroSerdeClass);
                throw new RuntimeException(e);
            }
        } else {
            return new SpecificAvroSerde<>();
        }
    }

    private void loggHendelse(String key, Personhendelse personhendelse) {
        log.info("Mottok en hendelse fra PDL: hendelseId={} opplysningstype={} endringstype={} master={} opprettet={} tidligereHendelseId={}",
            personhendelse.getHendelseId(), personhendelse.getOpplysningstype(), personhendelse.getEndringstype(),
            personhendelse.getMaster(), personhendelse.getOpprettet(), personhendelse.getTidligereHendelseId());
    }

    private <L> Serde<L> configureSchemaRegistry(KafkaSettings kafkaSettings, Serde<L> serde, boolean isKey) {
        if (isDeployment && kafkaSettings.getSchemaRegistryUrl() != null && !kafkaSettings.getSchemaRegistryUrl().isEmpty()) {
            boolean spesfikkAvroSerialiserer = kafkaAvroSerdeClass != null;

            var properties = new HashMap<String, Object>(kafkaSettings.schemaRegistryProps());
            properties.put("specific.avro.reader", spesfikkAvroSerialiserer);
            serde.configure(properties, isKey);
        }
        return serde;
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
