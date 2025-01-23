package no.nav.ung.fordel.kafka.utils;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import no.nav.ung.fordel.kafka.AivenKafkaSettings;
import no.nav.ung.fordel.kafka.Topic;
import org.apache.kafka.common.serialization.Serde;

import java.util.Map;

import static io.confluent.kafka.serializers.KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG;

public class KafkaUtils {

    public static String getBasicAuth(String schemaRegistryUsername, String schemaRegistryPassword) {
        return schemaRegistryUsername + ":" + schemaRegistryPassword;
    }

    public static <K, V> Topic<K, V> configureAvroTopic(String topicName, AivenKafkaSettings kafkaSettings, Serde<K> serdeKey, Serde<V> serdeValue) {
        var configuredTopic = new Topic<>(topicName, serdeKey, serdeValue);
        var schemaRegistryUrl = kafkaSettings.getSchemaRegistryUrl();
        var basicAuth = KafkaUtils.getBasicAuth(kafkaSettings.getSchemaRegistryUser(), kafkaSettings.getSchemaRegistryPass());

        if (schemaRegistryUrl != null && !schemaRegistryUrl.isEmpty()) {
            var schemaMap =
                Map.of(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl,
                    AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO",
                    AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG, basicAuth,
                    SPECIFIC_AVRO_READER_CONFIG, true);
            configuredTopic.getSerdeKey().configure(schemaMap, true);
            configuredTopic.getSerdeValue().configure(schemaMap, false);
        }
        return configuredTopic;
    }
}
