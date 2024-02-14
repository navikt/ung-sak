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
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
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
    private final boolean skalVurdereKunEndretPeriodeEnabled;

    @Inject
    public HentPerioderTilVurderingTjeneste(
        SøknadsperiodeTjeneste søknadsperiodeTjeneste,
        @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
        @Any Instance<ForlengelseTjeneste> forlengelseTjenester,
        @Any Instance<EndretUtbetalingPeriodeutleder> endretUtbetalingPeriodeutledere,
        @KonfigVerdi(value = "UTVIDET_ENDRING_UTBETALING_UTLEDER", defaultVerdi = "false") boolean skalVurdereKunEndretPeriodeEnabled) {
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.forlengelseTjenester = forlengelseTjenester;
        this.endretUtbetalingPeriodeutledere = endretUtbetalingPeriodeutledere;
        this.skalVurdereKunEndretPeriodeEnabled = skalVurdereKunEndretPeriodeEnabled;
    }

    public NavigableSet<DatoIntervallEntitet> hentPerioderTilVurderingUtenUbesluttet(BehandlingReferanse referanse) {
        if (skalVurdereKunEndretPeriodeEnabled) {
            var perioderMedRelevanteEndringer = finnPerioderTilVurderingMedRelevanteEndringer(referanse);
            var endretPerioderTidslinje = TidslinjeUtil.tilTidslinjeKomprimertMedMuligOverlapp(perioderMedRelevanteEndringer);
            return fjernTrukkedePerioder(referanse, endretPerioderTidslinje);
        } else {
            VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste = finnPerioderTilVurderingTjeneste(referanse);
            var søknadsperioder = TidslinjeUtil.tilTidslinjeKomprimert(perioderTilVurderingTjeneste.utledFraDefinerendeVilkår(referanse.getId()));
            return fjernTrukkedePerioder(referanse, søknadsperioder);
        }
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


    public NavigableSet<DatoIntervallEntitet> hentPerioderTilVurderingMedUbesluttet(BehandlingReferanse referanse, Optional<DatoIntervallEntitet> utvidetPeriodeSomFølgeAvDødsfall) {
        var datoer = søknadsperiodeTjeneste.utledFullstendigPeriode(referanse.getId());

        var søknadsperioder = TidslinjeUtil.tilTidslinjeKomprimert(datoer);
        if (utvidetPeriodeSomFølgeAvDødsfall.isPresent()) {
            søknadsperioder = søknadsperioder.combine(new LocalDateSegment<>(utvidetPeriodeSomFølgeAvDødsfall.get().toLocalDateInterval(), true), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }

        return fjernTrukkedePerioder(referanse, søknadsperioder);
    }

    private TreeSet<DatoIntervallEntitet> fjernTrukkedePerioder(BehandlingReferanse referanse, LocalDateTimeline<Boolean> søknadsperioder) {
        final LocalDateTimeline<Boolean> trukkedeKrav = hentTrukkedeKravTidslinje(referanse);
        return TidslinjeUtil.kunPerioderSomIkkeFinnesI(søknadsperioder, trukkedeKrav).stream()
            .map(s -> DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()))
            .collect(Collectors.toCollection(TreeSet::new));
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
