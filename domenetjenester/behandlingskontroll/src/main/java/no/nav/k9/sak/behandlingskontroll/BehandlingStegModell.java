package no.nav.k9.sak.behandlingskontroll;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;

public interface BehandlingStegModell {

    /**
     * Type kode for dette steget.
     */
    BehandlingStegType getBehandlingStegType();

    /**
     * Implementasjon av et gitt steg i behandlingen.
     */
    BehandlingSteg getSteg();

    /**
     * Forventet status når behandling er i steget.
     */
    String getForventetStatus();

    BehandlingModell getBehandlingModell();

}
