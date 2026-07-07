package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.dto;

import no.nav.ung.sak.formidling.innhold.TemplateInnholdDto;

import java.time.LocalDate;

public record OpphørOpphevetDto(
    LocalDate tidligereSluttdato,
    LocalDate maksdato) implements TemplateInnholdDto {
}
