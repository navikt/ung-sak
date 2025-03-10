package no.nav.ung.sak.formidling.template.dto;

import no.nav.ung.sak.formidling.template.dto.felles.PeriodeDto;

public record EndringRapportertInntektDto(
    PeriodeDto periode,
    long rapportertInntekt,
    long utbetalingBeløp,
    int reduksjonssats

) implements TemplateInnholdDto  {
}
