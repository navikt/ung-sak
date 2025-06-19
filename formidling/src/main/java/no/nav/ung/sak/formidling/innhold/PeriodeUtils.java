package no.nav.ung.sak.formidling.innhold;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class PeriodeUtils {
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
}
