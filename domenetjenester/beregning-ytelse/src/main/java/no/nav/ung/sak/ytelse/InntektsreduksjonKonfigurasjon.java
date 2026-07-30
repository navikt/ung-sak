package no.nav.ung.sak.ytelse;

import java.math.BigDecimal;

public record InntektsreduksjonKonfigurasjon(
    BigDecimal reduksjonsfaktorArbeidsinntekt,
    BigDecimal reduksjonsfaktorYtelse) {
}

