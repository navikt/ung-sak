package no.nav.ung.ytelse.aktivitetspenger.formidling.dto.innvilgelse.beregning;

import no.nav.ung.sak.kontrakt.aktivitetspenger.beregning.BesteBeregningResultatType;

import java.math.BigDecimal;

public record BeregningDto(
    String sisteLignedeÅr,
    BesteBeregningResultatType besteBeregningResultat,
    BigDecimal beregnetPrAar,
    long dagsats) {
}
