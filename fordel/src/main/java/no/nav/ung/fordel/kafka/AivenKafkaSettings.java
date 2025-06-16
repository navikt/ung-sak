package no.nav.ung.fordel.kafka;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;

import static no.nav.k9.felles.sikkerhet.abac.PepImpl.ENV;

@Dependent
public class AivenKafkaSettings implements KafkaSettings {

    private final String bootstrapServers;
    private final String trustStorePath;
    private final String credstorePassword;
    private final String keyStorePath;
    private final String vtpOverride; // TODO: Benyttes av verdikjeden
    private final String schemaRegistryUrl;
    private final String schemaRegistryUser;
    private final String schemaRegistryPass;
    private final boolean isDeployment = ENV.isProd() || ENV.isDev();

    @Inject
    AivenKafkaSettings(@KonfigVerdi(value = "KAFKA_BROKERS") String bootstrapServers,
                       @KonfigVerdi(value = "KAFKA_SCHEMA_REGISTRY", required = false) String schemaRegistryUrl,
                       @KonfigVerdi(value = "KAFKA_SCHEMA_REGISTRY_USER", required = false) String schemaRegistryUser,
                       @KonfigVerdi(value = "KAFKA_SCHEMA_REGISTRY_PASSWORD", required = false) String schemaRegistryPass,
                       @KonfigVerdi(value = "KAFKA_TRUSTSTORE_PATH") String trustStorePath,
                       @KonfigVerdi(value = "KAFKA_KEYSTORE_PATH") String keystorePath,
                       @KonfigVerdi(value = "KAFKA_CREDSTORE_PASSWORD") String credstorePassword,
                       @KonfigVerdi(value = "KAFKA_OVERRIDE_KEYSTORE_PASSWORD", required = false) String vtpOverride) {
        this.bootstrapServers = bootstrapServers;
        this.schemaRegistryUrl = schemaRegistryUrl;
        this.schemaRegistryUser = schemaRegistryUser;
        this.schemaRegistryPass = schemaRegistryPass;
        this.trustStorePath = trustStorePath;
        this.keyStorePath = keystorePath;
        this.credstorePassword = credstorePassword;
        this.vtpOverride = vtpOverride;
    }

    private String getBootstrapServers() {
        return bootstrapServers;
    }

    @Override
    public Properties toStreamPropertiesWith(String streamNavn, Serde<?> keySerde, Serde<?> valueSerde) {
        Properties props = baseProperties();

        var streamId = String.format("stream-%s-ung-sak", streamNavn);
        var clientId = String.format("%s-%s", streamId, clientId());

        props.put(StreamsConfig.APPLICATION_ID_CONFIG, streamId);
        props.put(StreamsConfig.CLIENT_ID_CONFIG, clientId);

        // Serde
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, keySerde.getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, valueSerde.getClass());

        props.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndFailExceptionHandler.class);

        // Polling
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "200");
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "60000");

        return props;
    }

    @Override
    public Properties toProducerPropertiesWith(String navn) {
        var producerClientId= String.format("producer-%s-%s", navn, clientId());
        final Properties props = baseProperties();
        props.put(ProducerConfig.CLIENT_ID_CONFIG, producerClientId);
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.LINGER_MS_CONFIG, "0");
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1");
        return props;
    }

    @Override
    public Properties toStreamPropertiesWith(String navn) {
        final Properties props = baseProperties();
        var streamId = String.format("stream-%s-ung-sak", navn);
        var clientId = String.format("%s-%s", streamId, clientId());
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, streamId);
        props.put(StreamsConfig.CLIENT_ID_CONFIG, clientId);
        return props;
    }

    @Override
    public String getSchemaRegistryUrl() {
        return schemaRegistryUrl;
    }

    @Override
    @Deprecated(forRemoval = true)
    public List<String> getDeactivatedTopics() {
        return null;
    }

    private Properties baseProperties() {
        final Properties props = new Properties();

        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers());

        // Sikkerhet - miljø eller lokal
        if (isDeployment) {
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name);
            props.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");
            props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "jks");
            props.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12");
        } else {
            props.setProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SASL_SSL.name);
            props.setProperty(SaslConfigs.SASL_MECHANISM, "PLAIN");
            String jaasTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";";
            String jaasCfg = String.format(jaasTemplate, "vtp", "vtp");
            props.setProperty(SaslConfigs.SASL_JAAS_CONFIG, jaasCfg);
        }
        if (keyStorePath != null && trustStorePath != null) {
            // Lokalt vil disse allerede være satt i JettyDevServer, for VTP, dev og prod settes de her.
            props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keyStorePath);
            props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, vtpOverride != null ? vtpOverride : credstorePassword);
            props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, trustStorePath);
            props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, credstorePassword);
        }

        return props;
    }

    @Override
    public Map<String, String> schemaRegistryProps() {
        var props = new HashMap<String, String>();
        if (schemaRegistryUrl != null) {
            props.put("schema.registry.url", schemaRegistryUrl);
            props.put("basic.auth.credentials.source", "USER_INFO");
            props.put("basic.auth.user.info", schemaRegistryUser + ":" + schemaRegistryPass);
        }
        return props;
    }

    public String getSchemaRegistryUser() {
        return schemaRegistryUser;
    }

    public String getSchemaRegistryPass() {
        return schemaRegistryPass;
    }

    private String clientId() {
        if (System.getenv().containsKey("NAIS_APP_NAME")) {
            try {
                return InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                throw new IllegalStateException("Feil ved henting av hostname", e);
            }
        } else {
            return UUID.randomUUID().toString();
        }
    }
}
