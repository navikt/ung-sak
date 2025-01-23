package no.nav.ung.sak.formidling.template.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import no.nav.ung.sak.formidling.template.dto.felles.FellesDto;

/**
 * Hoved-dto for alle brev
 *
 * @param felles
 * @param templateDataDto
 */
public record TemplateDto(
    FellesDto felles,
    //inliner feltene fordi det er enklere å jobbe med i templatefiler
    @JsonUnwrapped TemplateInnholdDto templateDataDto
    )
{
}
