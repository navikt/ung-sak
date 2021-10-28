package no.nav.folketrygdloven.beregningsgrunnlag.resultat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

public class InntektEndring {

    private BigDecimal fraInntekt;
    private BigDecimal tilInntekt;

    public InntektEndring(BigDecimal fraInntekt, BigDecimal tilInntekt) {
        this.fraInntekt = fraInntekt;
        this.tilInntekt = tilInntekt;
    }

    public Optional<BigDecimal> getFraInntekt() {
        return Optional.ofNullable(fraInntekt).map(i -> i.setScale(0, RoundingMode.HALF_UP));
    }

    public BigDecimal getFraMånedsinntekt() {
        return fraInntekt == null ? null : fraInntekt.divide(BigDecimal.valueOf(12), RoundingMode.HALF_UP).setScale(0, RoundingMode.HALF_UP);
    }

    public BigDecimal getTilInntekt() {
        return tilInntekt.setScale(0, RoundingMode.HALF_UP);
    }

    public BigDecimal getTilMånedsinntekt() {
        return tilInntekt == null ? null : tilInntekt.divide(BigDecimal.valueOf(12), RoundingMode.HALF_UP).setScale(0, RoundingMode.HALF_UP);
    }

}
