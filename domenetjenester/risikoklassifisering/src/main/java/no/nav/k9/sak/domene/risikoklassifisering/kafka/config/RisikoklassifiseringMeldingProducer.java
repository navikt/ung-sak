package no.nav.k9.sak.domene.risikoklassifisering.kafka.config;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.vedtak.konfig.KonfigVerdi;

@Dependent
class RisikoklassifiseringMeldingProducer extends GenerellMeldingProducer implements MeldingProducer {

    public RisikoklassifiseringMeldingProducer() {
        // for CDI proxy
    }

    @Inject
    public RisikoklassifiseringMeldingProducer(@KonfigVerdi("kafka.risikoklassifisering.topic") String topic,
                                               @KonfigVerdi("bootstrap.servers") String bootstrapServers,
                                               @KonfigVerdi("schema.registry.url") String schemaRegistryUrl,
                                               @KonfigVerdi("systembruker.username") String username,
                                               @KonfigVerdi("systembruker.password") String password) {
        super(topic, bootstrapServers, schemaRegistryUrl, "KP-" + topic, username, password);
    }
}
