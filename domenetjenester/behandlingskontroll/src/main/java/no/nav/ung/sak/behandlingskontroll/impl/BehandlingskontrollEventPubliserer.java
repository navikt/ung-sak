package no.nav.ung.sak.behandlingskontroll.impl;

import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegTilstandSnapshot;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingStegOvergangEvent;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingStegStatusEvent;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingTransisjonEvent;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingskontrollEvent;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingEvent;

/**
 * Håndterer fyring av events via CDI når det skjer en overgang i Behandlingskontroll mellom steg, eller statuser
 */
@ApplicationScoped
public class BehandlingskontrollEventPubliserer {

    public static final BehandlingskontrollEventPubliserer NULL_EVENT_PUB = new BehandlingskontrollEventPubliserer();
    private Event<BehandlingEvent> behandlingEvent;

    BehandlingskontrollEventPubliserer() {
        // null ctor, publiserer ingen events
    }

    @Inject
    public BehandlingskontrollEventPubliserer(@Any Event<BehandlingEvent> behandlingEvent) {
        this.behandlingEvent = behandlingEvent;
    }

    public void fireEvent(BehandlingStegOvergangEvent event) {
        Optional<BehandlingStegTilstandSnapshot> fraTilstand = event.getFraTilstand();
        Optional<BehandlingStegTilstandSnapshot> nyTilstand = event.getTilTilstand();
        if ((fraTilstand.isEmpty() && nyTilstand.isEmpty())
            || (fraTilstand.isPresent() && nyTilstand.isPresent() && Objects.equals(fraTilstand.get(), nyTilstand.get()))) {
            // ikke fyr duplikate events
            return;
        }

        doFireEvent(event);
    }

    public void fireEvent(BehandlingTransisjonEvent event) {
        doFireEvent(event);
    }

    public void fireEvent(BehandlingskontrollKontekst kontekst, BehandlingStegType stegType, BehandlingStegStatus forrigeStatus,
                          BehandlingStegStatus nyStatus) {
        if (Objects.equals(forrigeStatus, nyStatus)) {
            // gjør ingenting
            return;
        }
        doFireEvent(new BehandlingStegStatusEvent(kontekst, stegType, forrigeStatus, nyStatus));
    }

    public void fireEvent(BehandlingskontrollKontekst kontekst, BehandlingStatus gammelStatus, BehandlingStatus nyStatus) {
        if (Objects.equals(gammelStatus, nyStatus)) {
            // gjør ingenting
            return;
        }
        doFireEvent(BehandlingStatusEvent.nyEvent(kontekst, nyStatus, gammelStatus));
    }

    public void fireEvent(BehandlingskontrollEvent event) {
        doFireEvent(event);
    }

    public void fireEvent(AksjonspunktStatusEvent event) {
        doFireEvent(event);
    }

    /**
     * Fyrer event via BeanManager slik at håndtering av events som subklasser andre events blir korrekt.
     */
    protected void doFireEvent(BehandlingEvent event) {
        if (behandlingEvent == null) {
            return;
        }
        behandlingEvent.fire(event);
    }

}
