package no.nav.ung.sak.formidling.template.dto;

import no.nav.ung.sak.formidling.innhold.TemplateInnholdDto;

public record ManuellVedtaksbrevDto(
    String tekstHtml
) implements TemplateInnholdDto {
}
