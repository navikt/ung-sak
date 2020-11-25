package no.nav.k9.sak.ytelse.unntaksbehandling.steg;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.behandling.steg.kompletthet.VurderKompletthetSteg;

@BehandlingStegRef(kode = "VURDERKOMPLETT")
@BehandlingTypeRef("BT-010")
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderKompletthetUnntaksbehandlingSteg implements VurderKompletthetSteg {


    // Dummy-steg - for å støtte kompletthettsjekker

    @Inject
    public VurderKompletthetUnntaksbehandlingSteg() {
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
