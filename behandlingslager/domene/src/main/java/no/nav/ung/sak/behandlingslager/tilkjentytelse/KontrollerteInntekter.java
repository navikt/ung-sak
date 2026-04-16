package no.nav.ung.sak.behandlingslager.tilkjentytelse;

import java.math.BigDecimal;

public record KontrollerteInntekter(
    BigDecimal inntekt,
    BigDecimal ytelse
) {
    public BigDecimal arbeidsinntekt() {
        return inntekt != null ? inntekt : BigDecimal.ZERO;
    }

    public BigDecimal ytelse() {
        return ytelse != null ? ytelse : BigDecimal.ZERO;
    }

    public BigDecimal arbeidsinntektOgYtelse() {
        return arbeidsinntekt().add(ytelse());
    }

}
