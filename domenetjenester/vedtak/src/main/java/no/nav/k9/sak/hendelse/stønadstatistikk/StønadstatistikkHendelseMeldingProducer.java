package no.nav.k9.sak.hendelse.stønadstatistikk;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.kafka.GenerellMeldingProducer;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;

@ApplicationScoped
public class StønadstatistikkHendelseMeldingProducer extends GenerellMeldingProducer {
    public StønadstatistikkHendelseMeldingProducer() {
        // for CDI proxy
    }

    @Inject
    public StønadstatistikkHendelseMeldingProducer(@KonfigVerdi("kafka.stonadstatistikk.topic") String topic,
                                        @KonfigVerdi("bootstrap.servers") String bootstrapServers,
                                        @KonfigVerdi("schema.registry.url") String schemaRegistryUrl,
                                        @KonfigVerdi("systembruker.username") String username,
                                        @KonfigVerdi("systembruker.password") String password) {
        super(topic, bootstrapServers, schemaRegistryUrl, "KP-" + topic, username, password);
    }
}
