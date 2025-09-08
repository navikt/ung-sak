package no.nav.ung.sak.formidling.template.dto;

import no.nav.ung.sak.formidling.template.dto.endring.inntekt.EndringRapportertInntektPeriodeDto;
import no.nav.ung.sak.formidling.template.dto.felles.PeriodeDto;

import java.util.List;

public record EndringRapportertInntektDto(
    PeriodeDto totalPeriode,
    long totalRapportertInntekt,
    long totalUtbetalingBeløp,
    int reduksjonssats,
    boolean harRapportertFlerePerioder, //bort
    List<EndringRapportertInntektPeriodeDto> perioder
    ) implements TemplateInnholdDto {
}
