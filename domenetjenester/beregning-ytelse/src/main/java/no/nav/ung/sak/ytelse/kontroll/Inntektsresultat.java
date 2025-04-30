package no.nav.ung.sak.ytelse.kontroll;

import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;

import java.math.BigDecimal;

public record Inntektsresultat(BigDecimal inntekt, KontrollertInntektKilde kilde) {

    public static Inntektsresultat ingenInntektFraBruker() {
        return new Inntektsresultat(BigDecimal.ZERO, KontrollertInntektKilde.BRUKER);
    }
}
