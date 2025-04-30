package no.nav.ung.sak.formidling.template.dto;

import no.nav.ung.sak.formidling.template.dto.innvilgelse.GbeløpPeriodeDto;
import no.nav.ung.sak.formidling.template.dto.innvilgelse.ResultatFlaggDto;
import no.nav.ung.sak.formidling.template.dto.innvilgelse.SatserDto;
import no.nav.ung.sak.formidling.template.dto.innvilgelse.TilkjentPeriodeDto;

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
    @Deprecated
    List<TilkjentPeriodeDto> tilkjentePerioder,
    @Deprecated
    Set<GbeløpPeriodeDto> gbeløpPerioder,
    SatserDto satser,
    TilkjentPeriodeDto tilkjentPeriode,
    TilkjentPeriodeDto tilkjentPeriodeHøy
) implements TemplateInnholdDto {

}
