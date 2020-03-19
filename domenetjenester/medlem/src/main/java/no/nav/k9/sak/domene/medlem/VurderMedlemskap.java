package no.nav.k9.sak.domene.medlem;

import java.util.Set;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.medlem.VurderingsÅrsak;

public class VurderMedlemskap {

    
    private Set<AksjonspunktDefinisjon> aksjonspunkter;
    private Set<VurderingsÅrsak> årsaker;

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
