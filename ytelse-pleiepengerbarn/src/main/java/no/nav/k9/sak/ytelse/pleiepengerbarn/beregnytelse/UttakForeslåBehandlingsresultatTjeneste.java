package no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.revurdering.ytelse.RevurderingBehandlingsresultatutleder;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.behandling.steg.foreslåresultat.ForeslåBehandlingsresultatTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@FagsakYtelseTypeRef("PSB")
@FagsakYtelseTypeRef("PPN")
@ApplicationScoped
public class UttakForeslåBehandlingsresultatTjeneste extends ForeslåBehandlingsresultatTjeneste {

    private BehandlingRepository behandlingRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;

    UttakForeslåBehandlingsresultatTjeneste() {
        // for proxy
    }

    @Inject
    public UttakForeslåBehandlingsresultatTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                                   VedtakVarselRepository vedtakVarselRepository,
                                                   @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester,
                                                   @FagsakYtelseTypeRef RevurderingBehandlingsresultatutleder revurderingBehandlingsresultatutleder) {
        super(repositoryProvider, vedtakVarselRepository, revurderingBehandlingsresultatutleder);
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårsPerioderTilVurderingTjenester = vilkårsPerioderTilVurderingTjenester;
    }

    @Override
    protected DatoIntervallEntitet getMaksPeriode(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste = finnVilkårsperioderTilVurderingTjeneste(behandling);

        var definerendeVilkår = vilkårsPerioderTilVurderingTjeneste.definerendeVilkår();
        var timeline = new LocalDateTimeline<Boolean>(List.of());

        for (VilkårType vilkårType : definerendeVilkår) {
            timeline = timeline.combine(new LocalDateTimeline<>(vilkårsPerioderTilVurderingTjeneste.utled(behandlingId, vilkårType)
                .stream()
                .map(it -> new LocalDateSegment<>(it.getFomDato(), it.getTomDato(), true))
                .toList()), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        timeline.compress();
        if (timeline.isEmpty()) {
            return behandling.getFagsak().getPeriode();
        }
        return DatoIntervallEntitet.fraOgMedTilOgMed(timeline.getMinLocalDate(), timeline.getMaxLocalDate());
    }

    @Override
    protected boolean skalBehandlingenSettesTilAvslått(BehandlingReferanse ref, Vilkårene vilkårene) {
        if (skalAvslåsBasertPåAndreForhold(ref)) {
            return true;
        }
        Behandling behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        var harIngenPerioderForSykdomsvilkår = harIngenPerioderForSykdomsvilkår(behandling, vilkårene);
        if (harIngenPerioderForSykdomsvilkår) {
            return true;
        }

        final var maksPeriode = getMaksPeriode(ref.getBehandlingId());
        final var vilkårTidslinjer = vilkårene.getVilkårTidslinjer(maksPeriode);

        final var avslåtteVilkår = vilkårTidslinjer.entrySet().stream()
            .filter(e -> harAvslåtteVilkårsPerioder(e.getValue())
                && harIngenOppfylteVilkårsPerioder(e.getValue())
            )
            .map(Map.Entry::getKey)
            .toList();

        if (avslåtteVilkår.isEmpty()) {
            return false;
        }

        Set<VilkårType> sykdomVilkårTyper = sykdomVilkårTyper(behandling);
        boolean harAvslagForVilkårSomIkkeErSykdomsvilkår = avslåtteVilkår.stream().anyMatch(v -> !sykdomVilkårTyper.contains(v));
        if (harAvslagForVilkårSomIkkeErSykdomsvilkår) {
            return true;
        }

        return sykdomVilkårTyper.stream()
            .allMatch(vilkårtype -> harIngenOppfylteVilkårsPerioder(vilkårTidslinjer.get(vilkårtype)));
    }

    private boolean harIngenPerioderForSykdomsvilkår(Behandling behandling, Vilkårene vilkårene) {
        return sykdomVilkårTyper(behandling)
            .stream()
            .allMatch(it -> harIngenPerioder(it, vilkårene));
    }

    private boolean harIngenPerioder(VilkårType vilkårType, Vilkårene vilkårene) {
        return vilkårene.getVilkår(vilkårType).map(Vilkår::getPerioder).orElse(List.of()).isEmpty();
    }

    private Set<VilkårType> sykdomVilkårTyper(Behandling behandling) {
        return finnVilkårsperioderTilVurderingTjeneste(behandling).definerendeVilkår();
    }

    private VilkårsPerioderTilVurderingTjeneste finnVilkårsperioderTilVurderingTjeneste(Behandling behandling) {
        return VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjenester, behandling.getFagsakYtelseType(), behandling.getType());
    }
}
