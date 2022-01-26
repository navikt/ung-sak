package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.omsorgsdager;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.kafka.KafkaProducerAiven;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class RapidsBehovKafkaProducer extends RapidsBehovKlient {
    private static final String BEHOVSSEKVENS_ID = "behovssekvens_id";
    private static final Logger logger = LoggerFactory.getLogger(RapidsBehovKafkaProducer.class);
    private KafkaProducerAiven producer;
    private String clientId;
    private String topic;

    RapidsBehovKafkaProducer() {
    }

    @Inject
    public RapidsBehovKafkaProducer(
        @KonfigVerdi(value = "KAFKA_OMS_RAPID_TOPIC", defaultVerdi = "omsorgspenger.k9-rapid-v2") String topic,
        @KonfigVerdi(value = "KAFKA_BROKERS") String bootstrapServers,
        @KonfigVerdi(value = "KAFKA_TRUSTSTORE_PATH") String truststorePath,
        @KonfigVerdi(value = "KAFKA_TRUSTSTORE_PASSWORD", required = false) String truststorePassword,
        @KonfigVerdi(value = "KAFKA_KEYSTORE_PATH") String keystorePath,
        @KonfigVerdi(value = "KAFKA_CREDSTORE_PASSWORD") String credstorePassword) {

        Map<String, String> optionalProperties = Collections.emptyMap();

        if(truststorePassword == null) {
            truststorePassword = credstorePassword;
        }
        this.clientId = clientId();
        this.topic = topic;
        this.producer = new KafkaProducerAiven(topic, bootstrapServers, truststorePath, truststorePassword, keystorePath, credstorePassword, clientId, optionalProperties);
    }

    @Override
    public void send(String behovssekvensId, String behovssekvens) {
        MDC.put(BEHOVSSEKVENS_ID, behovssekvensId);
        producer.send(behovssekvensId, behovssekvens);
        logger.info("Sendt OK clientId={}, topic={}", clientId, topic);
        MDC.remove(BEHOVSSEKVENS_ID);
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
