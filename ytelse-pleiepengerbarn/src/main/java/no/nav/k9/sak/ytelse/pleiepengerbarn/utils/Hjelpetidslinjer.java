package no.nav.k9.sak.ytelse.pleiepengerbarn.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public final class Hjelpetidslinjer {

    private Hjelpetidslinjer() {
    }

    public static LocalDateTimeline<Boolean> lagTidslinjeMedKunHelger(LocalDateTimeline<?> tidslinje) {
        List<LocalDateSegment<Boolean>> helgesegmenter = new ArrayList<>();

        for (LocalDateSegment<?> segment : tidslinje.toSegments()) {
            var min = segment.getFom();
            var max = finnNærmeste(DayOfWeek.MONDAY, segment.getTom());
            LocalDate next = min;

            while (next.isBefore(max)) {
                if (Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).contains(next.getDayOfWeek()) && min.isEqual(next)) {
                    next = finnNeste(max, next);
                    helgesegmenter.add(new LocalDateSegment<>(min, next, true));
                }
                var start = finnNærmeste(DayOfWeek.SATURDAY, next);
                next = finnNeste(max, start);
                if (start.isBefore(max)) {
                    helgesegmenter.add(new LocalDateSegment<>(start, next, true));
                }
            }

        }
        return new LocalDateTimeline<>(helgesegmenter, StandardCombinators::coalesceRightHandSide);
    }

    private static LocalDate finnNeste(LocalDate max, LocalDate start) {
        LocalDate next;
        next = finnNærmeste(DayOfWeek.SUNDAY, start);
        if (next.isAfter(max)) {
            next = max;
        }
        return next;
    }

    private static LocalDate finnNærmeste(DayOfWeek target, LocalDate date) {
        var dayOfWeek = date.getDayOfWeek();
        if (target.equals(dayOfWeek)) {
            return date;
        }
        return finnNærmeste(target, date.plusDays(1));
    }

    public static <T> LocalDateTimeline<T> utledHullSomMåTettes(LocalDateTimeline<T> tidslinjen, KantIKantVurderer kantIKantVurderer) {
        var segmenter = tidslinjen.compress().toSegments();

        LocalDateSegment<T> periode = null;
        var resultat = new ArrayList<LocalDateSegment<T>>();

        for (LocalDateSegment<T> segment : segmenter) {
            if (periode != null) {
                var til = DatoIntervallEntitet.fra(segment.getLocalDateInterval());
                var fra = DatoIntervallEntitet.fra(periode.getLocalDateInterval());
                if (kantIKantVurderer.erKantIKant(til, fra) && !fra.grenserTil(til)) {
                    resultat.add(new LocalDateSegment<>(periode.getTom().plusDays(1), segment.getFom().minusDays(1), periode.getValue()));
                }
            }
            periode = segment;
        }

        return new LocalDateTimeline<T>(resultat);
    }
}
