package no.nav.ung.sak.ytelse.kontroll;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;

import java.time.YearMonth;
import java.util.Set;

@Dependent
public class RelevanteKontrollperioderUtleder {


    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;
    private MånedsvisTidslinjeUtleder månedsvisTidslinjeUtleder;

    @Inject
    public RelevanteKontrollperioderUtleder(ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder, MånedsvisTidslinjeUtleder månedsvisTidslinjeUtleder) {
        this.prosessTriggerPeriodeUtleder = prosessTriggerPeriodeUtleder;
        this.månedsvisTidslinjeUtleder = månedsvisTidslinjeUtleder;
    }

    public LocalDateTimeline<Set<BehandlingÅrsakType>> utledPerioderForKontrollAvInntekt(Long behandlingId) {
        return utledPerioderForKontrollAvInntekt(behandlingId, Set.of(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT));
    }


    public LocalDateTimeline<Set<BehandlingÅrsakType>> utledPerioderForKontrollAvInntekt(Long behandlingId,
                                                                                         Set<BehandlingÅrsakType> årsakerForKontroll) {

        final var periodisertMånedsvis = månedsvisTidslinjeUtleder.periodiserMånedsvis(behandlingId);
        final var relevantForKontrollTidslinje = utledPerioderRelevantForKontrollAvInntekt(periodisertMånedsvis);
        final var markertForKontrollTidslinje = prosessTriggerPeriodeUtleder.utledTidslinje(behandlingId).filterValue(it -> it.stream().anyMatch(årsakerForKontroll::contains));
        return markertForKontrollTidslinje.intersection(relevantForKontrollTidslinje);
    }


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
