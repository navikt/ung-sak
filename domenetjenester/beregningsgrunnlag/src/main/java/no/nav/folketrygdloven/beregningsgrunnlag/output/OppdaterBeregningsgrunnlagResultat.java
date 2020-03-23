package no.nav.folketrygdloven.beregningsgrunnlag.output;

import java.util.Optional;

public class OppdaterBeregningsgrunnlagResultat {

    private BeregningsgrunnlagEndring beregningsgrunnlagEndring;
    private FaktaOmBeregningVurderinger faktaOmBeregningVurderinger;

    public OppdaterBeregningsgrunnlagResultat(BeregningsgrunnlagEndring beregningsgrunnlagEndring, FaktaOmBeregningVurderinger faktaOmBeregningVurderinger) {
        this.beregningsgrunnlagEndring = beregningsgrunnlagEndring;
        this.faktaOmBeregningVurderinger = faktaOmBeregningVurderinger;
    }

    public Optional<BeregningsgrunnlagEndring> getBeregningsgrunnlagEndring() {
        return Optional.ofNullable(beregningsgrunnlagEndring);
    }

    public Optional<FaktaOmBeregningVurderinger> getFaktaOmBeregningVurderinger() {
        return Optional.ofNullable(faktaOmBeregningVurderinger);
    }
}
