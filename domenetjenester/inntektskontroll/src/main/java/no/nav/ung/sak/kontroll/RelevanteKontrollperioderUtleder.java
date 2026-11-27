package no.nav.ung.sak.kontroll;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
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

    private final Instance<ProsessTriggerPeriodeUtleder> prosessTriggerPeriodeUtledere;
    private final MånedsvisTidslinjeUtleder månedsvisTidslinjeUtleder;
    private final BehandlingRepository behandlingRepository;
    private final Instance<InntektskontrollFritakUtleder> fritakUtledere;

    @Inject
    public RelevanteKontrollperioderUtleder(@Any Instance<ProsessTriggerPeriodeUtleder> prosessTriggerPeriodeUtledere,
                                            MånedsvisTidslinjeUtleder månedsvisTidslinjeUtleder,
                                            BehandlingRepository behandlingRepository,
                                            @Any Instance<InntektskontrollFritakUtleder> fritakUtledere) {
        this.prosessTriggerPeriodeUtledere = prosessTriggerPeriodeUtledere;
        this.månedsvisTidslinjeUtleder = månedsvisTidslinjeUtleder;
        this.behandlingRepository = behandlingRepository;
        this.fritakUtledere = fritakUtledere;
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
    public LocalDateTimeline<InfoOmRådata> utledPerioderRelevantForKontrollAvInntekt(Long behandlingId) {
        final var periodisertMånedsvis = månedsvisTidslinjeUtleder.finnMånedsvisPeriodisertePerioder(behandlingId);
        final var ytelseType = behandlingRepository.hentBehandling(behandlingId).getFagsakYtelseType();
        return utledPerioderRelevantForKontrollAvInntekt(periodisertMånedsvis, ytelseType);
    }


    /**
     * Utleder måneder der vi skal gjøre kontroll av inntekt
     *
     * @param ytelsesPerioder Ytelseperioder
     * @return Perioder som er relevante for kontroll av inntekt
     */
    public LocalDateTimeline<InfoOmRådata> utledPerioderRelevantForKontrollAvInntekt(LocalDateTimeline<YearMonth> ytelsesPerioder,
                                                                                 FagsakYtelseType ytelseType) {
        final var ikkePåkrevdKontrollTidslinje = InntektskontrollFritakUtleder.finnTjeneste(ytelseType, fritakUtledere)
            .finnPerioderFritattForKontroll(ytelsesPerioder);
        final var perioderForKontroll = ytelsesPerioder.disjoint(ikkePåkrevdKontrollTidslinje).mapValue(_ -> true);
        return utvidTilHeleMåneder(perioderForKontroll);
    }

    private static LocalDateTimeline<InfoOmRådata> utvidTilHeleMåneder(LocalDateTimeline<Boolean> perioderForKontroll) {
        if (perioderForKontroll.isEmpty()) {
            return LocalDateTimeline.empty();
        }
        return perioderForKontroll.compress()
            .splitAtRegular(perioderForKontroll.getMinLocalDate().withDayOfMonth(1), perioderForKontroll.getMaxLocalDate(), Period.ofMonths(1))
            .segmenter()
            .stream()
            .map(it -> new LocalDateTimeline<>(it.getFom().withDayOfMonth(1), it.getTom().with(TemporalAdjusters.lastDayOfMonth()),
                new InfoOmRådata(it.getFom().getDayOfMonth() == 1 && it.getTom().equals(it.getTom().with(TemporalAdjusters.lastDayOfMonth())))))
            .reduce(LocalDateTimeline.empty(), (venstre, høyre) -> venstre.crossJoin(høyre,
                (periode, v, h) -> new LocalDateSegment<>(periode, new InfoOmRådata(
                    (v == null || v.getValue().gjelderHelePerioden()) && (h == null || h.getValue().gjelderHelePerioden())))))
            .compress();
    }

    public record InfoOmRådata(boolean gjelderHelePerioden) {
    }

}
