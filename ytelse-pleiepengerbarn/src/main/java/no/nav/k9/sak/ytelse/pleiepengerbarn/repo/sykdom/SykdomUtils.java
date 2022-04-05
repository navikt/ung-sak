package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NavigableSet;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;
import no.nav.k9.sak.typer.Periode;

public final class SykdomUtils {

    private SykdomUtils() {
    }


    public static List<Periode> toPeriodeList(LocalDateTimeline<?> t) {
        return t.stream().map(l -> new Periode(l.getFom(), l.getTom())).toList();
    }

    public static List<Periode> toPeriodeList(NavigableSet<DatoIntervallEntitet> datoer) {
        return datoer.stream().map(i -> new Periode(i.getFomDato(), i.getTomDato())).toList();
    }

    public static LocalDateTimeline<Boolean> toLocalDateTimeline(List<Periode> perioder) {
        return new LocalDateTimeline<Boolean>(perioder.stream().map(p -> new LocalDateSegment<Boolean>(p.getFom(), p.getTom(), true)).toList()).compress();
    }

    public static LocalDateTimeline<Boolean> toLocalDateTimeline(NavigableSet<DatoIntervallEntitet> datoer) {
        return new LocalDateTimeline<Boolean>(datoer.stream().map(p -> new LocalDateSegment<Boolean>(p.getFomDato(), p.getTomDato(), true)).toList()).compress();
    }

    public static <T> LocalDateTimeline<Boolean> unionTilBoolean(LocalDateTimeline<Boolean> timeline, LocalDateTimeline<T> other) {
        return timeline.union(other, StandardCombinators::alwaysTrueForMatch);
    }

    public static <E> List<E> values(LocalDateTimeline<E> elements) {
        return elements.compress().stream().map(LocalDateSegment::getValue).toList();
    }

    public static <T> LocalDateTimeline<T> kunPerioderSomIkkeFinnesI(LocalDateTimeline<T> perioder, LocalDateTimeline<?> perioderSomSkalTrekkesFra) {
        return perioder.disjoint(perioderSomSkalTrekkesFra).compress();
    }

    public static LocalDateTimeline<SykdomVurderingVersjon> tilTidslinjeForType(Collection<SykdomVurderingVersjon> vurderinger, SykdomVurderingType type) {
        return SykdomUtils.tilTidslinje(vurderinger.stream().filter(v -> v.getSykdomVurdering().getType() == type).toList());
    }

    public static LocalDateTimeline<SykdomVurderingVersjon> tilTidslinje(Collection<SykdomVurderingVersjon> vurderinger) {
        final Collection<LocalDateSegment<SykdomVurderingVersjon>> segments = new ArrayList<>();
        for (SykdomVurderingVersjon vurdering : vurderinger) {
            for (SykdomVurderingPeriode periode : vurdering.getPerioder()) {
                segments.add(new LocalDateSegment<>(periode.getFom(), periode.getTom(), vurdering));
            }
        }

        final LocalDateTimeline<SykdomVurderingVersjon> tidslinje = new LocalDateTimeline<>(segments, (datoInterval, datoSegment, datoSegment2) -> {
            final Long rangering1 = datoSegment.getValue().getSykdomVurdering().getRangering();
            final Long rangering2 = datoSegment2.getValue().getSykdomVurdering().getRangering();
            final Long versjon1 = datoSegment.getValue().getVersjon();
            final Long versjon2 = datoSegment2.getValue().getVersjon();

            final SykdomVurderingVersjon valgtVurdering;
            if (rangering1.compareTo(rangering2) > 0) {
                valgtVurdering = datoSegment.getValue();
            } else if (rangering1.compareTo(rangering2) < 0) {
                valgtVurdering = datoSegment2.getValue();
            } else {
                valgtVurdering = (versjon1.compareTo(versjon2) > 0) ? datoSegment.getValue() : datoSegment2.getValue();
            }

            return new LocalDateSegment<>(datoInterval, valgtVurdering);
        });

        return tidslinje.compress();
    }

    public static LocalDateTimeline<Boolean> toBooleanTimeline(LocalDateTimeline<?> tidslinje) {
        return tidslinje.mapValue(v -> Boolean.TRUE);
    }
}
