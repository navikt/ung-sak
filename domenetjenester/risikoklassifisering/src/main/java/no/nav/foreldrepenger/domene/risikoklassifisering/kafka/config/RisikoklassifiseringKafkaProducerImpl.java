package no.nav.foreldrepenger.domene.risikoklassifisering.kafka.config;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
class RisikoklassifiseringKafkaProducerImpl implements RisikoklassifiseringKafkaProducer {

    private MeldingProducer meldingProducer;

    RisikoklassifiseringKafkaProducerImpl(){

    }

    @Inject
    public RisikoklassifiseringKafkaProducerImpl(RisikoklassifiseringMeldingProducer meldingProducer) {
        this.meldingProducer = meldingProducer;
    }

    @Override
    public void publiserEvent(String key, String hendelseJson) {
        meldingProducer.sendJsonMedNøkkel(key,hendelseJson);
    }
}
