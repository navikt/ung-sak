package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste;

import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.perioder.EndretUtbetalingPeriodeutleder;
import no.nav.k9.sak.perioder.ForlengelseTjeneste;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;

@Dependent
public class HentPerioderTilVurderingTjeneste {

    private final SøknadsperiodeTjeneste søknadsperiodeTjeneste;
    private final Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;
    private final Instance<ForlengelseTjeneste> forlengelseTjenester;
    private final Instance<EndretUtbetalingPeriodeutleder> endretUtbetalingPeriodeutledere;

    @Inject
    public HentPerioderTilVurderingTjeneste(
        SøknadsperiodeTjeneste søknadsperiodeTjeneste,
        @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
        @Any Instance<ForlengelseTjeneste> forlengelseTjenester,
        @Any Instance<EndretUtbetalingPeriodeutleder> endretUtbetalingPeriodeutledere) {
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.forlengelseTjenester = forlengelseTjenester;
        this.endretUtbetalingPeriodeutledere = endretUtbetalingPeriodeutledere;
    }

    public NavigableSet<DatoIntervallEntitet> hentPerioderTilVurderingUtenUbesluttet(BehandlingReferanse referanse) {
        var perioderMedRelevanteEndringer = finnPerioderTilVurderingMedRelevanteEndringer(referanse);
        var endretPerioderTidslinje = TidslinjeUtil.tilTidslinjeKomprimertMedMuligOverlapp(perioderMedRelevanteEndringer);
        return fjernTrukkedePerioder(referanse, endretPerioderTidslinje).stream()
            .map(s -> DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private TreeSet<DatoIntervallEntitet> finnPerioderTilVurderingMedRelevanteEndringer(BehandlingReferanse referanse) {
        var forlengelseTjeneste = ForlengelseTjeneste.finnTjeneste(forlengelseTjenester, referanse.getFagsakYtelseType(), referanse.getBehandlingType());
        var endretUtbetalingPeriodeutleder = EndretUtbetalingPeriodeutleder.finnUtleder(endretUtbetalingPeriodeutledere, referanse.getFagsakYtelseType(), referanse.getBehandlingType());
        var vilkårsPerioderTilVurderingTjeneste = finnPerioderTilVurderingTjeneste(referanse);
        var perioderTilVurdering = vilkårsPerioderTilVurderingTjeneste.utledFraDefinerendeVilkår(referanse.getBehandlingId());
        var forlengelserIOpptjening = forlengelseTjeneste.utledPerioderSomSkalBehandlesSomForlengelse(referanse, perioderTilVurdering, VilkårType.OPPTJENINGSVILKÅRET);

        return perioderTilVurdering.stream()
            .flatMap(vilkårsperiode -> begrensVedForlengelse(referanse, vilkårsperiode, forlengelserIOpptjening, endretUtbetalingPeriodeutleder).stream())
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private static NavigableSet<DatoIntervallEntitet> begrensVedForlengelse(BehandlingReferanse referanse, DatoIntervallEntitet vilkårsperiode, NavigableSet<DatoIntervallEntitet> forlengelserIOpptjening, EndretUtbetalingPeriodeutleder endretUtbetalingPeriodeutleder) {
        if (erForlengelse(vilkårsperiode, forlengelserIOpptjening)) {
            return begrensPeriode(referanse, vilkårsperiode, endretUtbetalingPeriodeutleder);
        }
        return new TreeSet<>(Set.of(vilkårsperiode));
    }

    private static NavigableSet<DatoIntervallEntitet> begrensPeriode(BehandlingReferanse referanse, DatoIntervallEntitet vilkårsperiode, EndretUtbetalingPeriodeutleder endretUtbetalingPeriodeutleder) {
        return endretUtbetalingPeriodeutleder.utledPerioder(referanse, vilkårsperiode);
    }

    private static boolean erForlengelse(DatoIntervallEntitet vilkårsperiode, NavigableSet<DatoIntervallEntitet> forlengelserIOpptjening) {
        return forlengelserIOpptjening.contains(vilkårsperiode);
    }


    public NavigableSet<DatoIntervallEntitet> hentPerioderTilVurderingMedUbesluttet(BehandlingReferanse referanse, Optional<BehandlingReferanse> originalBehandlingReferanse, Optional<DatoIntervallEntitet> utvidetPeriodeSomFølgeAvDødsfall) {
        var tidslinje = hentRelevantePerioderFraFullstendigSøknadsperioder(referanse);
        tidslinje = originalBehandlingReferanse.map(this::hentRelevantePerioderFraFullstendigSøknadsperioder)
            .orElse(LocalDateTimeline.empty())
            .crossJoin(tidslinje);
        if (utvidetPeriodeSomFølgeAvDødsfall.isPresent()) {
            tidslinje = tidslinje.combine(new LocalDateSegment<>(utvidetPeriodeSomFølgeAvDødsfall.get().toLocalDateInterval(), true), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        return tidslinje.stream()
            .map(s -> DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private LocalDateTimeline<Boolean> hentRelevantePerioderFraFullstendigSøknadsperioder(BehandlingReferanse referanse) {
        var datoer = søknadsperiodeTjeneste.utledFullstendigPeriode(referanse.getId());

        var søknadsperioder = TidslinjeUtil.tilTidslinjeKomprimert(datoer);
        var endretUtbetalingPeriodeutleder = EndretUtbetalingPeriodeutleder.finnUtleder(endretUtbetalingPeriodeutledere, referanse.getFagsakYtelseType(), referanse.getBehandlingType());
        var begrensetTidslinje = søknadsperioder.getLocalDateIntervals()
            .stream()
            .flatMap(p -> begrensPeriode(referanse, DatoIntervallEntitet.fraOgMedTilOgMed(p.getFomDato(), p.getTomDato()), endretUtbetalingPeriodeutleder).stream())
            .map(p -> new LocalDateTimeline<>(p.toLocalDateInterval(), true))
            .reduce(LocalDateTimeline.empty(), LocalDateTimeline::crossJoin);
        return fjernTrukkedePerioder(referanse, begrensetTidslinje);
    }

    private LocalDateTimeline<Boolean> fjernTrukkedePerioder(BehandlingReferanse referanse, LocalDateTimeline<Boolean> søknadsperioder) {
        final LocalDateTimeline<Boolean> trukkedeKrav = hentTrukkedeKravTidslinje(referanse);
        return TidslinjeUtil.kunPerioderSomIkkeFinnesI(søknadsperioder, trukkedeKrav);
    }

    private LocalDateTimeline<Boolean> hentTrukkedeKravTidslinje(BehandlingReferanse referanse) {
        return TidslinjeUtil.tilTidslinjeKomprimert(søknadsperiodeTjeneste.hentKravperioder(referanse)
            .stream()
            .filter(SøknadsperiodeTjeneste.Kravperiode::isHarTrukketKrav)
            .map(SøknadsperiodeTjeneste.Kravperiode::getPeriode)
            .collect(Collectors.toCollection(TreeSet::new)));
    }

    private VilkårsPerioderTilVurderingTjeneste finnPerioderTilVurderingTjeneste(BehandlingReferanse behandlingReferanse) {
        return VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, behandlingReferanse.getFagsakYtelseType(), behandlingReferanse.getBehandlingType());
    }
}
