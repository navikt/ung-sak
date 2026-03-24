package no.nav.ung.ytelse.aktivitetspenger.formidling.dto;

import no.nav.ung.sak.formidling.innhold.TemplateInnholdDto;
import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.BesteBeregningResultatType;
import no.nav.ung.ytelse.aktivitetspenger.beregning.GrunnsatsType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InnvilgelseDto(
    LocalDate ytelseFom,
    LocalDate ytelseTom,
    GrunnsatsType grunnsatsType,
    boolean satsType,
    BigDecimal grunnbeløp,
    BigDecimal minsteytelse,
    String sisteLignedeÅr,
    BesteBeregningResultatType besteBeregningResultat,
    BigDecimal beregnetPrAar,
    BigDecimal dagsats,
    int dagsatsBarnetillegg
) implements TemplateInnholdDto { }
