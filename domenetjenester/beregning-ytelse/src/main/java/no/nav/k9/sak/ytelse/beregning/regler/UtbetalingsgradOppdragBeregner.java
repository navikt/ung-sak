package no.nav.k9.sak.ytelse.beregning.regler;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class UtbetalingsgradOppdragBeregner {

    public static final int VIRKEDAGER_I_ET_ÅR = 260;
    private final BigDecimal reduksjonsfaktorInaktivTypeA;
    private final BigDecimal bruttoBeregningsgrunnlag;

    public UtbetalingsgradOppdragBeregner(BigDecimal reduksjonsfaktorInaktivTypeA, BigDecimal bruttoBeregningsgrunnlag) {
        this.reduksjonsfaktorInaktivTypeA = reduksjonsfaktorInaktivTypeA;
        this.bruttoBeregningsgrunnlag = bruttoBeregningsgrunnlag;
    }

    public static BigDecimal finnDagsatsfaktorFraUtbetalingsgrad(BigDecimal utbetalingsgradOppdrag, BigDecimal dagsats) {
        return utbetalingsgradOppdrag.divide(dagsats, 10,  RoundingMode.HALF_UP);
    }

    public BigDecimal beregnUtbetalingsgradOppdrag(BigDecimal årssats) {
        boolean erMidlertidigInaktivTypeA = reduksjonsfaktorInaktivTypeA != null;
        BigDecimal maksimalUtbetaling = erMidlertidigInaktivTypeA
            ? bruttoBeregningsgrunnlag.multiply(reduksjonsfaktorInaktivTypeA).setScale(0, RoundingMode.HALF_UP)
            : bruttoBeregningsgrunnlag;
        return prosentAvMaksimal(årssats, maksimalUtbetaling);
    }

    private static BigDecimal prosentAvMaksimal(BigDecimal input, BigDecimal maks) {
        if (maks.signum() == 0) {
            return java.math.BigDecimal.ZERO;
        }
        return java.math.BigDecimal.valueOf(100).multiply(input).divide(maks, 2, RoundingMode.HALF_UP);
    }

}
