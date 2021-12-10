package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.util.Collection;

import no.nav.folketrygdloven.beregningsgrunnlag.BgRef;
import no.nav.folketrygdloven.kalkulus.request.v1.FortsettBeregningListeRequest;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandling.BehandlingReferanse;

public interface LagFortsettRequest {

    FortsettBeregningListeRequest lagRequest(BehandlingReferanse referanse,
                                             Collection<BgRef> bgReferanser,
                                             BehandlingStegType stegType);


}
