package no.nav.k9.sak.ytelse.omsorgspenger.rapid;

import no.nav.vedtak.konfig.KonfigVerdi;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class BehovKafkaProducer extends BehovKlient {
    private static final String BEHOVESSEKVENS_ID = "behovessekvens_id";
    private static final Logger LOG = LoggerFactory.getLogger(BehovKafkaProducer.class);

    private String topic;
    private Producer<String, String> producer;

    private BehovKafkaProducer() {}

    @Inject
    public BehovKafkaProducer(
        @KonfigVerdi(value = "kafka.k9-rapid.topic", defaultVerdi = "k9-rapid-v2", required = false) String topic,
        @KonfigVerdi("bootstrap.servers") String bootstrapServers,
        @KonfigVerdi("systembruker.username") String username,
        @KonfigVerdi("systembruker.password") String password) {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", bootstrapServers);
        properties.put("client.id", "TODO"); // TODO
        setSecurity(username, properties);
        setUsernameAndPassword(username, password, properties);
        this.topic = topic;
        this.producer = createProducer(properties);
    }

    @Override
    public void send(String behovssekvensId, String behovssekvens) {
        try {
            MDC.put(BEHOVESSEKVENS_ID, behovssekvensId);
            var metadata = producer.send(new ProducerRecord<>(topic, behovssekvensId, behovssekvens)).get();
            LOG.info("Sendt OK topic={}, offset={}, partition={}", metadata.topic(), metadata.offset(), metadata.partition());
        } catch (InterruptedException e) { // TODO
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } finally {
            MDC.remove(BEHOVESSEKVENS_ID);
        }
    }


    private void setUsernameAndPassword(String username, String password, Properties properties) {
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            String jaasTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";";
            String jaasCfg = String.format(jaasTemplate, username, password);
            properties.put("sasl.jaas.config", jaasCfg);
        }
    }

    private Producer<String, String> createProducer(Properties properties) {
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(properties);
    }

    private void setSecurity(String username, Properties properties) {
        if (username != null && !username.isEmpty()) {
            properties.put("security.protocol", "SASL_SSL");
            properties.put("sasl.mechanism", "PLAIN");
        }
    }
}
