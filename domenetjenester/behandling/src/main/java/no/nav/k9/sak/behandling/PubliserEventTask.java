package no.nav.k9.sak.behandling;

import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

public interface PubliserEventTask extends ProsessTaskHandler {
    String TASKTYPE = "oppgavebehandling.PubliserEvent";
    String PROPERTY_KEY = "topicKey";
}
