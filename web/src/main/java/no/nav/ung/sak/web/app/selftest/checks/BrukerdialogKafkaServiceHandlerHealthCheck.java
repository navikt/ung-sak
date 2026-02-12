package no.nav.ung.sak.web.app.selftest.checks;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.oppgave.kafka.BrukerdialogKafkaServiceHandler;

@ApplicationScoped
public class BrukerdialogKafkaServiceHandlerHealthCheck extends ExtHealthCheck {
    private BrukerdialogKafkaServiceHandler serviceHandler;

    BrukerdialogKafkaServiceHandlerHealthCheck() {
    }

    @Inject
    public BrukerdialogKafkaServiceHandlerHealthCheck(BrukerdialogKafkaServiceHandler serviceHandler) {
        this.serviceHandler = serviceHandler;
    }

    @Override
    protected String getDescription() {
        return "Consumer hendelser for svar på oppgaver.";
    }

    @Override
    protected String getEndpoint() {
        return serviceHandler.getTopicNames();
    }

    @Override
    protected InternalResult performCheck() {
        InternalResult intTestRes = new InternalResult();
        if (serviceHandler.allRunning()) {
            intTestRes.setOk(true);
        } else {
            intTestRes.setOk(false);
        }
        intTestRes.noteResponseTime();
        return intTestRes;
    }
}
