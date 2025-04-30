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
    @Deprecated // erstattes av en tom dato
    long antallDager,
    List<TilkjentPeriodeDto> tilkjentePerioder,
    Set<GbeløpPeriodeDto> gbeløpPerioder,
    SatserDto satser) implements TemplateInnholdDto {

}
