package no.nav.ung.sak.formidling.template.dto;

import java.time.LocalDate;

public record EndringBarnetilleggDto(
    LocalDate fom,
    long barnetillegg,
    long sats) implements TemplateInnholdDto {
}
