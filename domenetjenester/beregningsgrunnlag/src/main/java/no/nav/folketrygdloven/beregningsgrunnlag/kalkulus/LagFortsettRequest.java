package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.util.Collection;

import javax.enterprise.inject.Instance;

import no.nav.folketrygdloven.beregningsgrunnlag.BgRef;
import no.nav.folketrygdloven.kalkulus.request.v1.FortsettBeregningListeRequest;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;

public interface LagFortsettRequest {

    static LagFortsettRequest finnTjeneste(Instance<LagFortsettRequest> instances, FagsakYtelseType ytelseType, BehandlingType behandlingType, BehandlingStegType behandlingStegType) {
        return BehandlingStegRef.Lookup.find(LagFortsettRequest.class, instances, ytelseType, behandlingType, behandlingStegType)
            .orElseThrow(() -> new IllegalStateException("Har ikke tjeneste for ytelseType=" + ytelseType + ", behandlingType=" + behandlingType));
    }

    FortsettBeregningListeRequest lagRequest(BehandlingReferanse referanse,
                                             Collection<BgRef> bgReferanser,
                                             BehandlingStegType stegType);


}
