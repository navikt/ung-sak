package no.nav.ung.sak.formidling.innhold;

import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;

import java.math.BigDecimal;

/**
 * Intermediary objekt for å periodisere felter for beregning og tilkjent ytelse i brev.
 */
public record GrunnlagOgTilkjentYtelse(
    long dagsats,
    long utbetalingsgrad,
    UngdomsytelseSatsType satsType,
    BigDecimal grunnbeløpFaktor,
    long grunnbeløp,
    long årsbeløp,
    Integer antallBarn,
    int barnetillegg
) {
}
