package no.nav.k9.sak.ytelse.frisinn.beregnytelse;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagSteg;

/**
 * Dummysteg for FRISINN.
 *
 * Eksisterer for riktig opprydding av vilkår og beregningsgrunnlag ved kjøring av TilbakeTilStartBeregningTask eller tilbakehopp grunnet re-bekreftelse av 8004.
 *
 *
 */
@FagsakYtelseTypeRef("FRISINN")
@BehandlingStegRef(kode = "PRECONDITION_BERGRUNN")
@BehandlingTypeRef
@ApplicationScoped
public class FrisinnVurderPreconditionBeregningSteg implements BeregningsgrunnlagSteg {

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
