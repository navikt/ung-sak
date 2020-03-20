package no.nav.k9.sak.domene.behandling.steg.faresignaler;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.domene.risikoklassifisering.tjeneste.RisikovurderingTjeneste;

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
