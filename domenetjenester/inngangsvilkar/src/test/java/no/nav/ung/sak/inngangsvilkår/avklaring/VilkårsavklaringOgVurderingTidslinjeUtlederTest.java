package no.nav.ung.sak.inngangsvilkår.avklaring;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.BostedsavklaringKildeType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.VilkårsvurderingResultat;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.test.util.UnitTestLookupInstanceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VilkårsavklaringOgVurderingTidslinjeUtlederTest {

    private static final LocalDate FOM = LocalDate.of(2024, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2024, 1, 31);

    private final InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository = mock(InngangsvilkårVurderingRepository.class);
    private final VilkårsavklaringTjeneste vilkårsavklaringTjeneste = mock(VilkårsavklaringTjeneste.class);

    private final VilkårsavklaringOgVurderingTidslinjeUtleder utleder = new VilkårsavklaringOgVurderingTidslinjeUtleder(
        inngangsvilkårVurderingRepository,
        new UnitTestLookupInstanceImpl<>(vilkårsavklaringTjeneste));

    @Test
    void skal_kombinere_avklaring_og_vurdering_for_perioden_som_overlapper_og_beholde_avklaring_uten_vurdering_for_resten() {
        var behandlingId = 1L;
        var avklaringsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM);
        when(vilkårsavklaringTjeneste.hentSenesteAvklaringForBehandling(behandlingId))
            .thenReturn(Optional.of(new Vilkårsavklaring(Avklaringtype.AVSLAG, avklaringsperiode, BostedsavklaringKildeType.BRUKER, null)));

        // Vurdering finnes kun for første halvdel av perioden
        var vurdertTom = LocalDate.of(2024, 1, 15);
        var vurdering = enkelVurdering(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, vurdertTom));
        var vurderingTidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(FOM, vurdertTom, Map.of(VilkårType.BOSTEDSVILKÅR, vurdering))));
        when(inngangsvilkårVurderingRepository.hentVurderingTidslinje(behandlingId)).thenReturn(vurderingTidslinje);

        var resultat = utleder.utled(behandlingId);

        assertThat(resultat.getMinLocalDate()).isEqualTo(FOM);
        assertThat(resultat.getMaxLocalDate()).isEqualTo(TOM);

        var segmentMedVurdering = resultat.getSegment(new LocalDateInterval(FOM, vurdertTom));
        assertThat(segmentMedVurdering).isNotNull();
        var medVurdering = segmentMedVurdering.getValue().get(VilkårType.BOSTEDSVILKÅR);
        assertThat(medVurdering.vilkårsvurdering()).isEqualTo(vurdering);
        assertThat(medVurdering.behandlingÅrsakType()).isEqualTo(BehandlingÅrsakType.ENDRET_BOSTED);
        assertThat(medVurdering.vilkårsavklaring().periode()).isEqualTo(avklaringsperiode);

        var segmentUtenVurdering = resultat.getSegment(new LocalDateInterval(vurdertTom.plusDays(1), TOM));
        assertThat(segmentUtenVurdering).isNotNull();
        var utenVurdering = segmentUtenVurdering.getValue().get(VilkårType.BOSTEDSVILKÅR);
        assertThat(utenVurdering.vilkårsvurdering()).isNull();
        assertThat(utenVurdering.harVilkårsAvklaring()).isTrue();
    }

    @Test
    void skal_returnere_tom_tidslinje_naar_det_ikke_finnes_noen_avklaring() {
        var behandlingId = 2L;
        when(vilkårsavklaringTjeneste.hentSenesteAvklaringForBehandling(behandlingId)).thenReturn(Optional.empty());
        when(inngangsvilkårVurderingRepository.hentVurderingTidslinje(behandlingId)).thenReturn(LocalDateTimeline.empty());

        var resultat = utleder.utled(behandlingId);

        assertThat(resultat.isEmpty()).isTrue();
    }

    private VilkårsvurderingResultat enkelVurdering(DatoIntervallEntitet periode) {
        return new VilkårsvurderingResultat(VilkårType.BOSTEDSVILKÅR, true, null, null, null);
    }
}
