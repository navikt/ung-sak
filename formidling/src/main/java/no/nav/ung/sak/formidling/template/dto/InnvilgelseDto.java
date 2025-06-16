package no.nav.ung.sak.formidling.template.dto;

import no.nav.ung.sak.formidling.template.dto.innvilgelse.SatsEndringHendelseDto;
import no.nav.ung.sak.formidling.template.dto.innvilgelse.beregning.SatsOgBeregningDto;

import java.time.LocalDate;
import java.util.List;

/**
 * Hoved-DTO for innvilgelsesbrev
 */
public record InnvilgelseDto(
    LocalDate ytelseFom,
    LocalDate ytelseTom,
    long dagsats,
    List<SatsEndringHendelseDto> satsEndringer,
    SatsOgBeregningDto satsOgBeregning,
    String ikkeStøttetBrevTekst,
    boolean etterbetaling,
    boolean ingenSatsEndringHendelser)
    implements TemplateInnholdDto {

}
