package no.nav.ung.sak.web.app.tjenester.behandling.vilkår;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

class VilkårDtoMapperTest {

    private static final LocalDate FOM = LocalDate.of(2025, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2025, 1, 31);

    @Test
    void periode_vurderes_i_behandlingen_når_ytelsen_ikke_har_tidslinje_for_perioder_til_vurdering() {
        var dto = mapVilkår(Map.of());

        assertThat(dto).isTrue();
    }

    @Test
    void periode_vurderes_ikke_når_den_ikke_overlapper_perioder_til_vurdering() {
        var tilVurdering = new LocalDateTimeline<>(FOM.minusMonths(6), FOM.minusMonths(5), Boolean.TRUE);

        var dto = mapVilkår(Map.of(VilkårType.ALDERSVILKÅR, tilVurdering));

        assertThat(dto).isFalse();
    }

    @Test
    void periode_vurderes_når_den_overlapper_perioder_til_vurdering() {
        var tilVurdering = new LocalDateTimeline<>(FOM, FOM.plusDays(5), Boolean.TRUE);

        var dto = mapVilkår(Map.of(VilkårType.ALDERSVILKÅR, tilVurdering));

        assertThat(dto).isTrue();
    }

    private boolean mapVilkår(Map<VilkårType, LocalDateTimeline<Boolean>> perioderTilVurdering) {
        var behandling = mock(Behandling.class);
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.UNGDOMSYTELSE);
        var vilkårene = Vilkårene.builder()
            .leggTil(new VilkårBuilder(VilkårType.ALDERSVILKÅR)
                .leggTil(new VilkårPeriodeBuilder().medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM)).medUtfall(Utfall.OPPFYLT)))
            .build();

        var resultat = VilkårDtoMapper.lagVilkarMedPeriodeDto(behandling, false, vilkårene, perioderTilVurdering);

        return resultat.get(0).getPerioder().get(0).getVurderesIBehandlingen();
    }
}
