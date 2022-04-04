package no.nav.k9.sak.ytelse.frisinn.beregnytelse;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.PRECONDITION_BEREGNING;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.FRISINN;

import jakarta.enterprise.context.ApplicationScoped;

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
@FagsakYtelseTypeRef(FRISINN)
@BehandlingStegRef(stegtype = PRECONDITION_BEREGNING)
@BehandlingTypeRef
@ApplicationScoped
public class FrisinnVurderPreconditionBeregningSteg implements BeregningsgrunnlagSteg {

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
