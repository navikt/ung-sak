package no.nav.ung.sak.formidling.template.dto;

public record KlageMedholdDto(
    Boolean delvisMedhold,
    Boolean klagerett
) implements TemplateInnholdDto {
}
