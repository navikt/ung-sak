package no.nav.k9.sak.domene.vedtak.ekstern;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.Ytelse;
import no.nav.k9.sak.domene.iay.modell.YtelseFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.Saksnummer;

@ApplicationScoped
public class OverlappendeYtelserTjeneste {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private BeregningsresultatRepository beregningsresultatRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;

    OverlappendeYtelserTjeneste() {
        // CDI
    }

    @Inject
    public OverlappendeYtelserTjeneste(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                       BeregningsresultatRepository beregningsresultatRepository,
                                       @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
    }

    public Map<Ytelse, NavigableSet<LocalDateInterval>> finnOverlappendeYtelser(BehandlingReferanse ref, Set<FagsakYtelseType> ytelseTyperSomSjekkesMot) {
        var perioderTilVurderingTidslinje = hentBeregningsgrunnlagPerioderTilVurderingTidslinje(ref);
        var tilkjentYtelseTidslinje = hentTilkjentYtelseTidslinje(ref);
        tilkjentYtelseTidslinje = tilkjentYtelseTidslinje.intersection(perioderTilVurderingTidslinje);
        if (tilkjentYtelseTidslinje.isEmpty()) {
            return Map.of();
        }

        var aktørYtelse = inntektArbeidYtelseTjeneste.hentGrunnlag(ref.getBehandlingId())
            .getAktørYtelseFraRegister(ref.getAktørId());
        if (aktørYtelse.isEmpty()) {
            return Map.of();
        }

        return doFinnOverlappendeYtelser(ref.getSaksnummer(), tilkjentYtelseTidslinje, new YtelseFilter(aktørYtelse.get()).filter(yt -> ytelseTyperSomSjekkesMot.contains(yt.getYtelseType())));
    }

    private Map<Ytelse, NavigableSet<LocalDateInterval>> doFinnOverlappendeYtelser(Saksnummer saksnummer, LocalDateTimeline<Boolean> tilkjentYtelseTimeline, YtelseFilter ytelseFilter) {
        Map<Ytelse, NavigableSet<LocalDateInterval>> overlappendeYtelser = new HashMap<>();
        if (!tilkjentYtelseTimeline.isEmpty()) {

            for (var yt : ytelseFilter.getFiltrertYtelser()) {
                if (saksnummer.equals(yt.getSaksnummer())) {
                    // Skal ikke sjekke overlappende ytelser i IAY mot egen fagsak
                    continue;
                }
                var ytp = yt.getPeriode();
                var overlappPeriode = innvilgelseOverlapperMedAnnenYtelse(tilkjentYtelseTimeline, ytp);
                if (!overlappPeriode.isEmpty()) {
                    if (yt.getYtelseAnvist().isEmpty()) {
                        // har ingen utbetaling (kan skyldes både at ytelse er under behandling, eller at ytelsetype ikke er av type kontantytelse)
                        continue;
                    } else {
                        var anvistSegmenter = yt.getYtelseAnvist().stream()
                            .map(ya -> new LocalDateSegment<>(ya.getAnvistFOM(), ya.getAnvistTOM(), Boolean.TRUE))
                            .sorted()
                            .collect(Collectors.toCollection(LinkedHashSet::new));

                        var anvistTimeline = new LocalDateTimeline<>(anvistSegmenter, StandardCombinators::alwaysTrueForMatch);
                        var ovelappendeTidslinje = anvistTimeline.intersection(tilkjentYtelseTimeline);
                        if (!ovelappendeTidslinje.isEmpty()) {
                            var ovelappendeDatoIntervaller = new TreeSet<>(overlappendeYtelser.getOrDefault(yt, new TreeSet<>()));
                            ovelappendeDatoIntervaller.addAll(ovelappendeTidslinje.getLocalDateIntervals());
                            overlappendeYtelser.put(yt, ovelappendeDatoIntervaller);
                        }
                    }
                }
            }
        }
        return overlappendeYtelser;
    }

    private static NavigableSet<LocalDateInterval> innvilgelseOverlapperMedAnnenYtelse(LocalDateTimeline<Boolean> vilkårPeriode, DatoIntervallEntitet ytp) {
        return vilkårPeriode.getLocalDateIntervals()
            .stream()
            .map(it -> it.overlap(new LocalDateInterval(ytp.getFomDato(), ytp.getTomDato())))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private LocalDateTimeline<Boolean> hentBeregningsgrunnlagPerioderTilVurderingTidslinje(BehandlingReferanse ref) {
        var perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, ref.getFagsakYtelseType(), ref.getBehandlingType());
        var perioderTilVurdering = perioderTilVurderingTjeneste.utled(ref.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        var periodeSegmenter = perioderTilVurdering.stream().map(it -> new LocalDateSegment<>(it.getFomDato(), it.getTomDato(), true)).toList();

        return new LocalDateTimeline<>(periodeSegmenter);
    }

    private LocalDateTimeline<Boolean> hentTilkjentYtelseTidslinje(BehandlingReferanse ref) {
        var tilkjentYtelsePerioder = beregningsresultatRepository.hentBeregningsresultatAggregat(ref.getBehandlingId())
            .map(BehandlingBeregningsresultatEntitet::getBgBeregningsresultat)
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder)
            .filter(perioder -> !perioder.isEmpty())
            .map(perioder -> perioder.stream()
                .filter(brPeriode -> utbetalingsgradStørreEnn0(brPeriode))
                .map(brPeriode -> new LocalDateSegment<>(brPeriode.getPeriode().getFomDato(), brPeriode.getPeriode().getTomDato(), true))
                .collect(Collectors.toSet()))
            .orElse(Set.of());
        return new LocalDateTimeline<>(tilkjentYtelsePerioder);
    }

    private boolean utbetalingsgradStørreEnn0(BeregningsresultatPeriode brPeriode) {
        var utbetalingsgrad = brPeriode.getLavestUtbetalingsgrad().orElse(BigDecimal.ZERO);
        return utbetalingsgrad.compareTo(BigDecimal.ZERO) > 0;
    }
}
