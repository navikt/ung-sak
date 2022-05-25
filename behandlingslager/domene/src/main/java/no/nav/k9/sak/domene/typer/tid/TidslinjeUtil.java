package no.nav.k9.sak.domene.typer.tid;

import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

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

    public static LocalDateTimeline<Boolean> tilTidslinjeKomprimert(NavigableSet<DatoIntervallEntitet> datoIntervaller) {
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

}
