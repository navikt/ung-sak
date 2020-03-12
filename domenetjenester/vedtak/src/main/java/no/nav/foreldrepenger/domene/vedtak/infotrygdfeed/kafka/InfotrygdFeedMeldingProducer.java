package no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.kafka;

import no.nav.vedtak.konfig.KonfigVerdi;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InfotrygdFeedMeldingProducer extends GenerellMeldingProducer {
    public InfotrygdFeedMeldingProducer() {
        // for CDI proxy
    }

    public InfotrygdFeedMeldingProducer(
        @KonfigVerdi("kafka.infotrygdfeed.topic") String topic,
        @KonfigVerdi("bootstrap.servers") String bootstrapServers,
        @KonfigVerdi("schema.registry.url") String schemaRegistryUrl,
        @KonfigVerdi("kafka.infotrygdfeed.client.id") String clientId,
        @KonfigVerdi("systembruker.username") String username,
        @KonfigVerdi("systembruker.password") String password
    ) {
        super(topic, bootstrapServers, schemaRegistryUrl, clientId, username, password);
    }
}
