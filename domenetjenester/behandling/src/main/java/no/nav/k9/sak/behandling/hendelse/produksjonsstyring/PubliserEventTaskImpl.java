package no.nav.k9.sak.behandling.hendelse.produksjonsstyring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;

/**
 * Publiserer ulike prosesshendelser på kafka slik som aksjonspunkter funnet,, prosess stoppet, behandling lagt på vent, behandlende enhet
 * endret. Disse konsumeres primært av oppgavebehandling (eks. K9-Los).
 */
@ApplicationScoped
@ProsessTask(PubliserEventTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class PubliserEventTaskImpl implements PubliserEventTask {
    private static final Logger log = LoggerFactory.getLogger(PubliserEventTaskImpl.class);

    private ProsessEventKafkaProducer kafkaProducer;

    PubliserEventTaskImpl() {
        // for CDI proxy
    }

    @Inject
    public PubliserEventTaskImpl(ProsessEventKafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    protected void prosesser(ProsessTaskData prosessTaskData) {
        String key = prosessTaskData.getPropertyValue(PROPERTY_KEY);
        var eventJson = prosessTaskData.getPayloadAsString();
        String beskrivelse = prosessTaskData.getPropertyValue(BESKRIVELSE);
        kafkaProducer.sendJsonMedNøkkel(key, eventJson);
        log.info("Publisert aksjonspunktevent for behandling[{}], beskrivelse={}", key, beskrivelse);
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        prosesser(prosessTaskData);
    }
}
