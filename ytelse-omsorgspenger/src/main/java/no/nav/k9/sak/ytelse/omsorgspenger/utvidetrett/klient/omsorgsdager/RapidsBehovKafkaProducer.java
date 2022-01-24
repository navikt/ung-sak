package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.omsorgsdager;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.kafka.KafkaProducerAiven;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;

import java.util.Collections;
import java.util.Map;

@ApplicationScoped
public class RapidsBehovKafkaProducer extends RapidsBehovKlient {
    private KafkaProducerAiven producer;

    RapidsBehovKafkaProducer() {
    }

    @Inject
    public RapidsBehovKafkaProducer(
        @KonfigVerdi(value = "KAFKA_OMS_RAPID_TOPIC", defaultVerdi = "omsorgspenger.k9-rapid-v2") String topic,
        @KonfigVerdi(value = "KAFKA_BROKERS") String bootstrapServers,
        @KonfigVerdi(value = "KAFKA_TRUSTSTORE_PATH") String truststorePath,
        @KonfigVerdi(value = "KAFKA_KEYSTORE_PATH") String keystorePath,
        @KonfigVerdi(value = "KAFKA_CREDSTORE_PASSWORD") String credstorePassword) {

        Map<String, String> optionalProperties = Collections.emptyMap();
        var clientId = "k9-sak";
        this.producer = new KafkaProducerAiven(topic, bootstrapServers, truststorePath, keystorePath, credstorePassword, clientId, optionalProperties);
    }

    @Override
    public void send(String behovssekvensId, String behovssekvens) {
        producer.send(behovssekvensId, behovssekvens);
    }
}
