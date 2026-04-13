package no.nav.ung.ytelse.aktivitetspenger.formidling.dto.innvilgelse;

import no.nav.ung.sak.formidling.innhold.TemplateInnholdDto;

import java.time.LocalDate;

public record InnvilgelseDto(
    LocalDate ytelseFom,
    LocalDate ytelseTom,
    SatsOgBeregningDto satsOgBeregning
) implements TemplateInnholdDto {
}
