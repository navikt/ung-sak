package no.nav.k9.sak.ytelse.pleiepengerbarn.tilsyn;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandlingskontroll.*;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;

@ApplicationScoped
@BehandlingStegRef(kode = "VURDER_TILSYN")
@BehandlingTypeRef
@FagsakYtelseTypeRef("PSB")
public class VurderTilsynSteg implements BehandlingSteg {

    VurderTilsynSteg() {
        // for proxy
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var aksjonspunkter = new ArrayList<AksjonspunktDefinisjon>();
        if (søktOmNattevåk()) {
            aksjonspunkter.add(AksjonspunktDefinisjon.VURDER_NATTEVÅK);
        }
        if (søktOmBeredskap()) {
            aksjonspunkter.add(AksjonspunktDefinisjon.VURDER_BEREDSKAP);
        }
        if (aksjonspunkter.isEmpty()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunkter);
    }

    private boolean søktOmNattevåk() {
        //TODO: implementert dette
        return false;
    }


    private boolean søktOmBeredskap() {
        //TODO: implementert dette
        return false;
    }



}
