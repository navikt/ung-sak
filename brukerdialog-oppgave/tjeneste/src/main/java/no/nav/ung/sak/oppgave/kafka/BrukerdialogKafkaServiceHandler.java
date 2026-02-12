package no.nav.ung.sak.oppgave.kafka;

import jakarta.inject.Inject;
import no.nav.k9.felles.apptjeneste.AppServiceHandler;
import no.nav.ung.sak.oppgave.typer.varsel.kafka.SvarPåVarselHendelseHåndterer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class BrukerdialogKafkaServiceHandler implements AppServiceHandler {


    private static final Logger LOG = LoggerFactory.getLogger(BrukerdialogKafkaServiceHandler.class);

    private KafkaConsumerManager<String, String> kcm;


    @Inject
    public BrukerdialogKafkaServiceHandler(SvarPåVarselHendelseHåndterer svarPåVarselHendelseHåndterer) {
        this.kcm = new KafkaConsumerManager<>(List.of(svarPåVarselHendelseHåndterer));
    }

    @Override
    public void start() {
        LOG.info("Starter konsumering av topics={}", kcm.topicNames());
        kcm.start((t, e) -> LOG.error("{} :: Caught exception in stream, exiting", t, e));
    }

    @Override
    public void stop() {
        LOG.info("Starter shutdown av topics={} med 10 sekunder timeout", kcm.topicNames());
        kcm.stop();
    }

    public boolean allRunning() {
        return kcm.allRunning();
    }

    public String getTopicNames() {
        return kcm.topicNames();
    }


}
