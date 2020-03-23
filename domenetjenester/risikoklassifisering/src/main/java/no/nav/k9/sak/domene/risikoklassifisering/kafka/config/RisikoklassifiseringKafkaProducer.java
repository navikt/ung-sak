package no.nav.k9.sak.domene.risikoklassifisering.kafka.config;

public interface RisikoklassifiseringKafkaProducer {
    void publiserEvent(String key, String behandlingProsessEventJson);
}
