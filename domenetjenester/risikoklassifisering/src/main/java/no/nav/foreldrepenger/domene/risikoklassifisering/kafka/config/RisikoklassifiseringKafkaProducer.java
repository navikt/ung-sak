package no.nav.foreldrepenger.domene.risikoklassifisering.kafka.config;

public interface RisikoklassifiseringKafkaProducer {
    void publiserEvent(String key, String behandlingProsessEventJson);
}
