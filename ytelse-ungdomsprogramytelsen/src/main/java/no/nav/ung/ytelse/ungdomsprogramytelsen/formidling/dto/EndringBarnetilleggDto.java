package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.dto;

import no.nav.ung.sak.formidling.innhold.TemplateInnholdDto;

import java.time.LocalDate;

public record EndringBarnetilleggDto(
    LocalDate fom,
    LocalDate fødselsdato,
    long barnetillegg,
    long sats,
    boolean barnFødtFørStartdato) implements TemplateInnholdDto {
}
