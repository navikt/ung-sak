package no.nav.ung.sak.formidling.template.dto;

import no.nav.ung.sak.formidling.innhold.TemplateInnholdDto;

public record KlageMedholdDto(
    Boolean delvisMedhold,
    String fritekst,
    Boolean klagerett
) implements TemplateInnholdDto {
}
