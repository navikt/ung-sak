package no.nav.ung.sak.formidling.template.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import no.nav.ung.sak.formidling.template.dto.innvilgelse.GbeløpPeriodeDto;
import no.nav.ung.sak.formidling.template.dto.innvilgelse.ResultatFlaggDto;
import no.nav.ung.sak.formidling.template.dto.innvilgelse.SatserDto;
import no.nav.ung.sak.formidling.template.dto.innvilgelse.TilkjentPeriodeDto;

/**
 * Hoved-DTO for innvilgelsesbrev
 */
public record InnvilgelseDto(
    ResultatFlaggDto resultat,
    LocalDate ytelseFom,
    long antallDager,
    List<TilkjentPeriodeDto> tilkjentePerioder,
    Set<GbeløpPeriodeDto> gbeløpPerioder,
    SatserDto satser) implements TemplateInnholdDto {

}
