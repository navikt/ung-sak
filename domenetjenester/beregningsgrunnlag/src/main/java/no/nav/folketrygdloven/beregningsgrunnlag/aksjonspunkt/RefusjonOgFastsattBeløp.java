package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt;

import java.math.BigDecimal;

class RefusjonOgFastsattBeløp {

    private BigDecimal totalRefusjonPrÅr;
    private BigDecimal totalFastsattBeløpPrÅr = BigDecimal.ZERO;

    RefusjonOgFastsattBeløp(BigDecimal totalRefusjonPrÅr, BigDecimal totalFastsattBeløpPrÅr) {
        this.totalRefusjonPrÅr = totalRefusjonPrÅr;
        this.totalFastsattBeløpPrÅr = totalFastsattBeløpPrÅr;
    }

    RefusjonOgFastsattBeløp(BigDecimal totalRefusjonPrÅr) {
        this.totalRefusjonPrÅr = totalRefusjonPrÅr;
    }

    BigDecimal getTotalRefusjonPrÅr() {
        return totalRefusjonPrÅr;
    }

    BigDecimal getTotalFastsattBeløpPrÅr() {
        return totalFastsattBeløpPrÅr;
    }

    RefusjonOgFastsattBeløp leggTilRefusjon(BigDecimal refusjon) {
        BigDecimal nyTotalRefusjon = this.totalRefusjonPrÅr.add(refusjon);
        return new RefusjonOgFastsattBeløp(nyTotalRefusjon, this.totalFastsattBeløpPrÅr);
    }

    RefusjonOgFastsattBeløp leggTilFastsattBeløp(BigDecimal fastsattBeløp) {
        BigDecimal nyttTotalFastsattBeløp = this.totalFastsattBeløpPrÅr.add(fastsattBeløp);
        return new RefusjonOgFastsattBeløp(this.totalRefusjonPrÅr, nyttTotalFastsattBeløp);
    }
}
