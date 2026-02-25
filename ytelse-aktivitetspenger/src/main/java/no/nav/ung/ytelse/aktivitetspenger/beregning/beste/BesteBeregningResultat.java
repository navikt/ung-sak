package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import java.math.BigDecimal;

public class BesteberegningResultat {

    private final BeregningInput beregningInput;
    private final BigDecimal årsinntektSisteÅr;
    private final BigDecimal årsinntektSisteTreÅr;
    private final BigDecimal årsinntektBesteBeregning;
    private final String regelSporing;
    private final String regelInput;

    public BesteberegningResultat(BeregningInput beregningInput, BigDecimal årsinntektSisteÅr, BigDecimal årsinntektSisteTreÅr, BigDecimal årsinntektBesteBeregning, String regelSporing, String regelInput) {
        this.beregningInput = beregningInput;
        this.årsinntektSisteÅr = årsinntektSisteÅr;
        this.årsinntektSisteTreÅr = årsinntektSisteTreÅr;
        this.årsinntektBesteBeregning = årsinntektBesteBeregning;
        this.regelSporing = regelSporing;
        this.regelInput = regelInput;
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

    public BigDecimal getÅrsinntektBesteBeregning() {
        return årsinntektBesteBeregning;
    }

    public String getRegelSporing() {
        return regelSporing;
    }

    public String getRegelInput() {
        return regelInput;
    }
}
