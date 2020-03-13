package no.nav.k9.sak.domene.behandling.steg.faresignaler;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.RisikovurderingTjeneste;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;

import static java.util.Collections.singletonList;

public abstract class VurderFaresignalerStegFelles implements BehandlingSteg {

    private RisikovurderingTjeneste risikovurderingTjeneste;

    protected VurderFaresignalerStegFelles() {
        // for CDI proxy
    }

    public VurderFaresignalerStegFelles(RisikovurderingTjeneste risikovurderingTjeneste) {
        this.risikovurderingTjeneste = risikovurderingTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {

        if (risikovurderingTjeneste.skalVurdereFaresignaler(kontekst.getBehandlingId())) {
            return BehandleStegResultat.utførtMedAksjonspunkter(singletonList(AksjonspunktDefinisjon.VURDER_FARESIGNALER));
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
