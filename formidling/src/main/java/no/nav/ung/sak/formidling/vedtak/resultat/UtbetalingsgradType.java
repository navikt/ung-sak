package no.nav.ung.sak.formidling.vedtak.resultat;

import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;

import java.math.BigDecimal;

/**
 * Grovklassifisering av utbetalingsgraden. Brev bryr seg kun om hvilken av disse kategoriene perioden faller i,
 * så tidslinjen splittes ikke på beløp/prosent som varierer fra periode til periode.
 */
public enum UtbetalingsgradType {

    IKKE_SATT,
    INGEN_UTBETALING,
    REDUSERT,
    FULL;

    private static final BigDecimal HUNDRE = BigDecimal.valueOf(100);

    public static UtbetalingsgradType av(TilkjentYtelseVerdi tilkjentYtelse) {
        if (tilkjentYtelse == null) {
            return IKKE_SATT;
        }
        var grad = tilkjentYtelse.utbetalingsgrad();
        if (grad.compareTo(BigDecimal.ZERO) <= 0) {
            return INGEN_UTBETALING;
        }
        return grad.compareTo(HUNDRE) >= 0 ? FULL : REDUSERT;
    }

    public boolean erSatt() {
        return this != IKKE_SATT;
    }

    public boolean harUtbetaling() {
        return this == REDUSERT || this == FULL;
    }

    /**
     * Redusert eller helt bortfalt utbetaling — de to behandles likt i brev.
     */
    public boolean erRedusert() {
        return this == REDUSERT || this == INGEN_UTBETALING;
    }
}
