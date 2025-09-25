package no.nav.ung.sak.formidling.template.dto.endring.inntekt;

import no.nav.ung.sak.formidling.template.dto.felles.PeriodeDto;

public record EndringRapportertInntektPeriodeDto(
    PeriodeDto periode,
    long rapportertInntekt,
    long utbetalingBeløp
) {
}
