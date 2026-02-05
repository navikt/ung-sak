package no.nav.ung.sak.formidling.template.dto;

import no.nav.ung.sak.formidling.innhold.TemplateInnholdDto;

import java.time.LocalDate;

public record EndringBarnetilleggDto(
    LocalDate fom,
    long barnetillegg,
    long sats) implements TemplateInnholdDto {
}
