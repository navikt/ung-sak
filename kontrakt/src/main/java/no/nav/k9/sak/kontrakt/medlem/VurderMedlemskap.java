package no.nav.k9.sak.kontrakt.medlem;

import java.util.Set;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;

public class VurderMedlemskap {

    private final Set<AksjonspunktDefinisjon> aksjonspunkter;
    private final Set<VurderingsÅrsak> årsaker;

    public VurderMedlemskap(Set<AksjonspunktDefinisjon> aksjonspunkter, Set<VurderingsÅrsak> årsaker) {
        this.aksjonspunkter = aksjonspunkter;
        this.årsaker = årsaker;
    }

    public Set<AksjonspunktDefinisjon> getAksjonspunkter() {
        return aksjonspunkter;
    }

    public Set<VurderingsÅrsak> getÅrsaker() {
        return årsaker;
    }
}
