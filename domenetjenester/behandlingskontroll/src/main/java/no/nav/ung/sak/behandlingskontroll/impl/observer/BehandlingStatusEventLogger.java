package no.nav.ung.sak.behandlingskontroll.impl.observer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingStatusEvent;

@ApplicationScoped
public class BehandlingStatusEventLogger {
    private static final Logger log = LoggerFactory.getLogger(BehandlingStatusEventLogger.class);

    BehandlingStatusEventLogger() {
        // for CDI
    }

    public void loggBehandlingStatusEndring(@Observes BehandlingStatusEvent event) {
        Long behandlingId = event.getBehandlingId();
        Long fagsakId = event.getKontekst().getFagsakId();

        BehandlingStatus nyStatus = event.getNyStatus();
        String kode = nyStatus==null?null:nyStatus.getKode();
        log.info("Behandling status oppdatert; behandlingId [{}]; fagsakId [{}]; status [{}]]", behandlingId, fagsakId, kode);
    }
}
