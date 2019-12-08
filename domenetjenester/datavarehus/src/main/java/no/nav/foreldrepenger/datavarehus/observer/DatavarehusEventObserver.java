package no.nav.foreldrepenger.datavarehus.observer;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.FagsakStatusEvent;
import no.nav.foreldrepenger.behandling.impl.BehandlingEnhetEvent;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegTilstandSnapshot;
import no.nav.foreldrepenger.behandlingskontroll.events.AksjonspunktAvbruttEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.AksjonspunktTilbakeførtEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.AksjonspunktUtførtEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.AksjonspunkterFunnetEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStegTilstandEndringEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakEvent;

@ApplicationScoped
public class DatavarehusEventObserver {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    public DatavarehusEventObserver() {
    }

    public void observerAksjonspunktUtførtEvent(@Observes AksjonspunktUtførtEvent event) {
        List<Aksjonspunkt> aksjonspunkter = event.getAksjonspunkter();
        log.debug("Lagrer {} aksjonspunkter i DVH datavarehus, for behandling {} og steg {}", aksjonspunkter.size(), event.getBehandlingId(), event.getBehandlingStegType());//NOSONAR
    }

    public void observerAksjonspunkterFunnetEvent(@Observes AksjonspunkterFunnetEvent event) {
        List<Aksjonspunkt> aksjonspunkter = event.getAksjonspunkter();
        log.debug("Lagrer {} aksjonspunkter i DVH datavarehus, for behandling {} og steg {}", aksjonspunkter.size(), event.getBehandlingId(), event.getBehandlingStegType());//NOSONAR
    }

    public void observerAksjonspunktTilbakeførtEvent(@Observes AksjonspunktTilbakeførtEvent event) {
        List<Aksjonspunkt> aksjonspunkter = event.getAksjonspunkter();
        log.debug("Lagrer {} aksjonspunkter i DVH datavarehus, for behandling {} og steg {}", aksjonspunkter.size(), event.getBehandlingId(), event.getBehandlingStegType());//NOSONAR
    }

    public void observerAksjonspunktAvbruttEvent(@Observes AksjonspunktAvbruttEvent event) {
        List<Aksjonspunkt> aksjonspunkter = event.getAksjonspunkter();
        log.debug("Lagrer {} aksjonspunkter i DVH datavarehus, for behandling {} og steg {}", aksjonspunkter.size(), event.getBehandlingId(), event.getBehandlingStegType());//NOSONAR
    }

    public void observerFagsakStatus(@Observes FagsakStatusEvent event) {
        log.debug("Lagrer fagsak {} i DVH mellomalger", event.getFagsakId());//NOSONAR
    }

    public void observerBehandlingStegTilstandEndringEvent(@Observes BehandlingStegTilstandEndringEvent event) {
        Optional<BehandlingStegTilstandSnapshot> fraTilstand = event.getFraTilstand();
        if (fraTilstand.isPresent()) {
            BehandlingStegTilstandSnapshot tilstand = fraTilstand.get();
            log.debug("Lagrer behandligsteg endring fra tilstand {} i DVH datavarehus for behandling {}; behandlingStegTilstandId {}", //NOSONAR
                tilstand.getSteg().getKode(), event.getBehandlingId(), tilstand.getId());
        }
        Optional<BehandlingStegTilstandSnapshot> tilTilstand = event.getTilTilstand();
        if (tilTilstand.isPresent() && !Objects.equals(tilTilstand.orElse(null), fraTilstand.orElse(null))) {
            BehandlingStegTilstandSnapshot tilstand = tilTilstand.get();
            log.debug("Lagrer behandligsteg endring til tilstand {} i DVH datavarehus for behandlingId {}; behandlingStegTilstandId {}", //NOSONAR
                tilstand.getSteg().getKode(), event.getBehandlingId(), tilstand.getId());
        }
    }

    public void observerBehandlingEnhetEvent(@Observes BehandlingEnhetEvent event) {
        log.debug("Lagrer behandling {} i DVH datavarehus", event.getBehandlingId());//NOSONAR
    }

    public void observerBehandlingOpprettetEvent(@Observes BehandlingStatusEvent.BehandlingOpprettetEvent event) {
        log.debug("Lagrer behandling {} i DVH datavarehus", event.getBehandlingId());//NOSONAR
    }

    public void observerBehandlingAvsluttetEvent(@Observes BehandlingStatusEvent.BehandlingAvsluttetEvent event) {
        log.debug("Lagrer behandling {} i DVH datavarehus", event.getBehandlingId());//NOSONAR
    }

    public void observerBehandlingVedtakEvent(@Observes BehandlingVedtakEvent event) {
        log.debug("Lagrer vedtak {} for behandling {} i DVH datavarehus", event.getVedtak().getId(), event.getBehandlingId());//NOSONAR
    }

}
