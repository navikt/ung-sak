package no.nav.ung.sak.oppgave.kafka;

import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Optional;
import java.util.function.Supplier;

public interface KafkaMessageHandler<K,V> {

    void handleRecord(K key, V value);

    // Feature toggling - implement if the handler should be enabled. Default is enabled.
    default boolean enabled() {
        return true;
    }

    // Configuration
    String topic();
    String groupId(); // Keep stable (or it will read from autoOffsetReset()
    default Optional<OffsetResetStrategy> autoOffsetReset() {  // Implement if other than default (LATEST). Use NONE to discover low-volume topics
        return Optional.empty();
    }

    // Deserialization - should be configured if Avro. Provided as Supplier to handle Closeable
    Supplier<Deserializer<K>> keyDeserializer();
    Supplier<Deserializer<V>> valueDeserializer();

    // Implement KafkaStringMessageHandler for json-topics. The above are for Avro-topics
    interface KafkaStringMessageHandler extends KafkaMessageHandler<String, String> {
        default Supplier<Deserializer<String>> keyDeserializer() {
            return StringDeserializer::new;
        }

        default Supplier<Deserializer<String>> valueDeserializer() {
            return StringDeserializer::new;
        }
    }
}
