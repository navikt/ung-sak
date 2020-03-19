package no.nav.k9.sak.domene.vedtak.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakEvent;

@ApplicationScoped
public class BehandlingVedtakEventPubliserer {

    private Event<BehandlingVedtakEvent> behandlingVedtakEvent;

    BehandlingVedtakEventPubliserer() {
        //Cyclopedia Drainage Invariant
    }

    @Inject
    public BehandlingVedtakEventPubliserer(Event<BehandlingVedtakEvent> behandlingVedtakEvent) {
        this.behandlingVedtakEvent = behandlingVedtakEvent;
    }

    public void fireEvent(BehandlingVedtak vedtak, Behandling behandling) {

        BehandlingVedtakEvent event = new BehandlingVedtakEvent(vedtak, behandling);
        behandlingVedtakEvent.fire(event);
    }
}
