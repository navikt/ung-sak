package no.nav.ung.sak.kontrakt.kontroll;

import java.util.List;

public record RegisterinntektDto(
    RapportertInntektDto oppsummertRegister,
    List<InntektspostFraRegisterDto> inntekter
) {
}
