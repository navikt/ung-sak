package no.nav.ung.sak.oppgave.kafka;

import no.nav.k9.felles.konfigurasjon.env.Environment;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Optional;
import java.util.Properties;
import java.util.UUID;


public class KafkaProperties {

    private static final Environment ENV = Environment.current();
    private static final String APPLICATION_NAME = ENV.getProperty("NAIS_APP_NAME", "ung-sak");

    private KafkaProperties() {
    }

    public static <K, V> Properties forConsumerGenericValue(String groupId,
                                                            Deserializer<K> keyDeserializer,
                                                            Deserializer<V> valueDeserializer,
                                                            OffsetResetStrategy offsetReset) {
        final Properties props = new Properties();

        props.put(CommonClientConfigs.GROUP_ID_CONFIG, groupId);
        props.put(CommonClientConfigs.CLIENT_ID_CONFIG, generateClientId());
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, getAivenConfig(AivenProperty.KAFKA_BROKERS));
        Optional.ofNullable(offsetReset).ifPresent(or -> props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, or.toString()));

        putSecurity(props);

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer.getClass());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer.getClass());

        // Polling
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100"); // Unngå store Tx dersom alle prosesseres innen samme Tx. Default 500
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "100000"); // Gir inntil 1s pr record. Default er 600 ms/record

        return props;
    }

    private static String getAivenConfig(AivenProperty property) {
        return Optional.ofNullable(ENV.getProperty(property.name()))
            .orElseGet(() -> ENV.getProperty(property.name().toLowerCase().replace('_', '.')));
    }

    private static String generateClientId() {
        return APPLICATION_NAME + "-" + UUID.randomUUID();
    }

    private static void putSecurity(Properties props) {
        var credStorePassword = getAivenConfig(AivenProperty.KAFKA_CREDSTORE_PASSWORD);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name);
        props.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");
        props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "jks");
        props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, getAivenConfig(AivenProperty.KAFKA_TRUSTSTORE_PATH));
        props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, credStorePassword);
        props.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12");
        props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, getAivenConfig(AivenProperty.KAFKA_KEYSTORE_PATH));
        props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, credStorePassword);
    }

}
