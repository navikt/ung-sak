package no.nav.ung.ytelse.aktivitetspenger.formidling.dto;

import no.nav.ung.sak.formidling.innhold.TemplateInnholdDto;
import no.nav.ung.sak.typer.Periode;

public record EndringAvslagDto(
    Periode periode,
    KildeTilOpplysninger kilde,
    AvslåttBosted bosted,
    AvslåttAndreLivsoppholdsytelser andreLivsoppholdsytelser
) implements TemplateInnholdDto { }
