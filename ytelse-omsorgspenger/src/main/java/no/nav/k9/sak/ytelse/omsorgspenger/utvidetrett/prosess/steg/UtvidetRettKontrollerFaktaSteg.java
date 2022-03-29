package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess.steg;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.KONTROLLER_FAKTA;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;

@BehandlingStegRef(stegtype = KONTROLLER_FAKTA)
@FagsakYtelseTypeRef("OMP_KS")
@FagsakYtelseTypeRef("OMP_MA")
@FagsakYtelseTypeRef("OMP_AO")
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

