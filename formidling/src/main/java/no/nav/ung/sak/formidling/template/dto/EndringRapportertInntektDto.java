package no.nav.ung.sak.formidling.template.dto;

import no.nav.ung.sak.formidling.template.dto.endring.inntekt.EndringRapportertInntektPeriodeDto;

import java.util.List;

public record EndringRapportertInntektDto(
    int reduksjonssats,
    List<EndringRapportertInntektPeriodeDto> perioder,
    boolean harKunEnPeriode) implements TemplateInnholdDto {
}
