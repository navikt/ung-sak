package no.nav.ung.sak.formidling.innhold;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;

public class PeriodeBeregner {
    public static LocalDate nesteUkedag(LocalDate date) {
        LocalDate nesteDag = date.plusDays(1);
        if (nesteDag.getDayOfWeek() == DayOfWeek.SATURDAY) {
            return nesteDag.plusDays(2);
        } else if (nesteDag.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return nesteDag.plusDays(1);
        }
        return nesteDag;
    }

    public static LocalDate forrigeUkedag(LocalDate date) {
        LocalDate forrigeDag = date.minusDays(1);
        if (forrigeDag.getDayOfWeek() == DayOfWeek.SATURDAY) {
            return forrigeDag.minusDays(1);
        } else if (forrigeDag.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return forrigeDag.minusDays(2);
        }
        return forrigeDag;
    }

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
