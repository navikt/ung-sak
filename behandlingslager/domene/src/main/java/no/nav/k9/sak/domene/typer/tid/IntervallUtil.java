package no.nav.k9.sak.domene.typer.tid;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

public class IntervallUtil {

    public static long beregnKalanderdager(LocalDateInterval periode) {
        return ChronoUnit.DAYS.between(periode.getFomDato(), periode.getTomDato()) + 1;
    }

    public static int beregnUkedager(LocalDateInterval periode) {
        return beregnUkedager(periode.getFomDato(), periode.getTomDato());
    }

    public static int beregnUkedager(LocalDate fom, LocalDate tom) {
        int antallUkedager = 0;
        for (LocalDate d = fom; !d.isAfter(tom); d = d.plusDays(1)) {
            int dag = d.getDayOfWeek().getValue();
            if (dag <= DayOfWeek.FRIDAY.getValue()) {
                antallUkedager++;
            }
        }
        return antallUkedager;
    }

    public static <T> LocalDateTimeline<T> splittVedÅrskifte(LocalDateTimeline<T> tidslinje) {
        return tidslinje.isEmpty() || tidslinje.getMinLocalDate().getYear() == tidslinje.getMaxLocalDate().getYear()
            ? tidslinje
            : tidslinje.splitAtRegular(tidslinje.getMinLocalDate().withDayOfYear(1), tidslinje.getMaxLocalDate(), Period.ofYears(1));
    }
}
