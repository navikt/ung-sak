package no.nav.k9.sak.behandlingslager.behandling.opptjening;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetKlassifisering;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class OpptjeningResultatBuilderTest {

    @Test
    public void skal_oppføre_seg() {
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(29), LocalDate.now().minusDays(1));
        var opptjening = new Opptjening(LocalDate.now().minusDays(29), LocalDate.now().minusDays(1));
        opptjening.setOpptjentPeriode(Period.ofDays(28));
        opptjening.setOpptjeningAktivitet(Set.of(new OpptjeningAktivitet(periode.getFomDato(), periode.getTomDato(), OpptjeningAktivitetType.ARBEID, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT)));

        var vilkårBuilder = new VilkårBuilder(VilkårType.OPPTJENINGSVILKÅRET);
        var periodeBuilder = vilkårBuilder.hentBuilderFor(LocalDate.now(), LocalDate.now().plusDays(3))
            .medUtfall(Utfall.IKKE_VURDERT);

        vilkårBuilder.leggTil(periodeBuilder);
        var vilkårene = new VilkårResultatBuilder().leggTil(vilkårBuilder).build();


        var vilkår = vilkårene.getVilkår(VilkårType.OPPTJENINGSVILKÅRET).orElseThrow();
        var opptjeningResultat = new OpptjeningResultatBuilder(null)
            .leggTil(opptjening)
            .validerMotVilkår(vilkår)
            .build();

        assertThat(opptjeningResultat).isNotNull();
        assertThat(opptjeningResultat.getOpptjeningPerioder()).hasSize(1);

        var oppdateringsBuilder = new OpptjeningResultatBuilder(opptjeningResultat);
        var oppdatertResultat = oppdateringsBuilder.deaktiver(LocalDate.now())
            .validerMotVilkår(vilkår)
            .build();

        assertThat(oppdatertResultat).isNotNull();
        assertThat(oppdatertResultat.getOpptjeningPerioder()).hasSize(0);
        assertThat(opptjeningResultat.getOpptjeningPerioder()).hasSize(1);
    }
}
