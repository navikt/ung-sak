package no.nav.ung.sak.formidling.template.dto;

import no.nav.ung.sak.formidling.template.dto.endring.inntekt.EndringRapportertInntektPeriodeDto;

import java.util.List;

public record EndringRapportertInntektDto(
    int reduksjonssats,
    List<EndringRapportertInntektPeriodeDto> utbetalingsperioder,
    List<EndringRapportertInntektPeriodeDto> ingenUtbetalingPerioder,
    boolean harFlereUtbetalinger,
    boolean harIngenUtbetalinger) implements TemplateInnholdDto {
}
