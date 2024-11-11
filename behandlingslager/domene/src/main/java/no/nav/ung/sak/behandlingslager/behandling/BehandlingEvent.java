package no.nav.ung.sak.behandlingslager.behandling;

import no.nav.ung.sak.behandlingslager.fagsak.FagsakEvent;

/**
 * Marker interface for events fyrt på en Behandling.
 * Disse fyres ved hjelp av CDI Events.
 */
public interface BehandlingEvent extends FagsakEvent {

    Long getBehandlingId();

}
