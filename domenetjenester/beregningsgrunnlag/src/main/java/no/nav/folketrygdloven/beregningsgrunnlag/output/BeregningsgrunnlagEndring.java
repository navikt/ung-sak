package no.nav.folketrygdloven.beregningsgrunnlag.output;

import java.util.List;

public class BeregningsgrunnlagEndring {

    private List<BeregningsgrunnlagPeriodeEndring> beregningsgrunnlagPeriodeEndringer;

    public BeregningsgrunnlagEndring(List<BeregningsgrunnlagPeriodeEndring> beregningsgrunnlagPeriodeEndringer) {
        this.beregningsgrunnlagPeriodeEndringer = beregningsgrunnlagPeriodeEndringer;
    }

    public List<BeregningsgrunnlagPeriodeEndring> getBeregningsgrunnlagPeriodeEndringer() {
        return beregningsgrunnlagPeriodeEndringer;
    }
}
