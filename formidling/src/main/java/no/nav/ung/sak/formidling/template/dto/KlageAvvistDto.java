package no.nav.ung.sak.formidling.template.dto;

public record KlageAvvistDto(
    String avdeling,
    Boolean flereÅrsaker,
    Boolean aarsakKlagetForSent,
    Boolean aarsakManglerUnderskrift,
    Boolean aarsakIkkeEtVedtak,
    Boolean aarsakVedtakGjelderIkkePart,
    Boolean aarsakManglerKlagegrunn,
    String hjemler
) implements TemplateInnholdDto {
}
