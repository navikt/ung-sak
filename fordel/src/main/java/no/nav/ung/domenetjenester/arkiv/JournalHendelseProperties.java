package no.nav.ung.domenetjenester.arkiv;


import static io.confluent.kafka.serializers.KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG;

import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.domenetjenester.personhendelser.test.VtpKafkaAvroSerde;
import no.nav.ung.fordel.kafka.Topic;

@Dependent
class JournalHendelseProperties {

    private static final Environment ENV = Environment.current();

    private final String clientId;
    private final String bootstrapServers;
    private final String applicationId;
    private final String trustStorePath;
    private final String keyStorePath;
    private final String credStorePassword;
    private final String vtpOverride;
    private final Topic<String, JournalfoeringHendelseRecord> topic;
    private final boolean isDeployment = ENV.isProd() || ENV.isDev();


    @Inject
    JournalHendelseProperties(@KonfigVerdi(value = "kafka.journal.topic") String topicName,
                              // Verdiene nedenfor injectes av aivenator (nais)
                              @KonfigVerdi(value = "kafka.brokers") String bootstrapServers,
                              @KonfigVerdi(value = "kafka.schema.registry", required = false) String schemaRegistryUrl,
                              @KonfigVerdi(value = "kafka.schema.registry.user", required = false) String schemaRegistryUsername,
                              @KonfigVerdi(value = "kafka.schema.registry.password", required = false) String schemaRegistryPassword,
                              @KonfigVerdi(value = "kafka.truststore.path", required = false) String trustStorePath,
                              @KonfigVerdi(value = "kafka.keystore.path", required = false) String keyStorePath,
                              @KonfigVerdi(value = "kafka.credstore.password", required = false) String credStorePassword,
                              @KonfigVerdi(value = "kafka.override.keystore.password", required = false) String vtpOverride) {
        this.trustStorePath = trustStorePath;
        this.keyStorePath = keyStorePath;
        this.applicationId = "ung-sak-" + topicName;
        this.clientId = "ung-sak-" + topicName;
        this.bootstrapServers = bootstrapServers;
        this.vtpOverride = vtpOverride;
        this.credStorePassword = credStorePassword;
        this.topic = createConfiguredTopic(topicName, schemaRegistryUrl, getBasicAuth(schemaRegistryUsername, schemaRegistryPassword));
    }

    public Topic<String, JournalfoeringHendelseRecord> getTopic() {
        return topic;
    }

    private Topic<String, JournalfoeringHendelseRecord> createConfiguredTopic(String topicName, String schemaRegistryUrl,
                                                                              String basicAuth) {
        var configuredTopic = new Topic<>(topicName, Serdes.String(), getSerde());
        if (schemaRegistryUrl != null && !schemaRegistryUrl.isEmpty()) {
            var schemaMap =
                    Map.of(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl,
                            AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO",
                            AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG, basicAuth,
                            SPECIFIC_AVRO_READER_CONFIG, true);
            configuredTopic.getSerdeKey().configure(schemaMap, true);
            configuredTopic.getSerdeValue().configure(schemaMap, false);
        }
        return configuredTopic;
    }

    private String getBasicAuth(String schemaRegistryUsername, String schemaRegistryPassword) {
        return schemaRegistryUsername + ":" + schemaRegistryPassword;
    }

    Properties getProperties() {
        Properties props = new Properties();

        props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
        props.put(StreamsConfig.CLIENT_ID_CONFIG, clientId);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

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
            props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, vtpOverride != null ? vtpOverride : credStorePassword);
            props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, trustStorePath);
            props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, credStorePassword);
        }

        // Serde
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, topic.getSerdeKey().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, topic.getSerdeValue().getClass());
        props.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndFailExceptionHandler.class);

        // Polling
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "200");
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "60000");

        return props;
    }

    private Serde<JournalfoeringHendelseRecord> getSerde() {
        return isDeployment ? new SpecificAvroSerde<>() : new VtpKafkaAvroSerde<>();
    }

}
