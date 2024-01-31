package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import java.util.NavigableSet;
import java.util.Objects;
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
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@Dependent
public class PerioderMedSykdomInnvilgetUtleder {

    private final BehandlingRepository behandlingRepository;
    private final VilkårResultatRepository vilkårResultatRepository;
    private final Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;
    private final boolean ikkeVurderVedAvslag;

    @Inject
    public PerioderMedSykdomInnvilgetUtleder(BehandlingRepository behandlingRepository,
                                             VilkårResultatRepository vilkårResultatRepository,
                                             @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                             @KonfigVerdi(value = "UTTAK_KLIPP_BORT_AVSLAG", defaultVerdi = "false") boolean ikkeVurderVedAvslag
    ) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.ikkeVurderVedAvslag = ikkeVurderVedAvslag;
    }

    public NavigableSet<DatoIntervallEntitet> utledInnvilgedePerioderTilVurdering(BehandlingReferanse referanse) {
        var behandlingId = referanse.getBehandlingId();
        VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste = finnVilkårsPerioderTjeneste(behandlingId);
        final var perioderVurdertISykdom = perioderTilVurderingTjeneste.utledFraDefinerendeVilkår(behandlingId);

        var vilkårene = vilkårResultatRepository.hent(behandlingId);

        return finnInnvilgedePerioder(behandlingId, vilkårene, perioderVurdertISykdom);
    }

    private static LocalDateTimeline<Boolean> finnAvslåttTidslinjeAlleVilkår(Vilkårene vilkårene) {
        var avslåttePerioder = vilkårene.getVilkårene()
            .stream().flatMap(v -> v.getPerioder().stream())
            .filter(p -> p.getUtfall().equals(Utfall.IKKE_OPPFYLT))
            .map(VilkårPeriode::getPeriode)
            .map(p -> new LocalDateSegment<>(p.toLocalDateInterval(), true))
            .toList();
        return new LocalDateTimeline<>(avslåttePerioder, StandardCombinators::alwaysTrueForMatch);
    }

    private NavigableSet<DatoIntervallEntitet> finnInnvilgedePerioder(Long behandlingId, Vilkårene vilkårene, NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
        VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste = finnVilkårsPerioderTjeneste(behandlingId);

        var definerendeVilkår = vilkårsPerioderTilVurderingTjeneste.definerendeVilkår();
        LocalDateTimeline<Boolean> tidslinje = LocalDateTimeline.empty();

        for (VilkårType vilkårType : definerendeVilkår) {
            var segmenter = vilkårene.getVilkår(vilkårType).orElseThrow()
                .getPerioder()
                .stream()
                .filter(it -> perioderTilVurdering.stream().anyMatch(at -> at.overlapper(it.getPeriode())))
                .map(it -> new LocalDateSegment<>(it.getPeriode().toLocalDateInterval(), Objects.equals(Utfall.OPPFYLT, it.getGjeldendeUtfall())))
                .collect(Collectors.toCollection(TreeSet::new));
            tidslinje = tidslinje.combine(new LocalDateTimeline<>(segmenter), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }

        if (ikkeVurderVedAvslag) {
            var avslåttTidslinje = finnAvslåttTidslinjeAlleVilkår(vilkårene);
            tidslinje = tidslinje.disjoint(avslåttTidslinje);

        }

        tidslinje = tidslinje.filterValue(it -> it);
        return TidslinjeUtil.tilDatoIntervallEntiteter(tidslinje.compress());
    }

    private VilkårsPerioderTilVurderingTjeneste finnVilkårsPerioderTjeneste(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        return VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, behandling.getFagsakYtelseType(), behandling.getType());
    }
}
