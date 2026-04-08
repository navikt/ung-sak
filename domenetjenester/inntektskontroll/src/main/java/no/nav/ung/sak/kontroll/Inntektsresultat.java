package no.nav.ung.sak.kontroll;

import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record Inntektsresultat(Set<RapportertInntekt> inntekter, KontrollertInntektKilde kilde) {
}
