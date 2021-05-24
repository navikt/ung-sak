package no.nav.k9.sak.hendelsemottak.k9fordel.kafka;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.person.pdl.leesah.Personhendelse;

@Dependent
class PdlLeesahHendelseProperties {

    private static final Logger LOG = LoggerFactory.getLogger(PdlLeesahHendelseProperties.class);

    private static final String KAFKA_AVRO_SERDE_CLASS = "kafka.avro.serde.class";

    private final String bootstrapServers;
    private final String schemaRegistryUrl;
    private final Topic<String, Personhendelse> topic;
    private final String username;
    private final String password;
    private final String applicationId;
    private final String trustStorePath;
    private final String trustStorePassword;

    @Inject
    public PdlLeesahHendelseProperties(@KonfigVerdi("bootstrap.servers") String bootstrapServers,
                                       @KonfigVerdi("kafka.schema.registry.url") String schemaRegistry,
                                       @KonfigVerdi("kafka.pdl.leesah.topic") String topicName,
                                       @KonfigVerdi("systembruker.username") String username,
                                       @KonfigVerdi("systembruker.password") String password,
                                       @KonfigVerdi(value = "javax.net.ssl.trustStore", required = false) String trustStorePath,
                                       @KonfigVerdi(value = "javax.net.ssl.trustStorePassword", required = false) String trustStorePassword,
                                       @KonfigVerdi("kafka.pdl.leesah.application.id") String applicationId, // TODO: Sette opp i vtp, fpabonnent-default-KC-aapen-person-pdl-leesah-v1-vtp
                                       @KonfigVerdi(value = KAFKA_AVRO_SERDE_CLASS, defaultVerdi = "io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde") String kafkaAvroSerdeClass) {
        this.bootstrapServers = bootstrapServers;
        this.schemaRegistryUrl = schemaRegistry;
        this.username = username;
        this.password = password;
        this.trustStorePath = trustStorePath;
        this.trustStorePassword = trustStorePassword;
        this.applicationId = applicationId;
        this.topic = new Topic<>(topicName, Serdes.String(), getSerde(kafkaAvroSerdeClass));
    }

    public Topic<String, Personhendelse> getTopic() {
        return topic;
    }

    String getClientId() {
        return topic.getConsumerClientId();
    }

    public String getSchemaRegistryUrl() {
        return schemaRegistryUrl;
    }

    String getBootstrapServers() {
        return bootstrapServers;
    }

    String getUsername() {
        return username;
    }

    String getPassword() {
        return password;
    }

    boolean harSattBrukernavn() {
        return username != null && !username.isEmpty();
    }

    String getApplicationId() {
        return applicationId;
    }

    String getTrustStorePath() {
        return trustStorePath;
    }

    String getTrustStorePassword() {
        return trustStorePassword;
    }

    boolean harSattTrustStore() {
        return trustStorePath != null && !trustStorePath.isEmpty()
            && trustStorePassword != null && !trustStorePassword.isEmpty();
    }

    private Serde getSerde(@KonfigVerdi(KAFKA_AVRO_SERDE_CLASS) String kafkaAvroSerdeClass) {
        Serde serde = new SpecificAvroSerde<>();
        if (kafkaAvroSerdeClass != null && !kafkaAvroSerdeClass.isBlank()) {
            try {
                serde = (Serde)Class.forName(kafkaAvroSerdeClass).getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                LOG.warn(String.format("Utvikler-feil: Konfigurasjonsverdien '%s' peker på klasse '%s' som ikke kunne brukes. Benytter default.", KAFKA_AVRO_SERDE_CLASS, kafkaAvroSerdeClass), e);
            }
        }
        return serde;
    }

    Class<?> getKeyClass() {
        return topic.getSerdeKey().getClass();
    }


    Class<?> getValueClass() {
        return topic.getSerdeValue().getClass();
    }

}
