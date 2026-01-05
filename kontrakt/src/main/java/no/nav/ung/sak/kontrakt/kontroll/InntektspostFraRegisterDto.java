package no.nav.ung.sak.kontrakt.kontroll;

import no.nav.ung.kodeverk.arbeidsforhold.OverordnetYtelseType;

public record InntektspostFraRegisterDto(
    String arbeidsgiverIdentifikator,
    OverordnetYtelseType ytelseType,
    Integer inntekt
) {
}
