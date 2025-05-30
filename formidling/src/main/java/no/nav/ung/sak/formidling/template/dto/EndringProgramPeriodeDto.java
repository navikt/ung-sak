package no.nav.ung.sak.formidling.template.dto;

import no.nav.ung.sak.formidling.template.dto.endring.programperiode.EndretDatoDto;

public record EndringProgramPeriodeDto(
    EndretDatoDto endretSluttdato,
    EndretDatoDto endretStartdato,
    boolean muligTilbakekreving
) implements TemplateInnholdDto {
}
