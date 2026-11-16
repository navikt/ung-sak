package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import java.math.BigDecimal;

public class BesteberegningResultat {

    private final BeregningInput beregningInput;
    private final BigDecimal årsinntektSisteÅr;
    private final BigDecimal årsinntektSisteTreÅr;
    private final BigDecimal beregningsgrunnlag;
    private final BigDecimal grunnbeløp;
    private final String regelSporing;

    public BesteberegningResultat(BeregningInput beregningInput, BigDecimal årsinntektSisteÅr, BigDecimal årsinntektSisteTreÅr, BigDecimal beregningsgrunnlag, BigDecimal grunnbeløp, String regelSporing) {
        this.beregningInput = beregningInput;
        this.årsinntektSisteÅr = årsinntektSisteÅr;
        this.årsinntektSisteTreÅr = årsinntektSisteTreÅr;
        this.beregningsgrunnlag = beregningsgrunnlag;
        this.grunnbeløp = grunnbeløp;
        this.regelSporing = regelSporing;
    }

    public BeregningInput getBeregningInput() {
        return beregningInput;
    }

    public BigDecimal getÅrsinntektSisteÅr() {
        return årsinntektSisteÅr;
    }

    public BigDecimal getÅrsinntektSisteTreÅr() {
        return årsinntektSisteTreÅr;
    }

    public BigDecimal getBeregningsgrunnlag() {
        return beregningsgrunnlag;
    }

    public BigDecimal getGrunnbeløp() {
        return grunnbeløp;
    }

    public String getRegelSporing() {
        return regelSporing;
    }
}
