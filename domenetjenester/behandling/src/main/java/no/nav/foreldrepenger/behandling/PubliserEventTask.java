package no.nav.foreldrepenger.behandling;

import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

public interface PubliserEventTask extends ProsessTaskHandler {
    String TASKTYPE = "oppgavebehandling.PubliserEvent";
    String PROPERTY_EVENT = "event";
    String PROPERTY_KEY = "topicKey";
}
