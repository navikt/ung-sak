package no.nav.k9.sak.domene.vedtak.ekstern;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktørYtelse;
import no.nav.k9.sak.domene.iay.modell.Ytelse;
import no.nav.k9.sak.domene.iay.modell.YtelseFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@ApplicationScoped
public class OverlappendeYtelserTjeneste {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private BeregningsresultatRepository beregningsresultatRepository;

    OverlappendeYtelserTjeneste() {
        // CDI
    }

    @Inject
    public OverlappendeYtelserTjeneste(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                       BeregningsresultatRepository beregningsresultatRepository) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.beregningsresultatRepository = beregningsresultatRepository;
    }

    public Map<Ytelse, NavigableSet<LocalDateInterval>> finnOverlappendeYtelser(BehandlingReferanse ref, Set<FagsakYtelseType> ytelseTyperSomSjekkesMot) {
        var tilkjentYtelsePerioder = hentTilkjentYtelsePerioder(ref);
        var aktørYtelse = inntektArbeidYtelseTjeneste.hentGrunnlag(ref.getBehandlingId())
            .getAktørYtelseFraRegister(ref.getAktørId());
        if (tilkjentYtelsePerioder.isEmpty() || aktørYtelse.isEmpty()) {
            return Map.of();
        }

        var tilkjentYtelseTimeline = new LocalDateTimeline<Boolean>(List.of());
        for (LocalDateSegment<Boolean> periode : tilkjentYtelsePerioder) {
            tilkjentYtelseTimeline = tilkjentYtelseTimeline.combine(new LocalDateTimeline<>(List.of(periode)), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        tilkjentYtelseTimeline = tilkjentYtelseTimeline.compress();

        return doFinnOverlappendeYtelser(tilkjentYtelseTimeline, new YtelseFilter(aktørYtelse.get()).filter(yt -> ytelseTyperSomSjekkesMot.contains(yt.getYtelseType())));
    }

    public static Map<Ytelse, NavigableSet<LocalDateInterval>> doFinnOverlappendeYtelser(LocalDateTimeline<Boolean> tilkjentYtelseTimeline, YtelseFilter ytelseFilter) {
        Map<Ytelse, NavigableSet<LocalDateInterval>> overlapp = new TreeMap<>();
        if (!tilkjentYtelseTimeline.isEmpty()) {

            for (var yt : ytelseFilter.getFiltrertYtelser()) {
                var ytp = yt.getPeriode();
                var overlappPeriode = innvilgelseOverlapperMedAnnenYtelse(tilkjentYtelseTimeline, ytp);
                if (!overlappPeriode.isEmpty()) {
                    if (yt.getYtelseAnvist().isEmpty()) {
                        // er under behandling. flagger hele perioden med overlapp
                        overlapp.put(yt, overlappPeriode);
                    } else {
                        var anvistSegmenter = yt.getYtelseAnvist().stream()
                            .map(ya -> new LocalDateSegment<>(ya.getAnvistFOM(), ya.getAnvistTOM(), Boolean.TRUE))
                            .sorted()
                            .collect(Collectors.toCollection(LinkedHashSet::new));

                        var anvistTimeline = new LocalDateTimeline<>(anvistSegmenter, StandardCombinators::alwaysTrueForMatch);
                        var intersection = anvistTimeline.intersection(tilkjentYtelseTimeline);
                        if (!intersection.isEmpty()) {
                            overlapp.put(yt, intersection.getDatoIntervaller());
                        }
                    }
                }
            }
        }
        return overlapp;
    }

    private static NavigableSet<LocalDateInterval> innvilgelseOverlapperMedAnnenYtelse(LocalDateTimeline<Boolean> vilkårPeriode, DatoIntervallEntitet ytp) {
        return vilkårPeriode.getDatoIntervaller()
            .stream()
            .map(it -> it.overlap(new LocalDateInterval(ytp.getFomDato(), ytp.getTomDato())))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toCollection(TreeSet::new));
    }


    private Set<LocalDateSegment<Boolean>> hentTilkjentYtelsePerioder(BehandlingReferanse ref) {
        return beregningsresultatRepository.hentBeregningsresultatAggregat(ref.getBehandlingId())
            .map(BehandlingBeregningsresultatEntitet::getBgBeregningsresultat)
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder)
            .filter(perioder -> !perioder.isEmpty())
            .map(perioder -> perioder.stream()
                .map(it -> new LocalDateSegment<>(it.getPeriode().getFomDato(), it.getPeriode().getTomDato(), true))
                .collect(Collectors.toSet()))
            .orElse(Set.of());
    }
}
