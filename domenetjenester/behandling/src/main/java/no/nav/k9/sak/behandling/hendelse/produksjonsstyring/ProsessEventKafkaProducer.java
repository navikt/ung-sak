package no.nav.k9.sak.behandling.hendelse.produksjonsstyring;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.kafka.GenerellKafkaProducer;
import no.nav.k9.felles.integrasjon.kafka.KafkaPropertiesBuilder;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;

@ApplicationScoped
public class ProsessEventKafkaProducer {

    private static final Logger logger = LoggerFactory.getLogger(ProsessEventKafkaProducer.class);

    private GenerellKafkaProducer producer;
    String topic;

    public ProsessEventKafkaProducer() {
        // for CDI proxy
    }

    @Inject
    public ProsessEventKafkaProducer(@KonfigVerdi("kafka.aksjonspunkthendelse.topic") String topic,
                                     @KonfigVerdi("kafka.aksjonspunkthendelse.aiven.topic") String topicV2,
                                     @KonfigVerdi(value = "KAFKA_BROKERS") String bootstrapServersAiven,
                                     @KonfigVerdi("bootstrap.servers") String bootstrapServersOnPrem,
                                     @KonfigVerdi(value = "KAFKA_TRUSTSTORE_PATH", required = false) String trustStorePath,
                                     @KonfigVerdi(value = "KAFKA_CREDSTORE_PASSWORD", required = false) String trustStorePassword,
                                     @KonfigVerdi(value = "KAFKA_KEYSTORE_PATH", required = false) String keyStoreLocation,
                                     @KonfigVerdi(value = "KAFKA_CREDSTORE_PASSWORD", required = false) String keyStorePassword,
                                     @KonfigVerdi("systembruker.username") String username,
                                     @KonfigVerdi("systembruker.password") String password) {


        boolean aivenEnabled = !Environment.current().isLocal(); //har ikke støtte i vtp
        String _topicName = aivenEnabled ? topicV2 : topic;
        String _bootstrapServer = aivenEnabled ? bootstrapServersAiven : bootstrapServersOnPrem;

        var builder = new KafkaPropertiesBuilder()
            .clientId("KP-" + _topicName).bootstrapServers(_bootstrapServer);

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


        producer = new GenerellKafkaProducer(_topicName, props);

        this.topic = _topicName;

    }

    public void flush() {
        producer.flush();
    }

    public void sendHendelse(String nøkkel, String json) {
        ProducerRecord<String, String> melding = new ProducerRecord<>(topic, nøkkel, json);
        RecordMetadata recordMetadata = producer.runProducerWithSingleJson(melding);
        logger.info("Sendt hendelse til Aiven på {} partisjon {} offset {}", recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset());
    }

}
