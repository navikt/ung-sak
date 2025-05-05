package no.nav.ung.sak.formidling.innhold;

import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;

import java.math.BigDecimal;


/**
 * Intermediary objekt for å periodisere felter for beregning og tilkjent ytelse i brev.
 *
 * @param satsType              - satstype Lav eller Høy
 * @param grunnbeløp            - G-beløp
 * @param grunnbeløpFaktor      - faktor for satsType
 * @param årsbeløp              - grunnbeløp * grunnbeløpFaktor
 * @param dagsats               - dagsats av årsbeløp
 * @param antallBarn            - antall barn i perioden
 * @param barnetillegg          - barnetillegg i perioden
 * @param dagsatsTilkjentYtelse - smurt dagsats fra tilkjent ytelse
 * @param utbetalingsgrad       - utbetalingsgrad fra tilkjent ytelse
 */
public record GrunnlagOgTilkjentYtelse(
    UngdomsytelseSatsType satsType,
    long grunnbeløp,
    BigDecimal grunnbeløpFaktor,
    long årsbeløp,
    long dagsats,
    Integer antallBarn,
    int barnetillegg,
    long dagsatsTilkjentYtelse,
    BigDecimal utbetalingsgrad
) {
}
