package no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.kafka;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;

@ApplicationScoped
public class InfotrygdFeedMeldingProducer extends GenerellMeldingProducer {
    public InfotrygdFeedMeldingProducer() {
        // for CDI proxy
    }

    @Inject
    public InfotrygdFeedMeldingProducer(@KonfigVerdi(value = "KAFKA_BROKERS") String bootstrapServers,
                                        @KonfigVerdi(value = "KAFKA_INFOTRYGDFEED_AIVEN_TOPIC") String topic,
                                        @KonfigVerdi(value = "KAFKA_TRUSTSTORE_PATH", required = false) String trustStorePath,
                                        @KonfigVerdi(value = "KAFKA_CREDSTORE_PASSWORD", required = false) String trustStorePassword,
                                        @KonfigVerdi(value = "KAFKA_KEYSTORE_PATH", required = false) String keyStorePath,
                                        @KonfigVerdi(value = "KAFKA_CREDSTORE_PASSWORD", required = false) String keyStorePassword,
                                        @KonfigVerdi(value = "KAFKA_OVERRIDE_KEYSTORE_PASSWORD", required = false) String vtpOverride) {

        super(topic, bootstrapServers, "KP-" + topic, trustStorePath, trustStorePassword, keyStorePath, keyStorePassword, vtpOverride);
    }
}
