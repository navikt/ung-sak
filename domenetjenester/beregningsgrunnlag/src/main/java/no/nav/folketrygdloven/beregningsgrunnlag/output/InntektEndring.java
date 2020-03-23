package no.nav.folketrygdloven.beregningsgrunnlag.output;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class InntektEndring {

    private BigDecimal fraInntekt;
    private BigDecimal tilInntekt;

    public InntektEndring(BigDecimal fraInntekt, BigDecimal tilInntekt) {
        this.fraInntekt = fraInntekt;
        this.tilInntekt = tilInntekt;
    }

    public BigDecimal getFraInntekt() {
        return fraInntekt;
    }

    public BigDecimal getFraMånedsinntekt() {
        return fraInntekt == null ? null : fraInntekt.divide(BigDecimal.valueOf(12), RoundingMode.HALF_UP).setScale(0, RoundingMode.HALF_UP);
    }

    public BigDecimal getTilInntekt() {
        return tilInntekt;
    }

    public BigDecimal getTilMånedsinntekt() {
        return tilInntekt == null ? null : tilInntekt.divide(BigDecimal.valueOf(12), RoundingMode.HALF_UP).setScale(0, RoundingMode.HALF_UP);
    }

}
