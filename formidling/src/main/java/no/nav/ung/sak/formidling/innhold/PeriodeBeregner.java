package no.nav.ung.sak.formidling.innhold;

import java.time.LocalDate;
import java.time.YearMonth;

public class PeriodeBeregner {

    /**
     * Utleder fremtidig utbetalingsdato basert på sluttdato.
     * For siste måned i ungdomsprogrammet kontrolleres ikke inntekt
     * Oppdrag utbetaler siste måneden første virkedag i påfølgende måned.
     * Hvis sluttdato var forrige måned så utbetales det neste virkedag.
     * Nevner da ikke noe dato
     */
    static LocalDate utledFremtidigUtbetalingsdato(LocalDate sluttdato, YearMonth denneMåneden) {
        YearMonth sluttMåned = YearMonth.from(sluttdato);

        return sluttMåned.isBefore(denneMåneden) ? null
            : sluttMåned.plusMonths(1).atDay(10);
    }
}
