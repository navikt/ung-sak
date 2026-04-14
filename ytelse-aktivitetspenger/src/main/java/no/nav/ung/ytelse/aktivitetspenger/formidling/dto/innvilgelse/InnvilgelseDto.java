package no.nav.ung.ytelse.aktivitetspenger.formidling.dto.innvilgelse;

import no.nav.ung.sak.formidling.innhold.TemplateInnholdDto;
import no.nav.ung.sak.formidling.vedtak.satsendring.SatsEndringHendelseDto;

import java.time.LocalDate;
import java.util.List;

public record InnvilgelseDto(
    LocalDate ytelseFom,
    LocalDate ytelseTom,
    long dagsats,
    UtbetalingDto utbetaling,
    List<SatsEndringHendelseDto> satsEndringer,
    SatsOgBeregningDto satsOgBeregning
) implements TemplateInnholdDto {
}
