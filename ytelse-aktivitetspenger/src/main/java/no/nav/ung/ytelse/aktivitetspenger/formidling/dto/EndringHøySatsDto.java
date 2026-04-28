package no.nav.ung.ytelse.aktivitetspenger.formidling.dto;

import no.nav.ung.sak.formidling.innhold.TemplateInnholdDto;

import java.time.LocalDate;

public record EndringHøySatsDto(
    LocalDate fom,
    long nyDagsats,
    int aldersgrense,
    Long totalBarnetillegg) implements TemplateInnholdDto {
}

