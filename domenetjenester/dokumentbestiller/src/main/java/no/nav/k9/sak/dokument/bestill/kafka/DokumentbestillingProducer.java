package no.nav.k9.sak.dokument.bestill.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.kafka.GenerellKafkaProducer;
import no.nav.k9.felles.integrasjon.kafka.KafkaPropertiesBuilder;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;

@ApplicationScoped
public class DokumentbestillingProducer {

    GenerellKafkaProducer producer;
    String topic;

    public DokumentbestillingProducer() {
        // for CDI proxy
    }

    @Inject
    public DokumentbestillingProducer(
        @KonfigVerdi("kafka.dokumentbestilling.topic") String topicName,
        @KonfigVerdi(value = "kafka.dokumentbestilling.aiven.topic") String topicV2Name,
        @KonfigVerdi(value = "KAFKA_BROKERS") String bootstrapServersAiven,
        @KonfigVerdi("bootstrap.servers") String bootstrapServersOnPrem,
        @KonfigVerdi(value = "KAFKA_TRUSTSTORE_PATH", required = false) String trustStorePath,
        @KonfigVerdi(value = "KAFKA_CREDSTORE_PASSWORD", required = false) String trustStorePassword,
        @KonfigVerdi(value = "KAFKA_KEYSTORE_PATH", required = false) String keyStoreLocation,
        @KonfigVerdi(value = "KAFKA_CREDSTORE_PASSWORD", required = false) String keyStorePassword,
        @KonfigVerdi(value = "KAFKA_DOKUMENTBESTILLING_AIVEN_ENABLED", defaultVerdi = "false") boolean aivenEnabled,
        @KonfigVerdi("systembruker.username") String username,
        @KonfigVerdi("systembruker.password") String password
    ) {

        String _topicName = aivenEnabled ? topicV2Name : topicName;
        String _bootstrapServer = aivenEnabled ? bootstrapServersAiven : bootstrapServersOnPrem;

        var builder = new KafkaPropertiesBuilder()
            .clientId("KP-" + topicName).bootstrapServers(_bootstrapServer);

        var props = aivenEnabled ?
            builder
                .truststorePath(trustStorePath)
                .truststorePassword(trustStorePassword)
                .keystorePath(keyStoreLocation)
                .keystorePassword(keyStorePassword)
                .buildForProducerAiven() :
            builder
                .username(username)
                .password(password)
                .buildForProducerJaas();

        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        this.producer = new GenerellKafkaProducer(_topicName, props);

    }

    public void flush() {
        producer.flush();
    }

    public void publiserDokumentbestillingJson(String json) {
        producer.runProducerWithSingleJson(new ProducerRecord<>(producer.getTopic(), json));
    }
}
