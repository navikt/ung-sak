package no.nav.ung.sak.formidling.template.dto;

import java.time.LocalDate;

public record OpphørDto(
    LocalDate opphørsdato,
    String utbetalingsMånedÅr) implements TemplateInnholdDto {
}
