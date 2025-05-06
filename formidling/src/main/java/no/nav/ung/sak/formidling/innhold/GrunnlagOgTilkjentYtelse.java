package no.nav.ung.sak.formidling.innhold;

import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;

import java.math.BigDecimal;


/**
 * Intermediary objekt for å periodisere felter for beregning og tilkjent ytelse i brev.
 *
 * @param satsType                - satstype Lav eller Høy
 * @param dagsats                 - dagsatsGrunnbeløpFaktor + barnetillggSats
 * @param grunnbeløp              - G-beløp
 * @param grunnbeløpFaktor        - faktor for satsType
 * @param årsbeløp                - grunnbeløp * grunnbeløpFaktor
 * @param dagsatsGrunnbeløpFaktor - dagsats av årsbeløp
 * @param antallBarn              - antall barn i perioden
 * @param barnetilleggSats        - barnetillegg i perioden
 * @param dagsatsTilkjentYtelse   - smurt dagsats fra tilkjent ytelse
 * @param utbetalingsgrad         - utbetalingsgrad fra tilkjent ytelse
 */
public record GrunnlagOgTilkjentYtelse(
    UngdomsytelseSatsType satsType,
    long dagsats,
    long grunnbeløp,
    BigDecimal grunnbeløpFaktor,
    long årsbeløp,
    long dagsatsGrunnbeløpFaktor,
    Integer antallBarn,
    int barnetilleggSats,
    long dagsatsTilkjentYtelse,
    long utbetalingsgrad
) {
}
