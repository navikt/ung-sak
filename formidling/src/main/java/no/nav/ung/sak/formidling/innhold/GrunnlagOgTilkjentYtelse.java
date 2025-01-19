package no.nav.ung.sak.formidling.innhold;

import java.math.BigDecimal;

import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;

/**
 * Intermidiary objekt for å periodisere felter for beregning og tilkjent ytelse i brev.
 */
public record GrunnlagOgTilkjentYtelse(
    long dagsats,
    BigDecimal utbetalingsgrad,
    UngdomsytelseSatsType satsType,
    BigDecimal grunnbeløpFaktor,
    long grunnbeløp,
    long årsbeløp,
    Integer antallBarn,
    int barnetillegg
) {
}
