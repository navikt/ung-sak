package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.util.List;

import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;

public interface PreconditionBeregningAksjonspunktUtleder {

    List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param);

}
