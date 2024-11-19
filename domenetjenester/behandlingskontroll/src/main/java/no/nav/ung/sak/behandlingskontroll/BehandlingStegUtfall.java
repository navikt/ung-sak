package no.nav.ung.sak.behandlingskontroll;

import no.nav.ung.kodeverk.behandling.BehandlingStegStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;

public class BehandlingStegUtfall {
    private final BehandlingStegType behandlingStegType;
    private final BehandlingStegStatus resultat;

    public BehandlingStegUtfall(BehandlingStegType behandlingStegType, BehandlingStegStatus resultat) {
        this.behandlingStegType = behandlingStegType;
        this.resultat = resultat;
    }

    public BehandlingStegType getBehandlingStegType() {
        return behandlingStegType;
    }

    public BehandlingStegStatus getResultat() {
        return resultat;
    }
}
