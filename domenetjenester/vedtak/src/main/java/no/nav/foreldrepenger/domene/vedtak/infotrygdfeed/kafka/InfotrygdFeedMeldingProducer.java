package no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.kafka;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class InfotrygdFeedMeldingProducer extends GenerellMeldingProducer {
    public InfotrygdFeedMeldingProducer() {
        // for CDI proxy
    }

    @Inject
    public InfotrygdFeedMeldingProducer(@KonfigVerdi("kafka.infotrygdfeed.topic") String topic,
                                        @KonfigVerdi("bootstrap.servers") String bootstrapServers,
                                        @KonfigVerdi("schema.registry.url") String schemaRegistryUrl,
                                        @KonfigVerdi("kafka.infotrygdfeed.client.id") String clientId,
                                        @KonfigVerdi("systembruker.username") String username,
                                        @KonfigVerdi("systembruker.password") String password) {
        super(topic, bootstrapServers, schemaRegistryUrl, clientId, username, password);
    }
}
