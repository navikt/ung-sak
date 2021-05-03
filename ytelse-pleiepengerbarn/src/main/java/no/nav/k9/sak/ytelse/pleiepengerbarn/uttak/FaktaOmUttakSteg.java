package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;

import java.util.ArrayList;

@ApplicationScoped
@BehandlingStegRef(kode = "KOFAKUT")
@BehandlingTypeRef
@FagsakYtelseTypeRef("PSB")
public class FaktaOmUttakSteg implements BehandlingSteg {

    protected FaktaOmUttakSteg() {
        // for proxy
    }

    @SuppressWarnings("unused")
    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();

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
