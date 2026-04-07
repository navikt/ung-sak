package no.nav.ung.sak.behandlingslager.tilkjentytelse;

import java.math.BigDecimal;

public record KontrollerteInntekter(
    BigDecimal inntekt,
    BigDecimal ytelse
) {
    public BigDecimal inntekt() {
        return inntekt != null ? inntekt : BigDecimal.ZERO;
    }

    public BigDecimal arbeidsinntekt() {
        return inntekt();
    }

    public BigDecimal ytelse() {
        return ytelse != null ? ytelse : BigDecimal.ZERO;
    }
}
