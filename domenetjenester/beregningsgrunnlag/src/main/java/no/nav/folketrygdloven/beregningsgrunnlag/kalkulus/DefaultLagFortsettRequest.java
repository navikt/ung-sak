package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.util.Collection;

import no.nav.folketrygdloven.beregningsgrunnlag.BgRef;
import no.nav.folketrygdloven.kalkulus.kodeverk.StegType;
import no.nav.folketrygdloven.kalkulus.kodeverk.YtelseTyperKalkulusStøtterKontrakt;
import no.nav.folketrygdloven.kalkulus.request.v1.FortsettBeregningListeRequest;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandling.BehandlingReferanse;


class DefaultLagFortsettRequest implements LagFortsettRequest {

    @Override
    public FortsettBeregningListeRequest lagRequest(BehandlingReferanse referanse, Collection<BgRef> bgReferanser, BehandlingStegType stegType) {
        var bgRefs = BgRef.getRefs(bgReferanser);
        var ytelseType = YtelseTyperKalkulusStøtterKontrakt.fraKode(referanse.getFagsakYtelseType().getKode());
        return new FortsettBeregningListeRequest(
            referanse.getSaksnummer().getVerdi(),
            bgRefs,
            ytelseType,
            new StegType(stegType.getKode()));
    }

}
