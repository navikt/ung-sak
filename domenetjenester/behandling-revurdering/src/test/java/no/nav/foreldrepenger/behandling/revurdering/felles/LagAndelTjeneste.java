package no.nav.foreldrepenger.behandling.revurdering.felles;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;

public interface LagAndelTjeneste {

    public void lagAndeler(BeregningsgrunnlagPeriode periode, boolean medOppjustertDagsat, boolean skalDeleAndelMellomArbeidsgiverOgBruker);

}
