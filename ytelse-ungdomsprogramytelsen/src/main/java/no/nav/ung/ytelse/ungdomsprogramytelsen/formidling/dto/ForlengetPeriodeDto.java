package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.dto;

import no.nav.ung.sak.formidling.innhold.TemplateInnholdDto;

import java.time.LocalDate;

public record ForlengetPeriodeDto(
    LocalDate opprinneligMaksDato,
    LocalDate nyMaksDato) implements TemplateInnholdDto {
}

