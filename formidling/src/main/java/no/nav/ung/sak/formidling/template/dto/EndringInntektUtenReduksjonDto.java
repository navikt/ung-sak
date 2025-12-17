package no.nav.ung.sak.formidling.template.dto;

import no.nav.ung.sak.formidling.template.dto.felles.PeriodeDto;

import java.util.Set;

public record EndringInntektUtenReduksjonDto(Set<PeriodeDto> perioder)
    implements TemplateInnholdDto {
}
