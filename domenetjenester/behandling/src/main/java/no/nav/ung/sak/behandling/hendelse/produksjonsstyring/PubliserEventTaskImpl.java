package no.nav.ung.sak.behandling.hendelse.produksjonsstyring;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;

/**
 * Publiserer ulike prosesshendelser på kafka slik som aksjonspunkter funnet,, prosess stoppet, behandling lagt på vent, behandlende enhet
 * endret. Disse konsumeres primært av oppgavebehandling (eks. K9-Los).
 */
@ApplicationScoped
@ProsessTask(PubliserEventTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class PubliserEventTaskImpl implements PubliserEventTask {

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
        kafkaProducer.sendHendelse(key, eventJson);
        kafkaProducer.flush(); //for å sikre at ikke hendelser sniker i køen ved å bli publisert fra en annen node
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        prosesser(prosessTaskData);
    }
}
