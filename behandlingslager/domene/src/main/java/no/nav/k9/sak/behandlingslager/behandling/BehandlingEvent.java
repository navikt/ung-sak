package no.nav.k9.sak.behandlingslager.behandling;

import no.nav.k9.sak.behandlingslager.fagsak.FagsakEvent;

/**
 * Marker interface for events fyrt på en Behandling.
 * Disse fyres ved hjelp av CDI Events.
 */
public interface BehandlingEvent extends FagsakEvent {

    Long getBehandlingId();

}
