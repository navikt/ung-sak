package no.nav.ung.sak.ytelse;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

import java.time.YearMonth;

public class RelevanteKontrollperioderUtleder {

    /**
     * Utleder måneder der vi skal gjøre kontroll av inntekt
     *
     * @param ytelsesPerioder Ytelseperioder
     * @return Perioder som er relevante for kontroll av inntekt
     */
    public static LocalDateTimeline<Boolean> utledPerioderRelevantForKontrollAvInntekt(LocalDateTimeline<YearMonth> ytelsesPerioder) {
        LocalDateTimeline<Boolean> perioderForKontroll = LocalDateTimeline.empty();
        if (ytelsesPerioder.toSegments().size() > 2) {
            final var ikkePåkrevdKontrollTidslinje = finnPerioderDerKontrollIkkeErPåkrevd(ytelsesPerioder);
            perioderForKontroll = ytelsesPerioder.disjoint(ikkePåkrevdKontrollTidslinje).mapValue(it -> true);
        }
        return perioderForKontroll;
    }

    public static LocalDateTimeline<FritattForKontroll> finnPerioderDerKontrollIkkeErPåkrevd(LocalDateTimeline<YearMonth> ytelsesPerioder) {
        final var ikkePåkrevdKontrollSegmenter = ytelsesPerioder.toSegments().stream()
            .filter(it -> harIkkeYtelseDagenFør(ytelsesPerioder, it)
                || harIkkeYtelseDagenEtter(ytelsesPerioder, it))
            .map(it -> new LocalDateSegment<>(it.getFom(), it.getTom(), new FritattForKontroll(
                harIkkeYtelseDagenFør(ytelsesPerioder, it),
                harIkkeYtelseDagenEtter(ytelsesPerioder, it))))
            .toList();
        final var ikkePåkrevdKontrollTidslinje = new LocalDateTimeline<>(ikkePåkrevdKontrollSegmenter);
        return ikkePåkrevdKontrollTidslinje;
    }

    private static boolean harIkkeYtelseDagenEtter(LocalDateTimeline<YearMonth> ytelsesPerioder, LocalDateSegment<YearMonth> it) {
        return ytelsesPerioder.intersection(dagenEtter(it)).isEmpty();
    }

    private static boolean harIkkeYtelseDagenFør(LocalDateTimeline<YearMonth> ytelsesPerioder, LocalDateSegment<YearMonth> it) {
        return ytelsesPerioder.intersection(dagenFør(it)).isEmpty();
    }

    private static LocalDateInterval dagenFør(LocalDateSegment<?> it) {
        return new LocalDateInterval(it.getFom().minusDays(1), it.getFom().minusDays(1));
    }

    private static LocalDateInterval dagenEtter(LocalDateSegment<?> it) {
        return new LocalDateInterval(it.getTom().plusDays(1), it.getTom().plusDays(1));
    }

    public record FritattForKontroll(boolean gjelderFørstePeriode, boolean gjelderSistePeriode) {
    }

}
