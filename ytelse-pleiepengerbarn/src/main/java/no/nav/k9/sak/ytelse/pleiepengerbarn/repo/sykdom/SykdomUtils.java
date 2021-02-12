package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.util.List;
import java.util.NavigableSet;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Periode;

public final class SykdomUtils {
    
    private SykdomUtils() {}

    
    static List<Periode> toPeriodeList(LocalDateTimeline<?> t) {
        return t.stream().map(l -> new Periode(l.getFom(), l.getTom())).collect(Collectors.toList());
    }
    
    static LocalDateTimeline<Boolean> toLocalDateTimeline(List<Periode> perioder) {
        return new LocalDateTimeline<Boolean>(perioder.stream().map(p -> new LocalDateSegment<Boolean>(p.getFom(), p.getTom(), true)).collect(Collectors.toList()));
    }
    
    static LocalDateTimeline<Boolean> toLocalDateTimeline(NavigableSet<DatoIntervallEntitet> datoer) {
        return new LocalDateTimeline<Boolean>(datoer.stream().map(p -> new LocalDateSegment<Boolean>(p.getFomDato(), p.getTomDato(), true)).collect(Collectors.toList()));
    }
    
    static <T, U> LocalDateTimeline<T> kunPerioderSomIkkeFinnesI(LocalDateTimeline<T> perioder, LocalDateTimeline<U> perioderSomSkalTrekkesFra) {
        return perioder.combine(perioderSomSkalTrekkesFra, new LocalDateSegmentCombinator<T, U, T>() {
            @Override
            public LocalDateSegment<T> combine(LocalDateInterval datoInterval,
                    LocalDateSegment<T> datoSegment, LocalDateSegment<U> datoSegment2) {
                if (datoSegment2 == null) {
                    return new LocalDateSegment<>(datoInterval, datoSegment.getValue());
                }
                return null;
            }
        }, JoinStyle.LEFT_JOIN).compress();
    }
}
