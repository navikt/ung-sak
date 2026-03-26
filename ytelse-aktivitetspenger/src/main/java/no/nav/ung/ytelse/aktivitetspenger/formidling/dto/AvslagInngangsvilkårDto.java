package no.nav.ung.ytelse.aktivitetspenger.formidling.dto;

import no.nav.ung.sak.formidling.innhold.TemplateInnholdDto;

import java.time.LocalDate;

public record AvslagInngangsvilkårDto(
    LocalDate fom
) implements TemplateInnholdDto { }
