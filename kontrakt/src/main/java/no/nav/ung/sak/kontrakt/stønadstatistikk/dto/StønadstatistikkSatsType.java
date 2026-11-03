package no.nav.ung.sak.kontrakt.stønadstatistikk.dto;

/**
 * LAV/HØY er minstesats (gitt av grunnbeløpFaktor), og brukes både for ungdomsprogramytelsen og aktivitetspenger.
 * Reglene for når LAV/HØY gjelder kan være ulike mellom ytelsene, så tolkningen må ses sammen med ytelseType på hendelsen.
 * BEREGNINGSGRUNNLAG brukes kun for aktivitetspenger, når dagsatsen er beregnet fra pensjonsgivende inntekt (og er høyere enn minstesatsen).
 *
 * Er dette riktig eller bør det være egen lav og høy siden reglene er ulike?
 */
public enum StønadstatistikkSatsType {
    LAV,
    HØY,
    BEREGNINGSGRUNNLAG
}
