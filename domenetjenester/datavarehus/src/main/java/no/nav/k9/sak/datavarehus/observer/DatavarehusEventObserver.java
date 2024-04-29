package no.nav.k9.sak.datavarehus.observer;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandling.FagsakStatusEvent;
import no.nav.k9.sak.behandling.hendelse.BehandlingEnhetEvent;
import no.nav.k9.sak.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.k9.sak.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakEvent;

@ApplicationScoped
public class DatavarehusEventObserver {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    public DatavarehusEventObserver() {
    }

    public void observerAksjonspunktStatusEvent(@Observes AksjonspunktStatusEvent event) {
        List<Aksjonspunkt> aksjonspunkter = event.getAksjonspunkter();
        log.debug("Lagrer {} aksjonspunkter i DVH datavarehus, for behandling {} og steg {}", aksjonspunkter.size(), event.getBehandlingId(), event.getBehandlingStegType());//NOSONAR
    }

    public void observerFagsakStatus(@Observes FagsakStatusEvent event) {
        log.debug("Lagrer fagsak {} i DVH mellomalger", event.getFagsakId());//NOSONAR
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
