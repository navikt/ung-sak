package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess.steg;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;

@FagsakYtelseTypeRef("OMP_KS")
@FagsakYtelseTypeRef("OMP_MA")
@BehandlingStegRef(kode = "KOFAK")
@BehandlingTypeRef()
@ApplicationScoped
public class UtvidetRettKontrollerFaktaSteg implements BehandlingSteg {

    // Dummy-steg - for å støtte tilbakehopp ved registeroppdateringer

    @Inject
    public UtvidetRettKontrollerFaktaSteg() {
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
