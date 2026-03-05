package no.nav.ung.ytelse.aktivitetspenger.formidling.dto;

import no.nav.ung.sak.formidling.innhold.TemplateInnholdDto;

import java.time.LocalDate;

/**
 * Hoved-DTO for innvilgelsesbrev
 */
public record InnvilgelseDto(
    LocalDate ytelseFom,
    LocalDate ytelseTom)
    implements TemplateInnholdDto {

}
