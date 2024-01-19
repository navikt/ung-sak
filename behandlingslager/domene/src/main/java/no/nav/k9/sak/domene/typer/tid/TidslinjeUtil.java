package no.nav.k9.sak.domene.typer.tid;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.time.Year;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.typer.Periode;

public class TidslinjeUtil {

    private TidslinjeUtil() {
    }

    public static NavigableSet<DatoIntervallEntitet> tilDatoIntervallEntiteter(LocalDateTimeline<?> timeline) {
        if (timeline.isEmpty()) {
            return Collections.emptyNavigableSet();
        } else {
            return Collections.unmodifiableNavigableSet(timeline.getLocalDateIntervals().stream().map(DatoIntervallEntitet::fra).collect(Collectors.toCollection(TreeSet::new)));
        }
    }

    public static List<Periode> tilPerioder(LocalDateTimeline<?> tidslinje) {
        return tidslinje.stream().map(segment -> new Periode(segment.getFom(), segment.getTom())).toList();
    }

    public static List<Periode> tilPerioder(NavigableSet<DatoIntervallEntitet> datoIntervaller) {
        return datoIntervaller.stream().map(datoIntervall -> new Periode(datoIntervall.getFomDato(), datoIntervall.getTomDato())).toList();
    }

    public static LocalDateTimeline<Boolean> tilTidslinjeKomprimert(List<Periode> perioder) {
        return new LocalDateTimeline<>(perioder.stream().map(periode -> new LocalDateSegment<>(periode.getFom(), periode.getTom(), true)).toList()).compress();
    }
    
    public static LocalDateTimeline<Boolean> tilTidslinjeKomprimert(Collection<DatoIntervallEntitet> datoIntervaller) {
        return new LocalDateTimeline<>(datoIntervaller.stream().map(datoIntervall -> new LocalDateSegment<>(datoIntervall.getFomDato(), datoIntervall.getTomDato(), true)).toList()).compress();
    }

    public static <E> List<E> values(LocalDateTimeline<E> tidslinje) {
        return tidslinje.stream().map(LocalDateSegment::getValue).toList();
    }

    public static <T> LocalDateTimeline<T> kunPerioderSomIkkeFinnesI(LocalDateTimeline<T> perioder, LocalDateTimeline<?> perioderSomSkalTrekkesFra) {
        //TODO magisk å gjøre compress her
        return perioder.disjoint(perioderSomSkalTrekkesFra).compress();
    }

    public static LocalDateTimeline<Boolean> toBooleanTimeline(LocalDateTimeline<?> tidslinje) {
        return tidslinje.mapValue(v -> Boolean.TRUE);
    }

    public static <T> NavigableMap<Year, LocalDateTimeline<T>> splittOgGruperPåÅrstall(LocalDateTimeline<T> tidslinje) {
        if (tidslinje.isEmpty()) {
            return new TreeMap<>();
        }
        var tidsserieMedSplittedeSegmenter = tidslinje.splitAtRegular(tidslinje.getMinLocalDate().withDayOfYear(1), tidslinje.getMaxLocalDate(), Period.ofYears(1));
        var segmenterPrÅr = tidsserieMedSplittedeSegmenter.stream().collect(Collectors.groupingBy(segment -> Year.of(segment.getFom().getYear())));
        return segmenterPrÅr.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> new LocalDateTimeline<>(e.getValue()), (e1, e2) -> e1, TreeMap::new));
    }

    public static <T> LocalDateSegment<Set<T>> union(LocalDateInterval dateInterval, LocalDateSegment<Set<T>> lhs, LocalDateSegment<Set<T>> rhs) {
        Set<T> lv = lhs == null ? null : lhs.getValue();
        Set<T> rv = rhs == null ? null : rhs.getValue();

        Set<T> union;
        if (lv == null) {
            union = rv;
        } else if (rv == null) {
            union = lv;
        } else {
            union = new HashSet<>();
            union.addAll(lv);
            union.addAll(rv);
        }
        return new LocalDateSegment<>(dateInterval, union);
    }

    public static <T> LocalDateSegment<Set<T>> minus(LocalDateInterval dateInterval, LocalDateSegment<Set<T>> lhs, LocalDateSegment<Set<T>> rhs) {
        Set<T> lv = lhs == null ? null : lhs.getValue();
        Set<T> rv = rhs == null ? null : rhs.getValue();

        Set<T> resultat;
        if (lv == null) {
            resultat = null;
        } else if (rv == null) {
            resultat = lv;
        } else {
            resultat = new HashSet<>(lv);
            for (T t : rv) {
                //HashSet støtter null, men ikke i removeAll
                resultat.remove(t);
            }
        }
        return new LocalDateSegment<>(dateInterval, resultat);
    }

    /**
     * elementer som er i lhs eller rhs, men ikke i begge
     */
    public static <T> LocalDateSegment<Set<T>> forskjell(LocalDateInterval dateInterval, LocalDateSegment<Set<T>> lhs, LocalDateSegment<Set<T>> rhs) {
        Set<T> lhsValues = lhs != null && lhs.getValue() != null ? lhs.getValue() : Set.of();
        Set<T> rhsValues = rhs != null && rhs.getValue() != null ? rhs.getValue() : Set.of();
        HashSet<T> forskjell = new HashSet<>();
        lhsValues.stream().filter(v -> !rhsValues.contains(v)).forEach(forskjell::add);
        rhsValues.stream().filter(v -> !lhsValues.contains(v)).forEach(forskjell::add);
        if (forskjell.isEmpty()) {
            return null; //fjerner segmentet
        }
        return new LocalDateSegment<>(dateInterval, forskjell);
    }

    public static <T> LocalDateTimeline<T> begrensTilAntallDager(LocalDateTimeline<T> tidslinje, int maxAntallDager, boolean tellHelg) {
        int antallDager = 0;
        Iterator<LocalDateSegment<T>> segmentIterator = tidslinje.toSegments().iterator();
        LocalDate dato = null;
        while (segmentIterator.hasNext() && antallDager < maxAntallDager) {
            LocalDateSegment<T> segment = segmentIterator.next();
            dato = segment.getFom();
            while (antallDager < maxAntallDager && !dato.isAfter(segment.getTom())) {
                if (tellHelg || dato.getDayOfWeek().getValue() < DayOfWeek.SATURDAY.getValue()) {
                    antallDager++;
                }
                dato = dato.plusDays(1);
            }
        }
        return dato == null
            ? LocalDateTimeline.empty()
            : tidslinje.intersection(new LocalDateTimeline<>(tidslinje.getMinLocalDate(), dato.minusDays(1), null));
    }

}
