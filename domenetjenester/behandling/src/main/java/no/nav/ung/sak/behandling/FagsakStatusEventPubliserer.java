package no.nav.ung.sak.behandling;

import java.util.Objects;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;

@Dependent
public class FagsakStatusEventPubliserer {
    private static final Logger log = LoggerFactory.getLogger(FagsakStatusEventPubliserer.class);

    private Event<FagsakStatusEvent> fagsakStatusEvent;

    FagsakStatusEventPubliserer() {
        // for CDI
    }

    @Inject
    public FagsakStatusEventPubliserer(Event<FagsakStatusEvent> fagsakStatusEvent) {
        this.fagsakStatusEvent = fagsakStatusEvent;
    }

    public void fireEvent(Fagsak fagsak, Behandling behandling, FagsakStatus gammelStatusIn, FagsakStatus nyStatusIn) {
        if ((gammelStatusIn == null && nyStatusIn == null) // NOSONAR
            || Objects.equals(gammelStatusIn, nyStatusIn)) { // NOSONAR
            // gjør ingenting
            return;
        } else if (gammelStatusIn == null && nyStatusIn != null) {// NOSONAR
            log.info("Fagsak status opprettet: id [{}]; type [{}];", fagsak.getId(), fagsak.getYtelseType());
        } else {
            Long fagsakId = fagsak.getId();
            String gammelStatus = gammelStatusIn.getKode(); // NOSONAR false positive NPE dereference
            String nyStatus = nyStatusIn == null ? null : nyStatusIn.getKode();

            if (behandling != null) {
                log.info("Fagsak[{}-{}] status oppdatert: {} -> {}; fagsakId [{}] behandlingId [{}]", fagsak.getSaksnummer(), fagsak.getYtelseType(), gammelStatus, nyStatus, fagsakId, //$NON-NLS-1$
                    behandling.getId());
            } else {
                log.info("Fagsak[{}-{}] status oppdatert: {} -> {}; fagsakId [{}]", fagsak.getSaksnummer(), fagsak.getYtelseType(), gammelStatus, nyStatus, fagsakId); //$NON-NLS-1$
            }
        }

        FagsakStatusEvent event = new FagsakStatusEvent(fagsak.getId(), fagsak.getAktørId(), fagsak.getYtelseType(), gammelStatusIn, nyStatusIn);
        fagsakStatusEvent.fire(event);
    }

    public void fireEvent(Fagsak fagsak, FagsakStatus status) {
        fireEvent(fagsak, null, null, status);
    }
}
