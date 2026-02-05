package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.dto;

import no.nav.ung.sak.formidling.innhold.TemplateInnholdDto;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.dto.endring.inntekt.EndringInntektPeriodeDto;

import java.util.List;

public record EndringInntektReduksjonDto(
    int reduksjonssats,
    List<EndringInntektPeriodeDto> utbetalingsperioder,
    List<EndringInntektPeriodeDto> ingenUtbetalingPerioder,
    boolean harFlereUtbetalinger,
    boolean harIngenUtbetalinger) implements TemplateInnholdDto {
}
