package no.nav.ung.sak.kontrakt.kontroll;

import no.nav.ung.kodeverk.arbeidsforhold.OverordnetInntektYtelseType;

public record InntektspostFraRegisterDto(
    String arbeidsgiverIdentifikator,
    OverordnetInntektYtelseType ytelseType,
    Integer inntekt
) {
}
