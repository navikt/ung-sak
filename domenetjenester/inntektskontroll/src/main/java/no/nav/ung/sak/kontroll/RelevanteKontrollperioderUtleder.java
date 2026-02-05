package no.nav.ung.sak.kontroll;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;

import java.time.Period;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.Set;
import java.util.stream.Collectors;

@Dependent
public class RelevanteKontrollperioderUtleder {

    private Instance<ProsessTriggerPeriodeUtleder> prosessTriggerPeriodeUtledere;
    private MånedsvisTidslinjeUtleder månedsvisTidslinjeUtleder;
    private BehandlingRepository behandlingRepository;

    @Inject
    public RelevanteKontrollperioderUtleder(@Any Instance<ProsessTriggerPeriodeUtleder> prosessTriggerPeriodeUtledere,
                                            MånedsvisTidslinjeUtleder månedsvisTidslinjeUtleder,
                                            BehandlingRepository behandlingRepository) {
        this.prosessTriggerPeriodeUtledere = prosessTriggerPeriodeUtledere;
        this.månedsvisTidslinjeUtleder = månedsvisTidslinjeUtleder;
        this.behandlingRepository = behandlingRepository;
    }

    public LocalDateTimeline<Boolean> utledPerioderForKontrollAvInntekt(Long behandlingId) {
        return utledPerioderForKontrollAvInntekt(behandlingId, Set.of(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT)).mapValue(_ -> true);
    }


    public LocalDateTimeline<Set<BehandlingÅrsakType>> utledPerioderForKontrollAvInntekt(Long behandlingId,
                                                                                         Set<BehandlingÅrsakType> årsakerForKontroll) {

        final var relevantForKontrollTidslinje = utledPerioderRelevantForKontrollAvInntekt(behandlingId);
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder = ProsessTriggerPeriodeUtleder.finnTjeneste(prosessTriggerPeriodeUtledere, behandling.getFagsakYtelseType());
        final var markertForKontrollTidslinje = prosessTriggerPeriodeUtleder.utledTidslinje(behandlingId)
            .mapValue(it -> it.stream().filter(årsakerForKontroll::contains).collect(Collectors.toSet()))
            .filterValue(it -> !it.isEmpty());
        LocalDateTimeline<Set<BehandlingÅrsakType>> markertOgRelevant = markertForKontrollTidslinje.intersection(relevantForKontrollTidslinje).compress();
        if (markertOgRelevant.isEmpty()) {
            return LocalDateTimeline.empty();
        }
        return markertOgRelevant
            .splitAtRegular(markertOgRelevant.getMinLocalDate().withDayOfMonth(1), markertOgRelevant.getMaxLocalDate(), Period.ofMonths(1));
    }

    /**
     * Utleder måneder som er relevante for kontroll av inntekt basert på programperioder. Tar ikke hensyn til om det faktisk er markert for kontroll.
     *
     * @param behandlingId behandlingId
     * @return Perioder som er relevante for kontroll av inntekt
     */
    public LocalDateTimeline<Boolean> utledPerioderRelevantForKontrollAvInntekt(Long behandlingId) {
        final var periodisertMånedsvis = månedsvisTidslinjeUtleder.finnMånedsvisPeriodisertePerioder(behandlingId);
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
            final var ikkePåkrevdKontrollTidslinje = finnPerioderDerKontrollIkkeErPåkrevd(ytelsesPerioder);
            perioderForKontroll = ytelsesPerioder.disjoint(ikkePåkrevdKontrollTidslinje).mapValue(it -> true);
        }
        return utvidTilHeleMåneder(perioderForKontroll);
    }

    private static LocalDateTimeline<Boolean> utvidTilHeleMåneder(LocalDateTimeline<Boolean> perioderForKontroll) {
        var mappedSegments = perioderForKontroll
            .toSegments()
            .stream()
            .map(it -> new LocalDateSegment<>(it.getFom().withDayOfMonth(1), it.getTom().with(TemporalAdjusters.lastDayOfMonth()), it.getValue()))
            .toList(); // Mapper segmenter til å dekke hele måneder
        return new LocalDateTimeline<>(mappedSegments, StandardCombinators::alwaysTrueForMatch).compress();
    }

    public static LocalDateTimeline<FritattForKontroll> finnPerioderDerKontrollIkkeErPåkrevd(LocalDateTimeline<YearMonth> ytelsesPerioder) {
        var ikkePåkrevdKontrollSegmenter = ytelsesPerioder.toSegments().stream()
            .filter(it -> harIkkeYtelseDagenFør(ytelsesPerioder, it))
            .map(it -> new LocalDateSegment<>(it.getFom(), it.getTom(), new FritattForKontroll(harIkkeYtelseDagenFør(ytelsesPerioder, it), false)))
            .toList();
        return new LocalDateTimeline<>(ikkePåkrevdKontrollSegmenter);
    }

    private static boolean harIkkeYtelseDagenFør(LocalDateTimeline<YearMonth> ytelsesPerioder, LocalDateSegment<YearMonth> it) {
        return ytelsesPerioder.intersection(dagenFør(it)).isEmpty();
    }

    private static LocalDateInterval dagenFør(LocalDateSegment<?> it) {
        return new LocalDateInterval(it.getFom().minusDays(1), it.getFom().minusDays(1));
    }

    public record FritattForKontroll(boolean gjelderFørstePeriode, boolean gjelderSistePeriode) {
    }

}
