package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.omsorgsdager;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

@ApplicationScoped
public class RapidsBehovKafkaProducer extends RapidsBehovKlient {
    private static final String BEHOVSSEKVENS_ID = "behovssekvens_id";
    private static final Logger LOG = LoggerFactory.getLogger(RapidsBehovKafkaProducer.class);

    private String clientId;
    private String topic;
    private Producer<String, String> producer;

    RapidsBehovKafkaProducer() {
    }

    @Inject
    public RapidsBehovKafkaProducer(
        @KonfigVerdi(value = "KAFKA_BEHOV_TOPIC", defaultVerdi = "k9-rapid-v2", required = false) String topic,
        @KonfigVerdi(value = "KAFKA_BROKERS", required = false) String aivenBootstrapServers,
        @KonfigVerdi(value = "KAFKA_TRUSTSTORE_PATH", required = false) String aivenTruststorePath,
        @KonfigVerdi(value = "KAFKA_KEYSTORE_PATH", required = false) String aivenKeystorePath,
        @KonfigVerdi(value = "KAFKA_CREDSTORE_PASSWORD", required = false) String aivenCredstorePassword,
        @KonfigVerdi(value = "KAFKA_OVERRIDE_KEYSTORE_PASSWORD", required = false) String overrideKeystorePassword,
        @KonfigVerdi(value = "BOOTSTRAP_SERVERS", required = false) String onpremBootstrapServers,
        @KonfigVerdi(value = "K9_RAPID_AIVEN", defaultVerdi = "false") boolean isAivenInUse,
        @KonfigVerdi(value = "systembruker.username", defaultVerdi = "vtp") String username,
        @KonfigVerdi(value = "systembruker.password", defaultVerdi = "vtp") String password) {

        this.clientId = clientId();
        Properties properties = new Properties();
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, this.clientId);

        if (overrideKeystorePassword != null || !isAivenInUse) { // Ikke SSL for onprem & VTP.
            this.topic = topic;
            String jaasTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";";
            String jaasCfg = String.format(jaasTemplate, username, password);
            properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, onpremBootstrapServers);
            properties.put(SaslConfigs.SASL_JAAS_CONFIG, jaasCfg);
            properties.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
            properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        } else { // Aiven config
            this.topic = "omsorgspenger.k9-rapid-v2";
            properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, aivenBootstrapServers);
            properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name);
            properties.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");
            properties.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "JKS");
            properties.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12");
            properties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, aivenTruststorePath);
            properties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, aivenCredstorePassword);
            properties.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, aivenKeystorePath);
            properties.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, aivenCredstorePassword);
            properties.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, aivenCredstorePassword);
        }
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        this.producer = new KafkaProducer<>(properties);
    }

    @Override
    public void send(String behovssekvensId, String behovssekvens) {
        try {
            MDC.put(BEHOVSSEKVENS_ID, behovssekvensId);
            var metadata = producer.send(new ProducerRecord<>(topic, behovssekvensId, behovssekvens)).get();
            LOG.info("Sendt OK clientId={}, topic={}, offset={}, partition={}", clientId, metadata.topic(), metadata.offset(), metadata.partition());
        } catch (Exception e) {
            throw new RapidsBehovKafkaException(String.format("Oppsto feil når clientId=%s skulle sende behov på topic=%s", clientId, topic), e);
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

}
