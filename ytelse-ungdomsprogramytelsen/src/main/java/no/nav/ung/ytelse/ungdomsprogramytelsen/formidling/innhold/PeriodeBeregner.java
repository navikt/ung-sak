package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold;

import java.time.LocalDate;
import java.time.YearMonth;

public class PeriodeBeregner {

    /**
     * Utleder fremtidig utbetalingsdato basert på sluttdato.
     * Hvis sluttdato var forrige måned, oppgis ikke fremtidig utbetalingsdato i brevet.
     */
    static LocalDate utledFremtidigUtbetalingsdato(LocalDate sluttdato, YearMonth denneMåneden) {
        YearMonth sluttMåned = YearMonth.from(sluttdato);

        return sluttMåned.isBefore(denneMåneden) ? null
            : sluttMåned.plusMonths(1).atDay(12);
    }
}
