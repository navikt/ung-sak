package no.nav.ung.sak.formidling.template.dto.endring.inntekt;

import no.nav.ung.sak.formidling.template.dto.felles.PeriodeDto;

public record EndringInntektPeriodeDto(
    PeriodeDto periode,
    long inntekt,
    long utbetalingBeløp
) {
}
