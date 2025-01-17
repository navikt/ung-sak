package no.nav.ung.fordel.kafka;

import java.util.Objects;

import org.apache.kafka.common.serialization.Serde;

public class Topic<K, V> {

    private final String topic;
    private final Serde<K> serdeKey;
    private final Serde<V> serdeValue;

    @SuppressWarnings("resource")
    public Topic(String topic, Serde<K> serdeKey, Serde<V> serdeValue) {
        Objects.requireNonNull(topic, "topic");
        Objects.requireNonNull(serdeKey, "serdeKey");
        Objects.requireNonNull(serdeValue, "serdeValue");
        this.topic = topic;
        this.serdeKey = serdeKey;
        this.serdeValue = serdeValue;
    }

    public String getTopic() {
        return topic;
    }

    public String getTopicWithEnv(Environment environment) {
        return topic + "-" + environment.getKode();
    }


    public Serde<K> getSerdeKey() {
        return serdeKey;
    }

    public Serde<V> getSerdeValue() {
        return serdeValue;
    }

    /**
     * Genererer clientId basert på standard definert på https://confluence.adeo.no/display/AURA/Kafka#Kafka-TopicogSikkerhetskonfigurasjon
     *
     * @return clientId
     */
    public String getProducerClientId() {
        return "KP-" + topic;
    }

    /**
     * Genererer clientId basert på standard definert på https://confluence.adeo.no/display/AURA/Kafka#Kafka-TopicogSikkerhetskonfigurasjon
     *
     * @return clientId
     */
    public String getConsumerClientId() {
        return "KC-" + topic;
    }

    @Override
    public String toString() {
        return "Topic{" +
                "topic='" + topic + '\'' +
                ", serdeKey=" + serdeKey +
                ", serdeValue=" + serdeValue +
                '}';
    }
}

