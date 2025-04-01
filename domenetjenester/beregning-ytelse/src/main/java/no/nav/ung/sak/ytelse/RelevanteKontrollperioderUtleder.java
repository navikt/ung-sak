package no.nav.ung.sak.ytelse;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.ytelseperioder.YtelsesperiodeDefinisjon;

public class RelevanteKontrollperioderUtleder {

    /**
     * @param ytelsesPerioder
     * @return
     */
    public static LocalDateTimeline<Boolean> utledPerioderRelevantForKontrollAvInntekt(LocalDateTimeline<YtelsesperiodeDefinisjon> ytelsesPerioder) {
        LocalDateTimeline<Boolean> perioderForKontroll = LocalDateTimeline.empty();
        if (ytelsesPerioder.toSegments().size() > 2) {
            final var ikkePåkrevdKontrollTidslinje = finnPerioderDerKontrollIkkeErPåkrevd(ytelsesPerioder);
            perioderForKontroll = ytelsesPerioder.disjoint(ikkePåkrevdKontrollTidslinje).mapValue(it -> true);
        }
        return perioderForKontroll;
    }

    public static LocalDateTimeline<FritattForKontroll> finnPerioderDerKontrollIkkeErPåkrevd(LocalDateTimeline<YtelsesperiodeDefinisjon> ytelsesPerioder) {
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

    private static boolean harIkkeYtelseDagenEtter(LocalDateTimeline<YtelsesperiodeDefinisjon> ytelsesPerioder, LocalDateSegment<YtelsesperiodeDefinisjon> it) {
        return ytelsesPerioder.intersection(dagenEtter(it)).isEmpty();
    }

    private static boolean harIkkeYtelseDagenFør(LocalDateTimeline<YtelsesperiodeDefinisjon> ytelsesPerioder, LocalDateSegment<YtelsesperiodeDefinisjon> it) {
        return ytelsesPerioder.intersection(dagenFør(it)).isEmpty();
    }

    private static LocalDateInterval dagenFør(LocalDateSegment<YtelsesperiodeDefinisjon> it) {
        return new LocalDateInterval(it.getFom().minusDays(1), it.getFom().minusDays(1));
    }

    private static LocalDateInterval dagenEtter(LocalDateSegment<YtelsesperiodeDefinisjon> it) {
        return new LocalDateInterval(it.getTom().plusDays(1), it.getTom().plusDays(1));
    }

    public record FritattForKontroll(boolean gjelderFørstePeriode, boolean gjelderSistePeriode) {
    }

}
