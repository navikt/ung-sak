package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import java.math.BigDecimal;

public class BesteBeregningResultat {

    private final BigDecimal årsinntektSisteÅr;
    private final BigDecimal årsinntektSisteTreÅr;
    private final BigDecimal årsinntektBesteBeregning;
    private final String regelSporing;
    private final String regelInput;

    public BesteBeregningResultat(BigDecimal årsinntektSisteÅr, BigDecimal årsinntektSisteTreÅr, BigDecimal årsinntektBesteBeregning, String regelSporing, String regelInput) {
        this.årsinntektSisteÅr = årsinntektSisteÅr;
        this.årsinntektSisteTreÅr = årsinntektSisteTreÅr;
        this.årsinntektBesteBeregning = årsinntektBesteBeregning;
        this.regelSporing = regelSporing;
        this.regelInput = regelInput;
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
