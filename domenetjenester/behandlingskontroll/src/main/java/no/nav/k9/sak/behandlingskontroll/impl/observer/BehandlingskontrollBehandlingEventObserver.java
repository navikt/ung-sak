package no.nav.k9.sak.behandlingskontroll.impl.observer;

import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegTilstandSnapshot;
import no.nav.k9.sak.behandlingskontroll.events.BehandlingStegOvergangEvent;
import no.nav.k9.sak.behandlingskontroll.impl.BehandlingskontrollEventPubliserer;
import no.nav.k9.sak.behandlingskontroll.spi.BehandlingskontrollServiceProvider;

/**
 * Observerer og propagerer / håndterer events internt i Behandlingskontroll
 */
@ApplicationScoped
public class BehandlingskontrollBehandlingEventObserver {
    private static final Logger log = LoggerFactory.getLogger(BehandlingskontrollBehandlingEventObserver.class);

    private BehandlingskontrollEventPubliserer eventPubliserer;

    BehandlingskontrollBehandlingEventObserver() {
    }

    @Inject
    public BehandlingskontrollBehandlingEventObserver(BehandlingskontrollServiceProvider serviceProvider) {
        this.eventPubliserer = serviceProvider.getEventPubliserer();
    }

    /**
     * Intern event propagering i Behandlingskontroll.
     * <p>
     * Observer {@link BehandlingStegOvergangEvent} og propagerer events for {@link BehandlingStegStatusEvent} og
     * {@link BehandlingStatusEvent}
     * endringer
     */
    public void propagerBehandlingStatusEventVedStegOvergang(@Observes BehandlingStegOvergangEvent event) {

        if (eventPubliserer == null) {
            // gjør ingenting
            return;
        }

        Optional<BehandlingStegTilstandSnapshot> fraTilstand = event.getFraTilstand();
        Optional<BehandlingStegTilstandSnapshot> tilTilstand = event.getTilTilstand();

        if ((fraTilstand.isEmpty() && tilTilstand.isEmpty())
            || (fraTilstand.isPresent() && tilTilstand.isPresent() && Objects.equals(fraTilstand.get(), tilTilstand.get()))) {
            // gjør ingenting - ingen endring i steg
            return;
        }

        log.info("transisjon fra {} til {}", fraTilstand, tilTilstand);

        // fyr behandling status event
        BehandlingStatus gammelStatus = null;
        if (fraTilstand.isPresent()) {
            gammelStatus = fraTilstand.get().getSteg().getDefinertBehandlingStatus();
        }
        BehandlingStatus nyStatus = null;
        if (tilTilstand.isPresent()) {
            nyStatus = tilTilstand.get().getSteg().getDefinertBehandlingStatus();
        }

        // fyr behandling status event
        if (!Objects.equals(gammelStatus, nyStatus)) {
            eventPubliserer.fireEvent(event.getKontekst(), gammelStatus, nyStatus);
        }
    }
}
