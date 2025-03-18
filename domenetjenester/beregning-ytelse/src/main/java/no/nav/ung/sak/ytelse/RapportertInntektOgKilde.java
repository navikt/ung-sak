package no.nav.ung.sak.ytelse;

import no.nav.ung.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;

import java.util.Set;

public record RapportertInntektOgKilde(KontrollertInntektKilde kilde, Set<RapportertInntekt> rapporterteInntekter) {
}
