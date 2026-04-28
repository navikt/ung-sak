package no.nav.ung.ytelse.aktivitetspenger.formidling.dto.endring.inntekt;

import no.nav.ung.sak.formidling.template.dto.felles.PeriodeDto;

import java.time.Month;

public record EndringInntektPeriodeDto(
    PeriodeDto periode,
    long inntekt,
    long utbetalingBeløp,
    Month ufullstendigMåned) {
}

