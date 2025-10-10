package no.nav.ung.sak.formidling.template.dto;

public record KlageMedholdDto(
    Boolean delvisMedhold,
    String fritekst,
    Boolean klagerett
) implements TemplateInnholdDto {
}
