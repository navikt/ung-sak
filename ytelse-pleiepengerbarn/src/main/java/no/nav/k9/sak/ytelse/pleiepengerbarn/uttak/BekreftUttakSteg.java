package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;

@ApplicationScoped
@BehandlingStegRef(kode = "BEKREFT_UTTAK")
@BehandlingTypeRef
@FagsakYtelseTypeRef("PSB")
@FagsakYtelseTypeRef("PPN")
public class BekreftUttakSteg implements BehandlingSteg {


    BekreftUttakSteg() {
        // CDI
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        // TO BE REMOVED
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
