package no.nav.ung.sak.kontroll;

import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;

import java.util.Set;

public record Inntektsresultat(Set<RapportertInntekt> inntekter, KontrollertInntektKilde kilde) {
}
