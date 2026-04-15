package no.nav.ung.sak.vilkår;

import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VilkårPeriodeFilterTest {


    public static final VilkårType VILKÅR_TYPE = VilkårType.BISTANDSVILKÅR;

    @Test
    void skal_ikke_filtere_bort_periode_til_vurdering() {
        // Arrange
        var vilkårResultatRepository = mock(VilkårResultatRepository.class);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now());
        when(vilkårResultatRepository.hentHvisEksisterer(any())).thenReturn(Optional.of(lagIkkeVurdertVilkår(periode)));


        var filter = new VilkårPeriodeFilter(behandlingReferanse(), vilkårResultatRepository);

        filter.ignorerIkkeRelevantePerioder();

        // Act
        var perioder = filter.filtrerPerioder(List.of(periode), VILKÅR_TYPE);


        // Assert
        assertThat(perioder.size()).isEqualTo(1);

    }

    @Test
    void skal_filtere_bort_ikke_relevant_periode() {
        // Arrange
        var vilkårResultatRepository = mock(VilkårResultatRepository.class);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now());
        when(vilkårResultatRepository.hentHvisEksisterer(any())).thenReturn(Optional.of(lagIkkeRelevantVilkår(periode)));


        var filter = new VilkårPeriodeFilter(behandlingReferanse(), vilkårResultatRepository);


        // Act
        filter.ignorerIkkeRelevantePerioder();
        var perioder = filter.filtrerPerioder(List.of(periode), VILKÅR_TYPE);


        // Assert
        assertThat(perioder.size()).isEqualTo(0);
    }

    @Test
    void skal_ikke_filtere_avslått_periode() {
        // Arrange
        var vilkårResultatRepository = mock(VilkårResultatRepository.class);
        var ikkeRelevantPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now());
        var avslåttPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().plusDays(10), LocalDate.now().plusDays(11));
        when(vilkårResultatRepository.hentHvisEksisterer(any())).thenReturn(Optional.of(lagIkkeRelevantOgAvslåttVilkår(ikkeRelevantPeriode, avslåttPeriode)));

        var filter = new VilkårPeriodeFilter(behandlingReferanse(), vilkårResultatRepository);


        // Act
        filter.ignorerIkkeRelevantePerioder();
        var perioder = filter.filtrerPerioder(List.of(ikkeRelevantPeriode, avslåttPeriode), VILKÅR_TYPE);


        // Assert
        assertThat(perioder.size()).isEqualTo(1);
    }

    @Test
    void skal_filtere_bort_ikke_relevant_periode_og_avslått_periode() {
        // Arrange
        var vilkårResultatRepository = mock(VilkårResultatRepository.class);
        var ikkeRelevantPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now());
        var avslåttPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().plusDays(10), LocalDate.now().plusDays(11));

        when(vilkårResultatRepository.hentHvisEksisterer(any())).thenReturn(Optional.of(lagIkkeRelevantOgAvslåttVilkår(ikkeRelevantPeriode, avslåttPeriode)));

        var filter = new VilkårPeriodeFilter(behandlingReferanse(), vilkårResultatRepository);


        // Act
        filter.ignorerIkkeRelevantePerioder();
        filter.ignorerAvslåttePerioder();
        var perioder = filter.filtrerPerioder(List.of(ikkeRelevantPeriode, avslåttPeriode), VILKÅR_TYPE);


        // Assert
        assertThat(perioder.size()).isEqualTo(0);
    }

    private static BehandlingReferanse behandlingReferanse() {
        var behandlingReferanse = mock(BehandlingReferanse.class);
        when(behandlingReferanse.getBehandlingId()).thenReturn(1L);
        return behandlingReferanse;
    }


    private static Vilkårene lagIkkeVurdertVilkår(DatoIntervallEntitet periode) {
        var ikkeVurdertBGVilkår = Vilkårene.builder().leggTilIkkeVurderteVilkår(List.of(periode), VILKÅR_TYPE).build();
        return ikkeVurdertBGVilkår;
    }

    private static Vilkårene lagIkkeRelevantVilkår(DatoIntervallEntitet periode) {
        var vilkåreneBuilder = Vilkårene.builder();
        var vilkårBuilder = vilkåreneBuilder.hentBuilderFor(VILKÅR_TYPE);
        vilkårBuilder.leggTil(lagPeriodeMedUtfall(periode, vilkårBuilder, Utfall.IKKE_RELEVANT));
        vilkåreneBuilder.leggTil(vilkårBuilder);
        return vilkåreneBuilder.build();
    }

    private static Vilkårene lagIkkeRelevantOgAvslåttVilkår(DatoIntervallEntitet ikkeRelevantPeriode, DatoIntervallEntitet avslåttPeriode) {
        var vilkåreneBuilder = Vilkårene.builder();
        var vilkårBuilder = vilkåreneBuilder.hentBuilderFor(VILKÅR_TYPE);
        vilkårBuilder.leggTil(lagPeriodeMedUtfall(ikkeRelevantPeriode, vilkårBuilder, Utfall.IKKE_RELEVANT));
        vilkårBuilder.leggTil(lagPeriodeMedUtfall(avslåttPeriode, vilkårBuilder, Utfall.IKKE_OPPFYLT));
        vilkåreneBuilder.leggTil(vilkårBuilder);
        return vilkåreneBuilder.build();
    }

    private static VilkårPeriodeBuilder lagPeriodeMedUtfall(DatoIntervallEntitet ikkeRelevantPeriode, VilkårBuilder vilkårBuilder, Utfall utfall) {
        var ikkeRelevantPeriodeBuilder = vilkårBuilder.hentBuilderFor(ikkeRelevantPeriode.getFomDato(), ikkeRelevantPeriode.getTomDato());
        ikkeRelevantPeriodeBuilder.medUtfall(utfall);
        ikkeRelevantPeriodeBuilder.medPeriode(ikkeRelevantPeriode);
        return ikkeRelevantPeriodeBuilder;
    }

}
