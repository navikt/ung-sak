package no.nav.k9.sak.domene.behandling.steg.faresignaler;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.risikoklassifisering.tjeneste.RisikovurderingTjeneste;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@BehandlingStegRef(kode = "VURDER_FARESIGNALER")
@BehandlingTypeRef("BT-002")
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderFaresignalerStegImpl extends VurderFaresignalerStegFelles {

    protected VurderFaresignalerStegImpl() {
        // for CDI proxy
    }

    @Inject
    public VurderFaresignalerStegImpl(RisikovurderingTjeneste risikovurderingTjeneste) {
        super(risikovurderingTjeneste);
    }
}
