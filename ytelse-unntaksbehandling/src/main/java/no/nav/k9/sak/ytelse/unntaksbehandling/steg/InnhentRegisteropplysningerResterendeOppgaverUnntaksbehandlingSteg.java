package no.nav.k9.sak.ytelse.unntaksbehandling.steg;


import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;


@BehandlingStegRef(kode = "INREG_AVSL")
@BehandlingTypeRef("BT-010")
@FagsakYtelseTypeRef
@ApplicationScoped
public class InnhentRegisteropplysningerResterendeOppgaverUnntaksbehandlingSteg implements BehandlingSteg {

    // Dummy-steg - for å støtte Kompletthettsjekker

    @Inject
    public InnhentRegisteropplysningerResterendeOppgaverUnntaksbehandlingSteg() {
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}

