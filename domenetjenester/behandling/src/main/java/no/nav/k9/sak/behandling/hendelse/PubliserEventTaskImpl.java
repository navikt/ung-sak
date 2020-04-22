package no.nav.k9.sak.behandling.hendelse;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.sak.behandling.PubliserEventTask;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

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
        String key = prosessTaskData.getPropertyValue(PROPERTY_KEY);
        var eventJson = prosessTaskData.getPayloadAsString();
        kafkaProducer.sendJsonMedNøkkel(key, eventJson);
        log.info("Publisert aksjonspunktevent for behandling med id='{}' & value='{}'", key, eventJson);
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        prosesser(prosessTaskData);
    }
}
