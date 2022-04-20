package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@Dependent
public class PerioderMedSykdomInnvilgetUtleder {

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;

    @Inject
    public PerioderMedSykdomInnvilgetUtleder(BehandlingRepository behandlingRepository,
                                             VilkårResultatRepository vilkårResultatRepository,
                                             @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
    }

    public Set<VilkårPeriode> utledInnvilgedePerioderTilVurdering(BehandlingReferanse referanse) {
        var behandlingId = referanse.getBehandlingId();
        final var perioderVurdertISykdom = utledPerioderVurdert(behandlingId);

        var vilkårene = vilkårResultatRepository.hent(behandlingId);

        return finnInnvilgedePerioder(vilkårene, perioderVurdertISykdom);
    }

    private NavigableSet<DatoIntervallEntitet> utledPerioderVurdert(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, behandling.getFagsakYtelseType(), behandling.getType());

        LocalDateTimeline<Boolean> tidslinje = LocalDateTimeline.empty();
        for (VilkårType vilkårType : perioderTilVurderingTjeneste.definerendeVilkår()) {
            NavigableSet<DatoIntervallEntitet> perioderForVilkår = perioderTilVurderingTjeneste.utled(behandlingId, vilkårType);
            var perioderSomTidslinje = new LocalDateTimeline<>(perioderForVilkår.stream().map(it -> new LocalDateSegment<>(it.getFomDato(), it.getTomDato(), true)).toList());
            tidslinje = tidslinje.crossJoin(perioderSomTidslinje, StandardCombinators::alwaysTrueForMatch);
        }
        return DatoIntervallEntitet.fraTimeline(tidslinje.compress());
    }

    private Set<VilkårPeriode> finnInnvilgedePerioder(Vilkårene vilkårene,
                                                      NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
        var s1 = vilkårene.getVilkår(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR)
            .map(Vilkår::getPerioder)
            .orElse(List.of())
            .stream();
        var s2 = vilkårene.getVilkår(VilkårType.MEDISINSKEVILKÅR_18_ÅR)
            .map(Vilkår::getPerioder)
            .orElse(List.of())
            .stream();
        var s3 = vilkårene.getVilkår(VilkårType.I_LIVETS_SLUTTFASE)
            .map(Vilkår::getPerioder)
            .orElse(List.of())
            .stream();
        return Stream.concat(Stream.concat(s1, s2), s3)
            .filter(it -> perioderTilVurdering.stream().anyMatch(at -> at.overlapper(it.getPeriode())))
            .filter(it -> Utfall.OPPFYLT.equals(it.getUtfall()))
            .collect(Collectors.toSet());
    }

}
