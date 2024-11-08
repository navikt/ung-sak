package no.nav.k9.sak.domene.arbeidsforhold;

import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

/**
 * Utleder tidslinje for mellomliggende helg.
 */
public class MellomliggendeHelgUtleder {

    public <T> LocalDateTimeline<Boolean> beregnMellomliggendeHelg(LocalDateTimeline<T> tidsserie) {
        if (tidsserie.isEmpty()) {
            return LocalDateTimeline.empty();
        }
        LocalDateTimeline<Boolean> mellomliggendePerioder = new LocalDateTimeline<>(tidsserie.getMinLocalDate(), tidsserie.getMaxLocalDate(), true)
            .disjoint(tidsserie);
        return new LocalDateTimeline<>(mellomliggendePerioder.stream()
            .filter(this::erUtelukkendeHelg)
            .toList());
    }

    private <T> boolean erUtelukkendeHelg(LocalDateSegment<T> segment) {
        Set<DayOfWeek> helgedager = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        return helgedager.contains(segment.getFom().getDayOfWeek())
            && helgedager.contains(segment.getTom().getDayOfWeek())
            && ChronoUnit.DAYS.between(segment.getFom(), segment.getTom()) < 2;
    }

}


