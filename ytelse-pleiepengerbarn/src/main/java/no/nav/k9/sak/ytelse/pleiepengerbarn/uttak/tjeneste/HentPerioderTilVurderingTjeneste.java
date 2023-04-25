package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste;

import java.util.NavigableSet;
import java.util.Optional;
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
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.perioder.ForlengelseTjeneste;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;

@Dependent
public class HentPerioderTilVurderingTjeneste {

    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;

    private SøknadsperiodeRepository søknadsperiodeRepository;

    private Instance<ForlengelseTjeneste> forlengelseTjenester;

    @Inject
    public HentPerioderTilVurderingTjeneste(
        SøknadsperiodeTjeneste søknadsperiodeTjeneste,
        @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
        SøknadsperiodeRepository søknadsperiodeRepository,
        @Any Instance<ForlengelseTjeneste> forlengelseTjenester) {
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;

        this.søknadsperiodeRepository = søknadsperiodeRepository;
        this.forlengelseTjenester = forlengelseTjenester;
    }

    public NavigableSet<DatoIntervallEntitet> hentPerioderTilVurderingUtenUbesluttet(Behandling behandling) {
        BehandlingReferanse referanse = BehandlingReferanse.fra(behandling);
        var søknadsperioder = TidslinjeUtil.tilTidslinjeKomprimert(finnSykdomsperioder(referanse));

        return fjernTrukkedePerioder(referanse, søknadsperioder);
    }


    public NavigableSet<DatoIntervallEntitet> hentKunRelevantePerioder(Behandling behandling) {
        var referanse = BehandlingReferanse.fra(behandling);
        var perioderTilVurderingTjeneste = perioderTilVurderingTjeneste(referanse);

        var forlengelseTjeneste = ForlengelseTjeneste.finnTjeneste(forlengelseTjenester, behandling.getFagsakYtelseType(), behandling.getType());
        var søknadsperioderForBehandling = finnRelevanteSøknadsperioder(behandling);

        LocalDateTimeline<Boolean> tidslinje = LocalDateTimeline.empty();

        final var perioder = perioderTilVurderingTjeneste.utled(referanse.getBehandlingId(), VilkårType.OPPTJENINGSVILKÅRET);
        var forlengelseperioder = forlengelseTjeneste.utledPerioderSomSkalBehandlesSomForlengelse(referanse, perioder, VilkårType.OPPTJENINGSVILKÅRET);
        for (DatoIntervallEntitet p : perioder) {
            if (forlengelseperioder.contains(p)) {
                var relevanteSøknadsperioder = søknadsperioderForBehandling.stream().filter(sp -> sp.overlapper(p)).collect(Collectors.toCollection(TreeSet::new));
                tidslinje = tidslinje.combine(TidslinjeUtil.tilTidslinjeKomprimert(new TreeSet<>(relevanteSøknadsperioder)), StandardCombinators::alwaysTrueForMatch, LocalDateTimeline.JoinStyle.CROSS_JOIN);
            } else {
                tidslinje = tidslinje.combine(TidslinjeUtil.tilTidslinjeKomprimert(new TreeSet<>(perioder)), StandardCombinators::alwaysTrueForMatch, LocalDateTimeline.JoinStyle.CROSS_JOIN);
            }
        }

        return TidslinjeUtil.tilDatoIntervallEntiteter(tidslinje);
    }

    private TreeSet<DatoIntervallEntitet> finnRelevanteSøknadsperioder(Behandling behandling) {
        var søknadsperiodeGrunnlag = søknadsperiodeRepository.hentGrunnlag(behandling.getId());
        return søknadsperiodeGrunnlag.map(SøknadsperiodeGrunnlag::getRelevantSøknadsperioder)
            .stream()
            .flatMap(it -> it.getPerioder().stream())
            .flatMap(p -> p.getPerioder().stream())
            .map(Søknadsperiode::getPeriode).collect(Collectors.toCollection(TreeSet::new));
    }

    public NavigableSet<DatoIntervallEntitet> hentPerioderTilVurderingMedUbesluttet(Behandling behandling, Optional<DatoIntervallEntitet> utvidetPeriodeSomFølgeAvDødsfall) {
        BehandlingReferanse referanse = BehandlingReferanse.fra(behandling);
        var datoer = søknadsperiodeTjeneste.utledFullstendigPeriode(behandling.getId());

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

    private NavigableSet<DatoIntervallEntitet> finnSykdomsperioder(BehandlingReferanse referanse) {
        VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste = perioderTilVurderingTjeneste(referanse);
        var definerendeVilkår = perioderTilVurderingTjeneste.definerendeVilkår();
        LocalDateTimeline<Boolean> tidslinje = LocalDateTimeline.empty();
        for (VilkårType vilkårType : definerendeVilkår) {
            final var perioder = perioderTilVurderingTjeneste.utled(referanse.getBehandlingId(), vilkårType);
            tidslinje = tidslinje.combine(TidslinjeUtil.tilTidslinjeKomprimert(new TreeSet<>(perioder)), StandardCombinators::alwaysTrueForMatch, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        return TidslinjeUtil.tilDatoIntervallEntiteter(tidslinje);
    }

    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste(BehandlingReferanse behandlingReferanse) {
        return VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, behandlingReferanse.getFagsakYtelseType(), behandlingReferanse.getBehandlingType());
    }
}
