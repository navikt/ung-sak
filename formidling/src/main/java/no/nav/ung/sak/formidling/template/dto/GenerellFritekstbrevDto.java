package no.nav.ung.sak.formidling.template.dto;

public record GenerellFritekstbrevDto(
    String overskrift,
    String brødtekst
) implements TemplateInnholdDto {
}
