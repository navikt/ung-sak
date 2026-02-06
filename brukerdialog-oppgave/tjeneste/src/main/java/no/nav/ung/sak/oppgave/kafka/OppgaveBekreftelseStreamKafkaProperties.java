package no.nav.ung.sak.oppgave.kafka;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.kafka.KafkaPropertiesBuilder;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

@Dependent
public class OppgaveBekreftelseStreamKafkaProperties {

    private static final Logger log = LoggerFactory.getLogger(OppgaveBekreftelseStreamKafkaProperties.class);
    private final String bootstrapServers;
    private final String topic;
    private final String trustStorePath;
    private final String trustStorePassword;
    private final String keyStoreLocation;
    private final String keyStorePassword;

    @SuppressWarnings("resource")
    @Inject
    OppgaveBekreftelseStreamKafkaProperties(@KonfigVerdi(value = "KAFKA_BROKERS") String bootstrapServers,
                                            @KonfigVerdi(value = "KAFKA_OPPGAVEBEKREFTELSE_TOPIC") String topicName,
                                            @KonfigVerdi(value = "KAFKA_TRUSTSTORE_PATH", required = false) String trustStorePath,
                                            @KonfigVerdi(value = "KAFKA_CREDSTORE_PASSWORD", required = false) String trustStorePassword,
                                            @KonfigVerdi(value = "KAFKA_KEYSTORE_PATH", required = false) String keyStoreLocation,
                                            @KonfigVerdi(value = "KAFKA_CREDSTORE_PASSWORD", required = false) String keyStorePassword) {
        this.trustStorePath = trustStorePath;
        this.trustStorePassword = trustStorePassword;
        this.keyStoreLocation = keyStoreLocation;
        this.keyStorePassword = keyStorePassword;
        this.topic = topicName;
        this.bootstrapServers = bootstrapServers;
    }

    Properties setupProperties() {
        var builder = new KafkaPropertiesBuilder();

        Properties props = builder
            .clientId("ung-sak")
            .applicationId("ung-sak")
            .bootstrapServers(bootstrapServers)
            .truststorePath(trustStorePath)
            .truststorePassword(trustStorePassword)
            .keystorePath(keyStoreLocation)
            .keystorePassword(keyStorePassword)
            .buildForStreamsAiven();

        // Serde
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndFailExceptionHandler.class);

        // Polling
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "200");
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "100000");

        log.info("Configuring topic='{}' with applicationId='{} & SSL-auth enabled",
            topic, props.getProperty(StreamsConfig.APPLICATION_ID_CONFIG));

        return props;
    }

    String getTopic() {
        return topic;
    }
}
