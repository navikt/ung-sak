package no.nav.ung.sak.kontrakt.kontroll;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;

public record InntektspostFraRegisterDto(
    String arbeidsgiverIdentifikator,
    FagsakYtelseType ytelseType,
    Integer inntekt
) {
}
