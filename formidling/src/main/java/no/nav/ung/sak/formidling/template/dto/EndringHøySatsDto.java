package no.nav.ung.sak.formidling.template.dto;

import java.time.LocalDate;

public record EndringHøySatsDto(
    LocalDate fom,
    long nyDagsats,
    int aldersgrense
) implements TemplateInnholdDto {
}
