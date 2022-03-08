package no.nav.k9.sak.behandling.hendelse.produksjonsstyring;

import no.nav.k9.prosesstask.api.ProsessTaskHandler;

public interface PubliserEventTask extends ProsessTaskHandler {
    String TASKTYPE = "oppgavebehandling.PubliserEvent";
    String PROPERTY_KEY = "topicKey";
    String BESKRIVELSE = "event.beskrivelse";
}
