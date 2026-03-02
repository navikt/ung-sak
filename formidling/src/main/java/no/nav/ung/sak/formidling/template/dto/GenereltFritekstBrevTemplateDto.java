package no.nav.ung.sak.formidling.template.dto;

import no.nav.ung.sak.formidling.innhold.TemplateInnholdDto;

public record GenereltFritekstBrevTemplateDto(
    String overskrift,
    String brødtekst
) implements TemplateInnholdDto {
}
