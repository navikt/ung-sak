package no.nav.ung.sak.formidling.template.dto;

import no.nav.ung.sak.formidling.template.dto.endring.EndringRapportertInntektDto;

/**
 * Hoved-DTO for endringsbrev
 */
public record EndringDto(
    EndringRapportertInntektDto rapportertInntekt) implements TemplateInnholdDto {

}
