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
@BehandlingStegRef(kode = "MANUELL_VILKÅRSVURDERING")
@BehandlingTypeRef("BT-010")
@ApplicationScoped
public class ManuellVilkårsvurderingSteg implements BeregningsgrunnlagSteg {

    // Dummy-steg

    @Inject
    public ManuellVilkårsvurderingSteg() {
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
