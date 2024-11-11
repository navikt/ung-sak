package no.nav.ung.sak.behandling.hendelse.produksjonsstyring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;

/**
 * Publiserer aksjonspunkthendelser som ny, lukket, avbrutt og gjenåpnet.
 * Disse konsumeres av oppgavebehandling (eks. K9-Los).
 */
@ApplicationScoped
@ProsessTask(PubliserProduksjonsstyringHendelseTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class PubliserProduksjonsstyringHendelseTaskImpl implements PubliserProduksjonsstyringHendelseTask {
    private static final Logger log = LoggerFactory.getLogger(PubliserProduksjonsstyringHendelseTaskImpl.class);

    private PubliserProduksjonsstyringHendelseProducer kafkaProducer;

    PubliserProduksjonsstyringHendelseTaskImpl() {
        // for CDI proxy
    }

    @Inject
    public PubliserProduksjonsstyringHendelseTaskImpl(PubliserProduksjonsstyringHendelseProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    protected void prosesser(ProsessTaskData prosessTaskData) {
        String key = prosessTaskData.getPropertyValue(PROPERTY_KEY);
        var eventJson = prosessTaskData.getPayloadAsString();
        kafkaProducer.sendJsonMedNøkkel(key, eventJson);
        kafkaProducer.flush(); //for å sikre at ikke hendelser sniker i køen ved å bli publisert fra en annen node
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        prosesser(prosessTaskData);
    }
}
