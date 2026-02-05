package no.nav.ung.sak.formidling.template.dto;

import no.nav.ung.sak.formidling.innhold.TemplateInnholdDto;
import no.nav.ung.sak.formidling.template.dto.endring.programperiode.EndretSluttDato;
import no.nav.ung.sak.formidling.template.dto.endring.programperiode.EndretStartDato;

public record EndringProgramPeriodeDto(
    EndretStartDato endretStartdato,
    EndretSluttDato endretSluttdato,
    boolean muligTilbakekreving
) implements TemplateInnholdDto {
}
