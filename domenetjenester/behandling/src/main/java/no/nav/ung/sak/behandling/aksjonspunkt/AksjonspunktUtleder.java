package no.nav.ung.sak.behandling.aksjonspunkt;

import java.util.List;

import no.nav.ung.sak.behandlingskontroll.AksjonspunktResultat;

public interface AksjonspunktUtleder {

    List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param);
}
