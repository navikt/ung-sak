package no.nav.ung.sak.formidling.template.dto;

import no.nav.ung.sak.formidling.template.dto.innvilgelse.*;
import no.nav.ung.sak.formidling.template.dto.innvilgelse.beregning.SatsOgBeregningDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Hoved-DTO for innvilgelsesbrev
 */
public record InnvilgelseDto(
    ResultatFlaggDto resultat,
    LocalDate ytelseFom,
    LocalDate ytelseTom,
    long dagsats,
    @Deprecated
    List<TilkjentPeriodeDto> tilkjentePerioder,
    @Deprecated
    Set<GbeløpPeriodeDto> gbeløpPerioder,
    SatserDto satser,
    TilkjentPeriodeDto tilkjentPeriode,
    TilkjentPeriodeDto tilkjentPeriodeHøy,
    String ikkeStøttetBrevTekst,
    List<SatsEndringHendelseDto> satsEndringer,
    SatsOgBeregningDto satsOgBeregning)
    implements TemplateInnholdDto {

}
