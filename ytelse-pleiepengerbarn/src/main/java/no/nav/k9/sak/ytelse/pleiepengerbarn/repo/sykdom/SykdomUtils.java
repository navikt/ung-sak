package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NavigableSet;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;
import no.nav.k9.sak.typer.Periode;

public final class SykdomUtils {

    private SykdomUtils() {}


    public static List<Periode> toPeriodeList(LocalDateTimeline<?> t) {
        return t.stream().map(l -> new Periode(l.getFom(), l.getTom())).collect(Collectors.toList());
    }

    public static List<Periode> toPeriodeList(NavigableSet<DatoIntervallEntitet> datoer) {
        return datoer.stream().map(i -> new Periode(i.getFomDato(), i.getTomDato())).collect(Collectors.toList());
    }

    public static LocalDateTimeline<Boolean> toLocalDateTimeline(List<Periode> perioder) {
        return new LocalDateTimeline<Boolean>(perioder.stream().map(p -> new LocalDateSegment<Boolean>(p.getFom(), p.getTom(), true)).collect(Collectors.toList())).compress();
    }

    public static LocalDateTimeline<Boolean> toLocalDateTimeline(NavigableSet<DatoIntervallEntitet> datoer) {
        return new LocalDateTimeline<Boolean>(datoer.stream().map(p -> new LocalDateSegment<Boolean>(p.getFomDato(), p.getTomDato(), true)).collect(Collectors.toList())).compress();
    }

    public static <T> LocalDateTimeline<Boolean> unionTilBoolean(LocalDateTimeline<Boolean> timeline, LocalDateTimeline<T> other) {
        return timeline.union(other, new LocalDateSegmentCombinator<Boolean, T, Boolean>() {

            @Override
            public LocalDateSegment<Boolean> combine(LocalDateInterval datoInterval, LocalDateSegment<Boolean> datoSegment, LocalDateSegment<T> datoSegment2) {
                return new LocalDateSegment<Boolean>(datoInterval, true);
            }
        });
    }

    public static <E> List<E> values(LocalDateTimeline<E> elements) {
        return elements.compress().stream().map(LocalDateSegment::getValue).collect(Collectors.toList());
    }

    public static <T> LocalDateTimeline<T> kunPerioderSomIkkeFinnesI(LocalDateTimeline<T> perioder, LocalDateTimeline<?> perioderSomSkalTrekkesFra) {
        return perioder.combine(perioderSomSkalTrekkesFra, (datoInterval, datoSegment, datoSegment2) -> {
            if (datoSegment2 == null) {
                return new LocalDateSegment<>(datoInterval, datoSegment.getValue());
            }
            return null;
        }, JoinStyle.LEFT_JOIN).compress();
    }

    public static LocalDateTimeline<SykdomVurderingVersjon> tilTidslinjeForType(Collection<SykdomVurderingVersjon> vurderinger, SykdomVurderingType type) {
        return SykdomUtils.tilTidslinje(vurderinger.stream().filter(v -> v.getSykdomVurdering().getType() == type).collect(Collectors.toList()));
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
}
