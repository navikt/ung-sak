package no.nav.ung.fordel.kafka;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.common.serialization.Serde;

public interface KafkaSettings {
    Properties toStreamPropertiesWith(String clientId, Serde<?> keySerde, Serde<?> valueSerde);
    Properties toProducerPropertiesWith(String navn);
    Properties toStreamPropertiesWith(String navn);
    String getSchemaRegistryUrl();
    List<String> getDeactivatedTopics();

    Map<String, String> schemaRegistryProps();
}
