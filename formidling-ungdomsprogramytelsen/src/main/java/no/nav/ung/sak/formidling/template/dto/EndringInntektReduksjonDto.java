package no.nav.ung.sak.formidling.template.dto;

import no.nav.ung.sak.formidling.innhold.TemplateInnholdDto;
import no.nav.ung.sak.formidling.template.dto.endring.inntekt.EndringInntektPeriodeDto;

import java.util.List;

public record EndringInntektReduksjonDto(
    int reduksjonssats,
    List<EndringInntektPeriodeDto> utbetalingsperioder,
    List<EndringInntektPeriodeDto> ingenUtbetalingPerioder,
    boolean harFlereUtbetalinger,
    boolean harIngenUtbetalinger) implements TemplateInnholdDto {
}
