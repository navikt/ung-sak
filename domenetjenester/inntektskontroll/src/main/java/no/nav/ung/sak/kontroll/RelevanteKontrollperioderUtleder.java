package no.nav.ung.sak.kontroll;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;

import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Set;

@Dependent
public class RelevanteKontrollperioderUtleder {


    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;
    private MånedsvisTidslinjeUtleder månedsvisTidslinjeUtleder;
    private final boolean kontrollSisteMndEnabled;

    @Inject
    public RelevanteKontrollperioderUtleder(ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder, MånedsvisTidslinjeUtleder månedsvisTidslinjeUtleder, @KonfigVerdi(value = "KONTROLL_SISTE_MND_ENABLED", defaultVerdi = "false") boolean kontrollSisteMndEnabled) {
        this.prosessTriggerPeriodeUtleder = prosessTriggerPeriodeUtleder;
        this.månedsvisTidslinjeUtleder = månedsvisTidslinjeUtleder;
        this.kontrollSisteMndEnabled = kontrollSisteMndEnabled;
    }

    public LocalDateTimeline<Boolean> utledPerioderForKontrollAvInntekt(Long behandlingId) {
        return utledPerioderForKontrollAvInntekt(behandlingId, Set.of(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT)).mapValue(_ -> true);
    }


    public LocalDateTimeline<Set<BehandlingÅrsakType>> utledPerioderForKontrollAvInntekt(Long behandlingId,
                                                                                         Set<BehandlingÅrsakType> årsakerForKontroll) {

        final var relevantForKontrollTidslinje = utledPerioderRelevantForKontrollAvInntekt(behandlingId);
        final var markertForKontrollTidslinje = prosessTriggerPeriodeUtleder.utledTidslinje(behandlingId).filterValue(it -> it.stream().anyMatch(årsakerForKontroll::contains));
        if (markertForKontrollTidslinje.stream().anyMatch(s -> s.getLocalDateInterval().getFomDato().getDayOfMonth() != 1 || s.getLocalDateInterval().getTomDato().getDayOfMonth() != s.getLocalDateInterval().getTomDato().lengthOfMonth())) {
            throw new IllegalStateException("Forventet at alle perioder markert for kontroll er hele måneder");
        }
        return markertForKontrollTidslinje.intersection(relevantForKontrollTidslinje);
    }

    /**
     * Utleder måneder som er relevante for kontroll av inntekt basert på programperioder. Tar ikke hensyn til om det faktisk er markert for kontroll.
     *
     * @param behandlingId behandlingId
     * @return Perioder som er relevante for kontroll av inntekt
     */
    public LocalDateTimeline<Boolean> utledPerioderRelevantForKontrollAvInntekt(Long behandlingId) {
        final var periodisertMånedsvis = månedsvisTidslinjeUtleder.periodiserMånedsvis(behandlingId);
        final var relevantForKontrollTidslinje = utledPerioderRelevantForKontrollAvInntekt(periodisertMånedsvis);
        return relevantForKontrollTidslinje;
    }


    /**
     * Utleder måneder der vi skal gjøre kontroll av inntekt
     *
     * @param ytelsesPerioder Ytelseperioder
     * @return Perioder som er relevante for kontroll av inntekt
     */
    public LocalDateTimeline<Boolean> utledPerioderRelevantForKontrollAvInntekt(LocalDateTimeline<YearMonth> ytelsesPerioder) {
        LocalDateTimeline<Boolean> perioderForKontroll = LocalDateTimeline.empty();
        if (ytelsesPerioder.toSegments().size() > 1) {
            final var ikkePåkrevdKontrollTidslinje = finnPerioderDerKontrollIkkeErPåkrevd(ytelsesPerioder, kontrollSisteMndEnabled);
            perioderForKontroll = ytelsesPerioder.disjoint(ikkePåkrevdKontrollTidslinje).mapValue(it -> true);
        }
        if (kontrollSisteMndEnabled) {
            var mappedSegments = perioderForKontroll
                .toSegments()
                .stream()
                .map(it -> new LocalDateSegment<>(it.getFom(), it.getTom().with(TemporalAdjusters.lastDayOfMonth()), it.getValue()))
                .toList(); // Mapper segmenter til å dekke hele måneder
            perioderForKontroll = new LocalDateTimeline<>(mappedSegments, StandardCombinators::alwaysTrueForMatch).compress();
        }

        return perioderForKontroll;
    }

    public static LocalDateTimeline<FritattForKontroll> finnPerioderDerKontrollIkkeErPåkrevd(LocalDateTimeline<YearMonth> ytelsesPerioder, boolean kontrollSisteMndEnabled) {
        List<LocalDateSegment<FritattForKontroll>> ikkePåkrevdKontrollSegmenter;
        if (kontrollSisteMndEnabled) {
            ikkePåkrevdKontrollSegmenter = ytelsesPerioder.toSegments().stream()
                .filter(it -> harIkkeYtelseDagenFør(ytelsesPerioder, it))
                .map(it -> new LocalDateSegment<>(it.getFom(), it.getTom(), new FritattForKontroll(harIkkeYtelseDagenFør(ytelsesPerioder, it), false)))
                .toList();
        } else {
            ikkePåkrevdKontrollSegmenter = ytelsesPerioder.toSegments().stream()
                .filter(it -> harIkkeYtelseDagenFør(ytelsesPerioder, it)
                    ||  harIkkeYtelseDagenEtter(ytelsesPerioder, it))
                .map(it -> new LocalDateSegment<>(it.getFom(), it.getTom(), new FritattForKontroll(
                    harIkkeYtelseDagenFør(ytelsesPerioder, it),
                    harIkkeYtelseDagenEtter(ytelsesPerioder, it))))
                .toList();
        }
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
