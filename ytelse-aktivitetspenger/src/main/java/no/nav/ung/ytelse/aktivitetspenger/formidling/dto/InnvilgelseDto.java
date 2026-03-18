package no.nav.ung.ytelse.aktivitetspenger.formidling.dto;

import no.nav.ung.sak.formidling.innhold.TemplateInnholdDto;
import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.BesteBeregningResultatType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;

public record InnvilgelseDto(
    LocalDate ytelseFom,
    LocalDate ytelseTom,
    Year sisteLignedeÅr,
    BesteBeregningResultatType besteBeregningResultat,
    BigDecimal beregnetPrAar,
    BigDecimal dagsats
) implements TemplateInnholdDto { }
