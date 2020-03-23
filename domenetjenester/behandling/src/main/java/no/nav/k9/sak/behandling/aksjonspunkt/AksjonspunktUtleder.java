package no.nav.k9.sak.behandling.aksjonspunkt;

import java.util.List;

import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;

public interface AksjonspunktUtleder {

    List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param);
}
