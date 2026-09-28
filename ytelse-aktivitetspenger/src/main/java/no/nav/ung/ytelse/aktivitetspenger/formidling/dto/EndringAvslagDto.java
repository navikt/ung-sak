package no.nav.ung.ytelse.aktivitetspenger.formidling.dto;

import no.nav.ung.sak.formidling.innhold.TemplateInnholdDto;

public record EndringAvslagDto(
    AvslåttBosted bosted,
    AvslåttBistand bistand,
    AvslåttAndreLivsoppholdsytelser andreLivsoppholdsytelser
) implements TemplateInnholdDto { }
