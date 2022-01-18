package no.nav.k9.sak.ytelse.omsorgspenger.filter;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.behandling.steg.beregnytelse.BeregneYtelseSteg;

@FagsakYtelseTypeRef("OMP")
@BehandlingStegRef(kode = "VARIANT_FILTER")
@BehandlingTypeRef
@ApplicationScoped
public class FiltrerUtVariantSomIkkeStøttesSteg implements BeregneYtelseSteg {

    protected FiltrerUtVariantSomIkkeStøttesSteg() {
        // for proxy
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
