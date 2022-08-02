package no.nav.k9.sak.domene.typer.tid;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import no.nav.fpsak.tidsserie.LocalDateInterval;

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

    public static List<LocalDateInterval> periodiserPrÅr(LocalDateInterval feriepengerPeriode) {
        LocalDate fom = feriepengerPeriode.getFomDato();
        LocalDate tom = feriepengerPeriode.getTomDato();
        List<LocalDateInterval> perioder = new ArrayList<>();
        while (fom.getYear() != tom.getYear()) {
            LocalDate sisteDagIÅr = fom.withMonth(12).withDayOfMonth(31);
            LocalDateInterval dateInterval = new LocalDateInterval(fom, sisteDagIÅr);
            perioder.add(dateInterval);
            fom = sisteDagIÅr.plusDays(1);
        }
        LocalDateInterval dateInterval = new LocalDateInterval(fom, tom);
        perioder.add(dateInterval);
        return perioder;
    }
}
