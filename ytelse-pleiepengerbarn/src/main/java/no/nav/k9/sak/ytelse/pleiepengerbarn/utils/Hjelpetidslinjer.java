package no.nav.k9.sak.ytelse.pleiepengerbarn.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

public final class Hjelpetidslinjer {
    
    private Hjelpetidslinjer() {}

    public static LocalDateTimeline<Boolean> lagTidslinjeMedKunHelger(LocalDateTimeline<?> tidslinje) {
        var timeline = new LocalDateTimeline<Boolean>(List.of());
        for (LocalDateSegment<?> segment : tidslinje.toSegments()) {
            var min = segment.getFom();
            var max = segment.getTom();
            LocalDate next = min;

            while (next.isBefore(max)) {
                if (Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).contains(next.getDayOfWeek()) && min.isEqual(next)) {
                    next = finnNeste(max, next);
                    timeline = timeline.combine(new LocalDateSegment<>(min, next, true), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
                }
                var start = finnNærmeste(DayOfWeek.SATURDAY, next);
                next = finnNeste(max, start);
                if (start.isBefore(max)) {
                    timeline = timeline.combine(new LocalDateSegment<>(start, next, true), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
                }
            }

        }
        return timeline;
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
}
