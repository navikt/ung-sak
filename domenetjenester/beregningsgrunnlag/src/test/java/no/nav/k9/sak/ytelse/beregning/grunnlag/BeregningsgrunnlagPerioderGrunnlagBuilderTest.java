package no.nav.k9.sak.ytelse.beregning.grunnlag;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.Test;

import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;

public class BeregningsgrunnlagPerioderGrunnlagBuilderTest {

    @Test
    public void skal_oppføre_seg() {
        var beregningsgrunnlag = new BeregningsgrunnlagPeriode(UUID.randomUUID(), LocalDate.now());

        var vilkårBuilder = new VilkårBuilder(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        var periodeBuilder = vilkårBuilder.hentBuilderFor(LocalDate.now(), LocalDate.now().plusDays(3))
            .medUtfall(Utfall.IKKE_VURDERT);

        vilkårBuilder.leggTil(periodeBuilder);
        var vilkårene = new VilkårResultatBuilder().leggTil(vilkårBuilder).build();


        var vilkår = vilkårene.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow();
        var grunnlag = new BeregningsgrunnlagPerioderGrunnlagBuilder(null)
            .leggTil(beregningsgrunnlag)
            .validerMotVilkår(vilkår)
            .build();

        assertThat(grunnlag).isNotNull();
        assertThat(grunnlag.getGrunnlagPerioder()).hasSize(1);

        var oppdateringsBuilder = new BeregningsgrunnlagPerioderGrunnlagBuilder(grunnlag);
        var oppdatertResultat = oppdateringsBuilder.deaktiver(LocalDate.now())
            .validerMotVilkår(vilkår)
            .build();

        assertThat(oppdatertResultat).isNotNull();
        assertThat(oppdatertResultat.getGrunnlagPerioder()).hasSize(0);
        assertThat(grunnlag.getGrunnlagPerioder()).hasSize(1);
    }
}
