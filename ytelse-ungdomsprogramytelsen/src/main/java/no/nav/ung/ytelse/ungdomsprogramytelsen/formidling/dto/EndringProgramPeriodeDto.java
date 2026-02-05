package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.dto;

import no.nav.ung.sak.formidling.innhold.TemplateInnholdDto;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.dto.endring.programperiode.EndretSluttDato;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.dto.endring.programperiode.EndretStartDato;

public record EndringProgramPeriodeDto(
    EndretStartDato endretStartdato,
    EndretSluttDato endretSluttdato,
    boolean muligTilbakekreving
) implements TemplateInnholdDto {
}
