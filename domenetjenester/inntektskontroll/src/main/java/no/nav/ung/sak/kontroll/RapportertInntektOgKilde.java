package no.nav.ung.sak.kontroll;

import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;

import java.math.BigDecimal;
import java.util.List;

public record RapportertInntektOgKilde(KontrollertInntektKilde kilde, List<RapportertInntekt> inntekter) {
}
