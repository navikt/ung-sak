package no.nav.ung.sak.formidling.template.dto;

public record KlageOversendtDto(
    String ettersendelsesenhet,
    String fritekst
) implements TemplateInnholdDto {
}
