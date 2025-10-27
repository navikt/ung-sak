package no.nav.ung.sak.kontroll;

import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;

import java.math.BigDecimal;

public record RapportertInntektOgKilde(KontrollertInntektKilde kilde, BigDecimal samletInntekt) {
}
