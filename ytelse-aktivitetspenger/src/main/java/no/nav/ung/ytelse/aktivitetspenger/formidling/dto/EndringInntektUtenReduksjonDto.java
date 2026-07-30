package no.nav.ung.ytelse.aktivitetspenger.formidling.dto;

import no.nav.ung.sak.formidling.innhold.TemplateInnholdDto;
import no.nav.ung.sak.formidling.template.dto.felles.PeriodeDto;

import java.util.Set;

public record EndringInntektUtenReduksjonDto(Set<PeriodeDto> perioder)
    implements TemplateInnholdDto {
}

