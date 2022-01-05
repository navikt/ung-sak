package no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.revurdering.ytelse.RevurderingBehandlingsresultatutleder;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
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

    private VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste;
    private BehandlingRepository behandlingRepository;

    UttakForeslåBehandlingsresultatTjeneste() {
        // for proxy
    }

    @Inject
    public UttakForeslåBehandlingsresultatTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                                   VedtakVarselRepository vedtakVarselRepository,
                                                   @FagsakYtelseTypeRef("PSB") @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste,
                                                   @FagsakYtelseTypeRef RevurderingBehandlingsresultatutleder revurderingBehandlingsresultatutleder) {
        super(repositoryProvider, vedtakVarselRepository, revurderingBehandlingsresultatutleder);
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    @Override
    protected DatoIntervallEntitet getMaksPeriode(Long behandlingId) {
        var definerendeVilkår = vilkårsPerioderTilVurderingTjeneste.definerendeVilkår();
        var timeline = new LocalDateTimeline<Boolean>(List.of());

        for (VilkårType vilkårType : definerendeVilkår) {
            timeline = timeline.combine(new LocalDateTimeline<>(vilkårsPerioderTilVurderingTjeneste.utled(behandlingId, vilkårType)
                .stream()
                .map(it -> new LocalDateSegment<>(it.getFomDato(), it.getTomDato(), true))
                .collect(Collectors.toList())), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        timeline.compress();
        if (timeline.isEmpty()) {
            return behandlingRepository.hentBehandling(behandlingId).getFagsak().getPeriode();
        }
        return DatoIntervallEntitet.fraOgMedTilOgMed(timeline.getMinLocalDate(), timeline.getMaxLocalDate());
    }

    @Override
    protected boolean skalBehandlingenSettesTilAvslått(BehandlingReferanse ref, Vilkårene vilkårene) {
        if (skalAvslåsBasertPåAndreForhold(ref)) {
            return true;
        }

        var harIngenPerioderForMedisinsk = harIngenPerioderForMedisinsk(vilkårene);
        if (harIngenPerioderForMedisinsk) {
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

        var definerendeVilkår = vilkårsPerioderTilVurderingTjeneste.definerendeVilkår();
        if (avslåtteVilkår.stream().anyMatch(v -> !definerendeVilkår.contains(v))) {
            return true;
        }

        final var ingenAvSykdomsvilkåreneErOppfylt = definerendeVilkår.stream().allMatch(v -> harIngenOppfylteVilkårsPerioder(vilkårTidslinjer.get(v)));

        return ingenAvSykdomsvilkåreneErOppfylt;
    }

    private boolean harIngenPerioderForMedisinsk(Vilkårene vilkårene) {
        return vilkårsPerioderTilVurderingTjeneste.definerendeVilkår()
            .stream()
            .allMatch(it -> harIngenPerioder(it, vilkårene));
    }

    private boolean harIngenPerioder(VilkårType vilkårType, Vilkårene vilkårene) {
        return vilkårene.getVilkår(vilkårType).map(Vilkår::getPerioder).orElse(List.of()).isEmpty();
    }
}
