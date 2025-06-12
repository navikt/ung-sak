package no.nav.ung.sak.formidling.template.dto;

public record GenereltFritekstBrevTemplateDto(
    String overskrift,
    String brødtekst
) implements TemplateInnholdDto {
}
