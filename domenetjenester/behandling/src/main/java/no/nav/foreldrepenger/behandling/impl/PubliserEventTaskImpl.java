package no.nav.foreldrepenger.behandling.impl;

import no.nav.foreldrepenger.behandling.PubliserEventTask;
import no.nav.foreldrepenger.behandling.impl.kafka.behandlingskontroll.AksjonspunktKafkaProducer;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
@ProsessTask(PubliserEventTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class PubliserEventTaskImpl implements PubliserEventTask {
    private static final Logger log = LoggerFactory.getLogger(PubliserEventTaskImpl.class);

    private AksjonspunktKafkaProducer kafkaProducer;

    PubliserEventTaskImpl() {
        // for CDI proxy
    }

    @Inject
    public PubliserEventTaskImpl(AksjonspunktKafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    protected void prosesser(ProsessTaskData prosessTaskData) {
        String eventJson = prosessTaskData.getPropertyValue(PROPERTY_EVENT);
        String key = prosessTaskData.getPropertyValue(PROPERTY_KEY);
        kafkaProducer.sendJsonMedNøkkel(key, eventJson);
        log.info("Publiser aksjonspunktevent på kafka slik at f.eks fplos kan fordele oppgaven for videre behandling. BehandlingsId: {}", key);
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        prosesser(prosessTaskData);
    }
}
