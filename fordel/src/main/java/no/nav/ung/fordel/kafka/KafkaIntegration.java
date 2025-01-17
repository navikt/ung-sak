package no.nav.ung.fordel.kafka;

import no.nav.k9.felles.apptjeneste.AppServiceHandler;

public interface KafkaIntegration extends AppServiceHandler {

    /**
     * Er integrasjonen i live.
     *
     * @return true / false
     */
    boolean isAlive();
}
