package no.nav.ung.sak.domene.behandling.steg.foreslåresultat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.tid.DatoIntervallEntitet;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

import java.util.List;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

@FagsakYtelseTypeRef
@ApplicationScoped
public class DefaultForeslåBehandlingsresultatTjeneste extends ForeslåBehandlingsresultatTjeneste {

    private BehandlingRepository behandlingRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;

    DefaultForeslåBehandlingsresultatTjeneste() {
        // for proxy
    }

    @Inject
    public DefaultForeslåBehandlingsresultatTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                                     @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester) {
        super(repositoryProvider);
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårsPerioderTilVurderingTjenester = vilkårsPerioderTilVurderingTjenester;
    }

    @Override
    protected DatoIntervallEntitet getMaksPeriode(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjenester, behandling.getFagsakYtelseType(), behandling.getType());
        var definerendeVilkår = vilkårsPerioderTilVurderingTjeneste.definerendeVilkår();
        var timeline = new LocalDateTimeline<Boolean>(List.of());

        for (VilkårType vilkårType : definerendeVilkår) {
            timeline = timeline.combine(new LocalDateTimeline<>(vilkårsPerioderTilVurderingTjeneste.utled(behandlingId, vilkårType)
                .stream()
                .map(it -> new LocalDateSegment<>(it.getFomDato(), it.getTomDato(), true))
                .toList()), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        if (timeline.isEmpty()) {
            return behandling.getFagsak().getPeriode();
        }
        return DatoIntervallEntitet.fraOgMedTilOgMed(timeline.getMinLocalDate(), timeline.getMaxLocalDate());
    }

    @Override
    protected boolean skalBehandlingenSettesTilAvslått(BehandlingReferanse ref, Vilkårene vilkårene) {
        var maksPeriode = getMaksPeriode(ref.getBehandlingId());
        var vilkårTidslinjer = vilkårene.getVilkårTidslinjer(maksPeriode);
        return vilkårTidslinjer.entrySet().stream()
            .anyMatch(e -> harAvslåtteVilkårsPerioder(e.getValue())
                && harIngenOppfylteVilkårsPerioder(e.getValue())
            );
    }


}
