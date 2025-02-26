package no.nav.ung.sak.formidling.template.dto.endring;

import no.nav.ung.sak.formidling.template.dto.felles.PeriodeDto;

public record EndringRapportertInntektDto(
    PeriodeDto periode,
    long rapportertInntekt,
    long utbetalingBeløp,
    int reduksjonssats,
    long reduksjonBeløp,
    long dagsats,
    long redusertDagsats

) {
}
