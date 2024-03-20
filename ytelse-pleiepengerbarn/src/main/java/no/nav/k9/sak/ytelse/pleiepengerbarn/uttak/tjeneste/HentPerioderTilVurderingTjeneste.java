package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste;

import java.util.List;
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
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.kontrakt.vilkår.VilkårUtfallSamlet;
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
    private final VilkårResultatRepository vilkårResultatRepository;

    @Inject
    public HentPerioderTilVurderingTjeneste(
        SøknadsperiodeTjeneste søknadsperiodeTjeneste,
        @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
        @Any Instance<ForlengelseTjeneste> forlengelseTjenester,
        @Any Instance<EndretUtbetalingPeriodeutleder> endretUtbetalingPeriodeutledere,
        VilkårResultatRepository vilkårResultatRepository) {
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.forlengelseTjenester = forlengelseTjenester;
        this.endretUtbetalingPeriodeutledere = endretUtbetalingPeriodeutledere;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    public NavigableSet<DatoIntervallEntitet> hentPerioderTilVurderingUtenUbesluttet(BehandlingReferanse referanse) {
        var perioderMedRelevanteEndringer = finnPerioderTilVurderingMedRelevanteEndringer(referanse);
        var endretPerioderTidslinje = TidslinjeUtil.tilTidslinjeKomprimertMedMuligOverlapp(perioderMedRelevanteEndringer);
        return fjernTrukkedePerioder(referanse, endretPerioderTidslinje);
    }

    private TreeSet<DatoIntervallEntitet> finnPerioderTilVurderingMedRelevanteEndringer(BehandlingReferanse referanse) {
        var forlengelseTjeneste = ForlengelseTjeneste.finnTjeneste(forlengelseTjenester, referanse.getFagsakYtelseType(), referanse.getBehandlingType());
        var endretUtbetalingPeriodeutleder = EndretUtbetalingPeriodeutleder.finnUtleder(endretUtbetalingPeriodeutledere, referanse.getFagsakYtelseType(), referanse.getBehandlingType());
        var vilkårsPerioderTilVurderingTjeneste = finnPerioderTilVurderingTjeneste(referanse);
        var perioderTilVurdering = vilkårsPerioderTilVurderingTjeneste.utledFraDefinerendeVilkår(referanse.getBehandlingId());
        var forlengelserIOpptjening = finnForengelserUtenEndretSamletVilkårsresultat(referanse, forlengelseTjeneste, perioderTilVurdering);
        return perioderTilVurdering.stream()
            .flatMap(vilkårsperiode -> begrensVedForlengelse(referanse, vilkårsperiode, forlengelserIOpptjening, endretUtbetalingPeriodeutleder).stream())
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private TreeSet<DatoIntervallEntitet> finnForengelserUtenEndretSamletVilkårsresultat(BehandlingReferanse referanse, ForlengelseTjeneste forlengelseTjeneste, NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
        var samletResultat = samletVilkårsresultat(referanse.getBehandlingId());
        var originalSamletResultat = referanse.getOriginalBehandlingId().map(this::samletVilkårsresultat).orElse(LocalDateTimeline.empty());
        var endretVilkårsresultatTidslinje = samletResultat.crossJoin(originalSamletResultat, erEndretVilkårsresultat()).filterValue(it -> it);
        return forlengelseTjeneste.utledPerioderSomSkalBehandlesSomForlengelse(referanse, perioderTilVurdering, VilkårType.OPPTJENINGSVILKÅRET)
            .stream().filter(p -> endretVilkårsresultatTidslinje.intersection(p.toLocalDateInterval()).isEmpty())
            .collect(Collectors.toCollection(TreeSet::new));
    }

    public LocalDateTimeline<VilkårUtfallSamlet> samletVilkårsresultat(Long behandlingId) {
        var vilkårene = vilkårResultatRepository.hent(behandlingId);
        LocalDateTimeline<Boolean> allePerioder = vilkårene.getAlleIntervaller();
        var maksPeriode = DatoIntervallEntitet.fra(allePerioder.getMinLocalDate(), allePerioder.getMaxLocalDate());
        return samleVilkårUtfall(vilkårene, maksPeriode);
    }

    private LocalDateTimeline<VilkårUtfallSamlet> samleVilkårUtfall(Vilkårene vilkårene, DatoIntervallEntitet maksPeriode) {
        var timelinePerVilkår = vilkårene.getVilkårTidslinjer(maksPeriode);
        var timeline = new LocalDateTimeline<List<VilkårUtfallSamlet.VilkårUtfall>>(List.of());
        for (var e : timelinePerVilkår.entrySet()) {
            LocalDateTimeline<VilkårUtfallSamlet.VilkårUtfall> utfallTimeline = e.getValue().mapValue(v -> new VilkårUtfallSamlet.VilkårUtfall(e.getKey(), v.getAvslagsårsak(), v.getUtfall()));
            timeline = timeline.crossJoin(utfallTimeline.compress(), StandardCombinators::allValues);
        }
        return timeline.mapValue(VilkårUtfallSamlet::fra);
    }


    private static LocalDateSegmentCombinator<VilkårUtfallSamlet, VilkårUtfallSamlet, Boolean> erEndretVilkårsresultat() {
        return (di, lhs, rhs) -> new LocalDateSegment<>(di, (lhs == null || rhs == null) ||
            !lhs.getValue().getSamletUtfall().equals(Utfall.OPPFYLT) ||
            !lhs.getValue().getSamletUtfall().equals(rhs.getValue().getSamletUtfall()));
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
