package no.nav.ung.sak.formidling.template.dto;

import no.nav.ung.sak.formidling.innhold.TemplateInnholdDto;

public record KlageOversendtDto(
    String ettersendelsesenhet,
    String fritekst
) implements TemplateInnholdDto {
}
