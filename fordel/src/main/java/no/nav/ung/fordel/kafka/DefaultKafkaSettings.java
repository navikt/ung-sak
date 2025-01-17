package no.nav.ung.fordel.kafka;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.fordel.auth.SystembrukerClientCredentials;

@Dependent
public class DefaultKafkaSettings implements KafkaSettings {

    private final String bootstrapServers;
    private final String schemaRegistryUrl;
    private final String username;
    private final String password;
    private final String applicationId;
    private final String trustStorePath;
    private final String trustStorePassword;
    private final boolean readAllRecords; // TODO: Benyttes av verdikjeden
    private final List<String> deactivatedTopics;

    @Inject
    DefaultKafkaSettings(@KonfigVerdi("kafka.bootstrap.servers") String bootstrapServers,
                         @KonfigVerdi(value = "kafka.schema.registry.url", required = false) String schemaRegistryUrl,
                         @KonfigVerdi(value = "javax.net.ssl.trustStore") String trustStorePath,
                         @KonfigVerdi(value = "javax.net.ssl.trustStorePassword") String trustStorePassword,
                         @KonfigVerdi(value = "kafka.read.all.records", defaultVerdi = "false") boolean readAllRecords,
                         @KonfigVerdi(value = "KAFKA_DEACTIVATED_TOPICS", defaultVerdi = "", required = false) String deactivatedTopics,
                         SystembrukerClientCredentials clientCredentials) {
        this.trustStorePath = trustStorePath;
        this.trustStorePassword = trustStorePassword;
        this.readAllRecords = readAllRecords;
        this.applicationId = ApplicationIdUtil.get();
        this.bootstrapServers = bootstrapServers;
        this.schemaRegistryUrl = schemaRegistryUrl;
        this.username = clientCredentials.getClientId();
        this.password = clientCredentials.getClientSecret();
        this.deactivatedTopics = List.of(Optional.ofNullable(deactivatedTopics).orElse("").split(", *"));
    }

    private String getBootstrapServers() {
        return bootstrapServers;
    }

    @Override
    public String getSchemaRegistryUrl() {
        return schemaRegistryUrl;
    }

    @Override
    public Map<String, String> schemaRegistryProps() {
        var props = new HashMap<String, String>();
        if (schemaRegistryUrl != null) {
            props.put("schema.registry.url", schemaRegistryUrl);
        }
        return props;
    }

    private String getUsername() {
        return username;
    }

    private String getPassword() {
        return password;
    }

    private boolean harSattBrukernavn() {
        return username != null && !username.isEmpty();
    }

    private String getApplicationId() {
        return applicationId;
    }

    private boolean harSattTrustStore() {
        return trustStorePath != null && !trustStorePath.isEmpty()
                && trustStorePassword != null && !trustStorePassword.isEmpty();
    }

    private String getTrustStorePath() {
        return trustStorePath;
    }

    private String getTrustStorePassword() {
        return trustStorePassword;
    }

    @Override
    public List<String> getDeactivatedTopics() {
        return deactivatedTopics;
    }

    @Override
    public Properties toStreamPropertiesWith(String clientId, Serde<?> keySerde, Serde<?> valueSerde) {
        final Properties props = baseProperties();

        /*
         * Application ID må være unik per strøm for å unngå en feilsituasjon der
         * man enkelte ganger får feil partition (dvs partitions fra annen topic
         * enn den man skal ha).
         */
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, getApplicationId() + "-" + clientId);
        props.put(StreamsConfig.CLIENT_ID_CONFIG, clientId);
        if (readAllRecords) {
            props.put("auto.offset.reset", "earliest");
        } else {
            props.put("auto.offset.reset", "none");
        }

        // Serde
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, keySerde.getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, valueSerde.getClass());
        props.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndFailExceptionHandler.class);

        return props;
    }

    @Override
    public Properties toProducerPropertiesWith(String navn) {
        var producerClientId = String.format("producer-%s-%s", navn, clientId());
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


    private Properties baseProperties() {
        final Properties props = new Properties();

        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers());

        // Sikkerhet
        if (harSattBrukernavn()) {
            props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
            String jaasTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";";
            props.put(SaslConfigs.SASL_JAAS_CONFIG, String.format(jaasTemplate, getUsername(), getPassword()));
        }

        // Setup truststore? Skal det settes opp?
        if (harSattTrustStore()) {
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
            props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, getTrustStorePath());
            props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, getTrustStorePassword());
        }

        // Setup schema-registry
        if (getSchemaRegistryUrl() != null) {
            props.put("schema.registry.url", getSchemaRegistryUrl());
        }

        return props;
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
