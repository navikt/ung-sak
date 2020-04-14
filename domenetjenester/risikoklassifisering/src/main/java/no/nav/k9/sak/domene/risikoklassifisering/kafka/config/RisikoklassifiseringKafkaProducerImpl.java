package no.nav.k9.sak.domene.risikoklassifisering.kafka.config;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

@Dependent
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
