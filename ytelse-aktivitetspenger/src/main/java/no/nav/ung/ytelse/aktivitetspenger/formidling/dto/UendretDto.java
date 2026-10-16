package no.nav.ung.ytelse.aktivitetspenger.formidling.dto;

import no.nav.ung.sak.formidling.innhold.TemplateInnholdDto;

import java.time.LocalDate;

public record UendretDto(
    LocalDate maksdato
) implements TemplateInnholdDto { }
