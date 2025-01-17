package no.nav.ung.fordel.kafka;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.common.serialization.Serde;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.fordel.auth.SystembrukerClientCredentials;

@Dependent
public class EarliestOffsetResetKafkaSettings implements KafkaSettings {

    private final DefaultKafkaSettings defaultKafkaSettings;

    @Inject
    EarliestOffsetResetKafkaSettings(@KonfigVerdi("kafka.bootstrap.servers") String bootstrapServers,
                                     @KonfigVerdi(value = "kafka.schema.registry.url", required = false) String schemaRegistryUrl,
                                     @KonfigVerdi(value = "javax.net.ssl.trustStore") String trustStorePath,
                                     @KonfigVerdi(value = "javax.net.ssl.trustStorePassword") String trustStorePassword,
                                     @KonfigVerdi(value = "KAFKA_DEACTIVATED_TOPICS", defaultVerdi = "", required = false) String deactivatedTopics,
                                     SystembrukerClientCredentials clientCredentials) {
        defaultKafkaSettings = new DefaultKafkaSettings(bootstrapServers, schemaRegistryUrl, trustStorePath, trustStorePassword, true, deactivatedTopics, clientCredentials);
    }

    @Override
    public String getSchemaRegistryUrl() {
        return defaultKafkaSettings.getSchemaRegistryUrl();
    }

    @Override
    public Map<String, String> schemaRegistryProps() {
        return defaultKafkaSettings.schemaRegistryProps();
    }

    @Override
    public Properties toStreamPropertiesWith(String clientId, Serde<?> keySerde, Serde<?> valueSerde) {
        return defaultKafkaSettings.toStreamPropertiesWith(clientId, keySerde, valueSerde);
    }

    @Override
    public Properties toProducerPropertiesWith(String navn) {
        return defaultKafkaSettings.toProducerPropertiesWith(navn);
    }

    @Override
    public Properties toStreamPropertiesWith(String navn) {
        return defaultKafkaSettings.toStreamPropertiesWith(navn);
    }

    @Override
    public List<String> getDeactivatedTopics() {
        return defaultKafkaSettings.getDeactivatedTopics();
    }
}
