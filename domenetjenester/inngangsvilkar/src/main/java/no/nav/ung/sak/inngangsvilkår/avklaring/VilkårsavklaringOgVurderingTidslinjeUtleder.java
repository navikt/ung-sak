package no.nav.ung.sak.inngangsvilkår.avklaring;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.VilkårsvurderingResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.domene.typer.tid.TidslinjeUtil;

import java.util.List;
import java.util.Map;

/**
 * Fletter vilkårsavklaringene på behandlingen (som styres av {@link BehandlingÅrsakType}) sammen med
 * vilkårsvurderingene (som er knyttet til {@link VilkårType}) til én tidslinje.
 * <p>
 * Utlederen sier ingenting om utfallet — avgrensning mot avslåtte eller oppfylte perioder gjøres av kalleren.
 */
@Dependent
public class VilkårsavklaringOgVurderingTidslinjeUtleder {

    private static final Map<VilkårType, BehandlingÅrsakType> VILKÅR_OG_BEHANDLINGSÅRSAK = Map.of(
        VilkårType.BOSTEDSVILKÅR, BehandlingÅrsakType.ENDRET_BOSTED
    );

    private final InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    private final Instance<VilkårsavklaringTjeneste> vilkårsavklaringTjenester;

    @Inject
    public VilkårsavklaringOgVurderingTidslinjeUtleder(InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository,
                                                       @Any Instance<VilkårsavklaringTjeneste> vilkårsavklaringTjenester) {
        this.inngangsvilkårVurderingRepository = inngangsvilkårVurderingRepository;
        this.vilkårsavklaringTjenester = vilkårsavklaringTjenester;
    }

    public LocalDateTimeline<VilkårsavklaringMedVurdering> utled(long behandlingId) {
        var vurderingTidslinje = inngangsvilkårVurderingRepository.hentVurderingTidslinje(behandlingId);

        return VILKÅR_OG_BEHANDLINGSÅRSAK.entrySet().stream()
            .map(entry -> lagTidslinjeForVilkår(behandlingId, vurderingTidslinje, entry.getKey(), entry.getValue()))
            .reduce(LocalDateTimeline.empty(), LocalDateTimeline::crossJoin);
    }

    private LocalDateTimeline<VilkårsavklaringMedVurdering> lagTidslinjeForVilkår(long behandlingId,
                                                                                  LocalDateTimeline<Map<VilkårType, VilkårsvurderingResultatPeriode>> vurderingTidslinje,
                                                                                  VilkårType vilkårType,
                                                                                  BehandlingÅrsakType behandlingÅrsakType) {
        var vilkårsavklaring = VilkårsavklaringTjeneste.finnForÅrsak(vilkårsavklaringTjenester, behandlingÅrsakType)
            .flatMap(it -> it.hentSenesteAvklaringForBehandling(behandlingId));
        if (vilkårsavklaring.isEmpty()) {
            return LocalDateTimeline.empty();
        }

        var avklartTidslinje = TidslinjeUtil.tilTidslinjeKomprimert(List.of(vilkårsavklaring.get().periode()));
        return avklartTidslinje.combine(vurderingTidslinje,
            (interval, _, vurdering) -> new LocalDateSegment<>(interval,
                new VilkårsavklaringMedVurdering(
                    vilkårType,
                    behandlingÅrsakType,
                    vilkårsavklaring.get(),
                    vurdering == null ? null : vurdering.getValue().get(vilkårType))),
            LocalDateTimeline.JoinStyle.LEFT_JOIN);
    }
}
