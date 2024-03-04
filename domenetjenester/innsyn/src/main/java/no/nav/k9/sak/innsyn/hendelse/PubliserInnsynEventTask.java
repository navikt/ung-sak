package no.nav.k9.sak.innsyn.hendelse;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.innsyn.BrukerdialoginnsynMeldingProducer;

@ApplicationScoped
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
@ProsessTask(PubliserInnsynEventTask.TASKTYPE)
public class PubliserInnsynEventTask implements ProsessTaskHandler {
    private static final Logger log = LoggerFactory.getLogger(PubliserInnsynEventTask.class);
    public static final String TASKTYPE = "innsyn.PubliserInnsynEvent";

    private BrukerdialoginnsynMeldingProducer producer;

    PubliserInnsynEventTask() {
        // for CDI proxy
    }

    @Inject
    public PubliserInnsynEventTask(BrukerdialoginnsynMeldingProducer producer) {
        this.producer = producer;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        Objects.requireNonNull(prosessTaskData.getSaksnummer());
        producer.send(prosessTaskData.getSaksnummer(), prosessTaskData.getPayloadAsString());
    }

}
