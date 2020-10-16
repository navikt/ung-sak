package no.nav.k9.sak.ytelse.unntaksbehandling.steg;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagSteg;

@FagsakYtelseTypeRef
@BehandlingStegRef(kode = "KOFAK")
@BehandlingTypeRef("BT-010")
@ApplicationScoped
public class KontrollerFaktaUnntaksbehandlingSteg implements BeregningsgrunnlagSteg {

    // Dummy-steg - for å støtte tilbakehopp ved registeroppdateringer

    @Inject
    public KontrollerFaktaUnntaksbehandlingSteg() {
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
