package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
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
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
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

        return finnInnvilgedePerioder(behandlingId, vilkårene, perioderVurdertISykdom);
    }

    private NavigableSet<DatoIntervallEntitet> utledPerioderVurdert(Long behandlingId) {
        VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste = finnVilkårsPerioderTjeneste(behandlingId);

        LocalDateTimeline<Boolean> tidslinje = LocalDateTimeline.empty();
        for (VilkårType vilkårType : perioderTilVurderingTjeneste.definerendeVilkår()) {
            NavigableSet<DatoIntervallEntitet> perioderForVilkår = perioderTilVurderingTjeneste.utled(behandlingId, vilkårType);
            var perioderSomTidslinje = TidslinjeUtil.tilTidslinjeKomprimert(perioderForVilkår);
            tidslinje = tidslinje.crossJoin(perioderSomTidslinje, StandardCombinators::alwaysTrueForMatch);
        }
        return TidslinjeUtil.tilDatoIntervallEntiteter(tidslinje.compress());
    }

    private Set<VilkårPeriode> finnInnvilgedePerioder(Long behandlingId, Vilkårene vilkårene, NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
        VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste = finnVilkårsPerioderTjeneste(behandlingId);

        List<VilkårPeriode> vilkårsperioder = new ArrayList<>();
        for (VilkårType vilkårType : vilkårsPerioderTilVurderingTjeneste.definerendeVilkår()) {
            vilkårene.getVilkår(vilkårType).map(Vilkår::getPerioder).ifPresent(vilkårsperioder::addAll);
        }

        return vilkårsperioder.stream()
            .filter(it -> perioderTilVurdering.stream().anyMatch(at -> at.overlapper(it.getPeriode())))
            .filter(it -> Utfall.OPPFYLT.equals(it.getUtfall()))
            .collect(Collectors.toSet());
    }

    private VilkårsPerioderTilVurderingTjeneste finnVilkårsPerioderTjeneste(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        return VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, behandling.getFagsakYtelseType(), behandling.getType());
    }
}
