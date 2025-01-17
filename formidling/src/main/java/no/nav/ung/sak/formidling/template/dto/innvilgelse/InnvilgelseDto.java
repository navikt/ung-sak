package no.nav.ung.sak.formidling.template.dto.innvilgelse;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import no.nav.ung.sak.formidling.template.TemplateData;
import no.nav.ung.sak.formidling.template.dto.felles.FellesDto;

/**
 * Hoved-DTO for innvilgelsesbrev
 */
public record InnvilgelseDto(
    FellesDto felles,
    ResultatFlaggDto resultat,
    LocalDate ytelseFom,
    long antallDager,
    List<TilkjentPeriodeDto> tilkjentePerioder,
    Set<GbeløpPeriodeDto> gbeløpPerioder,
    SatserDto satser) implements TemplateData {}
