package no.nav.ung.sak.produksjonsstyring.behandlingenhet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandling.hendelse.BehandlingEnhetEvent;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;


@ApplicationScoped
public class BehandlingEnhetEventPubliserer {

    private Event<BehandlingEnhetEvent> behandlingEnhetEvent;

    BehandlingEnhetEventPubliserer() {
        //Cyclopedia Drainage Invariant
    }

    @Inject
    public BehandlingEnhetEventPubliserer(@Any Event<BehandlingEnhetEvent> behandlingEnhetEvent) {
        this.behandlingEnhetEvent = behandlingEnhetEvent;
    }

    public void fireEvent(Behandling behandling) {
        if (behandlingEnhetEvent == null) {
            return;
        }
        BehandlingEnhetEvent event = new BehandlingEnhetEvent(behandling);
        behandlingEnhetEvent.fire(event);
    }
}
