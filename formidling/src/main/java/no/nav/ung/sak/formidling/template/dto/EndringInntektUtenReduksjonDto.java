package no.nav.ung.sak.formidling.template.dto;

import no.nav.ung.sak.formidling.template.dto.felles.PeriodeDto;

import java.time.Month;
import java.util.Set;

public record EndringInntektUtenReduksjonDto(Set<PeriodeDto> perioder, Month måned)
    implements TemplateInnholdDto {
}
