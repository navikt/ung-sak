package no.nav.ung.sak.formidling.template.dto;

/**
 * Marker for template innmat dto'er
 */
public sealed interface TemplateInnholdDto permits EndringHøySatsDto, EndringRapportertInntektDto, ManuellVedtaksbrevDto, InnvilgelseDto {
}
