package no.nav.k9.sak.ytelse.beregning.regler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Objects;

import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrStatus;

public class UtbetalingsgradOppdragBeregner {

    private final BigDecimal reduksjonsfaktorInaktivTypeA;
    private final BigDecimal bruttoBeregningsgrunnlag;
    private final BigDecimal brukersAndelPrÅr; //HAXX utnytter at denne inkluderer naturalytelser. TODO bedre å ta inn naturalytelser spesifikt og legge til bruttoBeregningsgrunnlag

    public UtbetalingsgradOppdragBeregner(BigDecimal reduksjonsfaktorInaktivTypeA, BigDecimal bruttoBeregningsgrunnlag, Collection<BeregningsgrunnlagPrStatus> beregningsgrunnlagPrStatus) {
        this.reduksjonsfaktorInaktivTypeA = reduksjonsfaktorInaktivTypeA;
        this.bruttoBeregningsgrunnlag = bruttoBeregningsgrunnlag;
        this.brukersAndelPrÅr = beregningsgrunnlagPrStatus.stream()
            .filter(it->it.getArbeidsforhold() != null)
            .flatMap(it->it.getArbeidsforhold().stream())
            .map(BeregningsgrunnlagPrArbeidsforhold::getRedusertBrukersAndelPrÅr)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal finnDagsatsfaktorFraUtbetalingsgrad(BigDecimal utbetalingsgradOppdrag, BigDecimal dagsats) {
        return utbetalingsgradOppdrag.divide(dagsats, 10,  RoundingMode.HALF_UP);
    }

    public BigDecimal beregnUtbetalingsgradOppdrag(BigDecimal årssats) {
        boolean erMidlertidigInaktivTypeA = reduksjonsfaktorInaktivTypeA != null;
        BigDecimal grunnlagInklBortfaltNaturalytelse = bruttoBeregningsgrunnlag.compareTo(brukersAndelPrÅr) >= 0 ? bruttoBeregningsgrunnlag : brukersAndelPrÅr;
        BigDecimal maksimalUtbetaling = erMidlertidigInaktivTypeA
            ? grunnlagInklBortfaltNaturalytelse.multiply(reduksjonsfaktorInaktivTypeA).setScale(0, RoundingMode.HALF_UP)
            : grunnlagInklBortfaltNaturalytelse;
        return prosentAvMaksimal(årssats, maksimalUtbetaling);
    }

    private static BigDecimal prosentAvMaksimal(BigDecimal input, BigDecimal maks) {
        if (maks.signum() == 0) {
            return java.math.BigDecimal.ZERO;
        }
        return java.math.BigDecimal.valueOf(100).multiply(input).divide(maks, 2, RoundingMode.HALF_UP);
    }

}
