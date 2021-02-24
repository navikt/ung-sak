package no.nav.k9.sak.ytelse.omsorgspenger.behov;

import no.nav.vedtak.konfig.KonfigVerdi;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.UUID;

@ApplicationScoped
public class BehovKafkaProducer extends BehovKlient {
    private static final String BEHOVSSEKVENS_ID = "behovssekvens_id";
    private static final Logger LOG = LoggerFactory.getLogger(BehovKafkaProducer.class);

    private String clientId;
    private String topic;
    private Producer<String, String> producer;

    BehovKafkaProducer() {}

    @Inject
    public BehovKafkaProducer(
        @KonfigVerdi(value = "KAFKA_BEHOV_TOPIC", defaultVerdi = "k9-rapid-v2", required = false) String topic,
        @KonfigVerdi("BOOTSTRAP_SERVERS") String bootstrapServers,
        @KonfigVerdi("SYSTEMBRUKER_USERNAME") String username,
        @KonfigVerdi("SYSTEMBRUKER_PASSWORD") String password) {
        this.clientId = clientId();
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, this.clientId);
        addCredentials(username, password, properties);
        this.topic = topic;
        this.producer = createProducer(properties);
    }

    @Override
    public void send(String behovssekvensId, String behovssekvens) {
        try {
            MDC.put(BEHOVSSEKVENS_ID, behovssekvensId);
            var metadata = producer.send(new ProducerRecord<>(topic, behovssekvensId, behovssekvens)).get();
            LOG.info("Sendt OK clientId={}, topic={}, offset={}, partition={}", clientId, metadata.topic(), metadata.offset(), metadata.partition());
        } catch (Exception e) {
            throw new BehovKafkaException(String.format("Oppsto feil når clientId=%s skulle sende behov på topic=%s", clientId, topic), e);
        } finally {
            MDC.remove(BEHOVSSEKVENS_ID);
        }
    }

    private static String clientId() {
        if (System.getenv().containsKey("NAIS_APP_NAME")) {
            try {
                return InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                return UUID.randomUUID().toString();
            }
        } else {
            return UUID.randomUUID().toString();
        }
    }

    private static void addCredentials(String username, String password, Properties properties) {
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            String jaasTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";";
            String jaasCfg = String.format(jaasTemplate, username, password);
            properties.put(SaslConfigs.SASL_JAAS_CONFIG, jaasCfg);
            properties.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
            properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        }
    }

    private static Producer<String, String> createProducer(Properties properties) {
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(properties);
    }
}
