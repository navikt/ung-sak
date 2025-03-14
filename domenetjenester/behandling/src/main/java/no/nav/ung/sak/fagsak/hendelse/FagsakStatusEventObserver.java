package no.nav.ung.sak.fagsak.hendelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;

/**
 * Observerer og propagerer / håndterer events internt i Behandlingskontroll
 */
@ApplicationScoped
public class FagsakStatusEventObserver {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private Instance<OppdaterFagsakStatus> oppdaterFagsakStatuser;
    private BehandlingRepository behandlingRepository;

    FagsakStatusEventObserver() {
        // For CDI
    }

    @Inject
    public FagsakStatusEventObserver(@Any Instance<OppdaterFagsakStatus> oppdaterFagsakStatuser, BehandlingRepository behandlingRepository) {
        this.oppdaterFagsakStatuser = oppdaterFagsakStatuser;
        this.behandlingRepository = behandlingRepository;
    }

    public void observerBehandlingOpprettetEvent(@Observes BehandlingStatusEvent.BehandlingOpprettetEvent event) {
        log.debug("Oppdaterer status på Fagsak etter endring i behandling {}", event.getBehandlingId());//NOSONAR
        Behandling behandling = behandlingRepository.hentBehandling(event.getBehandlingId());
        OppdaterFagsakStatus oppdaterFagsakStatus = FagsakYtelseTypeRef.Lookup.find(oppdaterFagsakStatuser, behandling.getFagsakYtelseType())
            .orElseThrow(() -> new IllegalStateException("Ingen implementasjoner funnet for ytelse: " + behandling.getFagsakYtelseType().getKode()));
        oppdaterFagsakStatus.oppdaterFagsakNårBehandlingEndret(behandling);
    }

    public void observerBehandlingAvsluttetEvent(@Observes BehandlingStatusEvent.BehandlingAvsluttetEvent event) {
        log.debug("Oppdaterer status på Fagsak etter endring i behandling {}", event.getBehandlingId());//NOSONAR
        Behandling behandling = behandlingRepository.hentBehandling(event.getBehandlingId());
        OppdaterFagsakStatus oppdaterFagsakStatus = FagsakYtelseTypeRef.Lookup.find(oppdaterFagsakStatuser, behandling.getFagsakYtelseType())
            .orElseThrow(() -> new IllegalStateException("Ingen implementasjoner funnet for ytelse: " + behandling.getFagsakYtelseType().getKode()));
        oppdaterFagsakStatus.oppdaterFagsakNårBehandlingEndret(behandling);
    }
}
