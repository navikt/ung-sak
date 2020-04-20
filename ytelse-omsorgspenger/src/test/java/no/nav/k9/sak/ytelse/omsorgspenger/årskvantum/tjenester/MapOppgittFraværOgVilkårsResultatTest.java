package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;

import org.junit.Test;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFravær;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

public class MapOppgittFraværOgVilkårsResultatTest {

    @Test
    public void skal_ta_hensyn_til_avslåtte_vilkår() {
        var vilkårResultatBuilder = new VilkårResultatBuilder();
        vilkårResultatBuilder.leggTil(vilkårResultatBuilder.hentBuilderFor(VilkårType.OPPTJENINGSVILKÅRET)
            .leggTil(new VilkårPeriodeBuilder().medUtfall(Utfall.IKKE_OPPFYLT).medPeriode(LocalDate.now().minusDays(10), LocalDate.now())));

        var vilkårene = vilkårResultatBuilder.build();

        var oppgittFravær = new OppgittFravær(new OppgittFraværPeriode(LocalDate.now().minusDays(20), LocalDate.now(), UttakArbeidType.ARBEIDSTAKER, Duration.ofHours(8)));

        var perioder = new MapOppgittFraværOgVilkårsResultat().utledPerioderMedUtfallHvisAvslåttVilkår(oppgittFravær, vilkårene);

        assertThat(perioder).hasSize(2);
        assertThat(perioder.stream().filter(WrappedOppgittFraværPeriode::getErAvslått)).hasSize(1);
        assertThat(perioder.stream().filter(it -> !it.getErAvslått())).hasSize(1);
    }

    @Test
    public void skal_ta_hensyn_til_flere_avslåtte_vilkår() {
        var vilkårResultatBuilder = new VilkårResultatBuilder();
        vilkårResultatBuilder
            .leggTil(vilkårResultatBuilder.hentBuilderFor(VilkårType.OPPTJENINGSVILKÅRET)
                .leggTil(new VilkårPeriodeBuilder().medUtfall(Utfall.IKKE_OPPFYLT).medPeriode(LocalDate.now().minusDays(10), LocalDate.now())))
            .leggTil(vilkårResultatBuilder.hentBuilderFor(VilkårType.MEDLEMSKAPSVILKÅRET)
                .leggTil(new VilkårPeriodeBuilder().medUtfall(Utfall.IKKE_OPPFYLT).medPeriode(LocalDate.now().minusDays(15), LocalDate.now())));

        var vilkårene = vilkårResultatBuilder.build();

        var oppgittFravær = new OppgittFravær(new OppgittFraværPeriode(LocalDate.now().minusDays(10), LocalDate.now(), UttakArbeidType.ARBEIDSTAKER, Duration.ofHours(8)),
            new OppgittFraværPeriode(LocalDate.now().minusDays(30), LocalDate.now().minusDays(20), UttakArbeidType.ARBEIDSTAKER, Duration.ofHours(8)));

        var perioder = new MapOppgittFraværOgVilkårsResultat().utledPerioderMedUtfallHvisAvslåttVilkår(oppgittFravær, vilkårene);

        assertThat(perioder).hasSize(2);
        assertThat(perioder.stream().filter(WrappedOppgittFraværPeriode::getErAvslått)).hasSize(1);
        assertThat(perioder.stream().filter(it -> !it.getErAvslått())).hasSize(1);
    }
}
