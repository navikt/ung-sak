package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
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

        assertThat(perioder).hasSize(1);
        for (Map.Entry<Aktivitet, Set<WrappedOppgittFraværPeriode>> entries : perioder.entrySet()) {
            assertThat(entries.getValue()).hasSize(2);
            assertThat(entries.getValue().stream().filter(WrappedOppgittFraværPeriode::erAvslått)).hasSize(1);
            assertThat(entries.getValue().stream().filter(it -> !it.erAvslått())).hasSize(1);
        }
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

        assertThat(perioder).hasSize(1);
        for (Map.Entry<Aktivitet, Set<WrappedOppgittFraværPeriode>> entries : perioder.entrySet()) {
            assertThat(entries.getValue()).hasSize(2);
            assertThat(entries.getValue().stream().filter(WrappedOppgittFraværPeriode::erAvslått)).hasSize(1);
            assertThat(entries.getValue().stream().filter(it -> !it.erAvslått())).hasSize(1);
        }
    }

    @Test
    public void skal_ta_hensyn_overlapp_flere_arbeidsgivere() {
        var vilkårResultatBuilder = new VilkårResultatBuilder();
        vilkårResultatBuilder
            .leggTil(vilkårResultatBuilder.hentBuilderFor(VilkårType.OPPTJENINGSVILKÅRET)
                .leggTil(new VilkårPeriodeBuilder().medUtfall(Utfall.IKKE_OPPFYLT).medPeriode(LocalDate.now().minusDays(10), LocalDate.now())))
            .leggTil(vilkårResultatBuilder.hentBuilderFor(VilkårType.MEDLEMSKAPSVILKÅRET)
                .leggTil(new VilkårPeriodeBuilder().medUtfall(Utfall.IKKE_OPPFYLT).medPeriode(LocalDate.now().minusDays(15), LocalDate.now())));

        var vilkårene = vilkårResultatBuilder.build();

        var oppgittFravær = new OppgittFravær(new OppgittFraværPeriode(LocalDate.now().minusDays(20), LocalDate.now(), UttakArbeidType.ARBEIDSTAKER, Arbeidsgiver.virksomhet("000000000"), InternArbeidsforholdRef.nullRef(), Duration.ofHours(8)),
            new OppgittFraværPeriode(LocalDate.now().minusDays(30), LocalDate.now().minusDays(20), UttakArbeidType.ARBEIDSTAKER, Arbeidsgiver.virksomhet("000000001"), InternArbeidsforholdRef.nullRef(), Duration.ofHours(8)));

        var perioder = new MapOppgittFraværOgVilkårsResultat().utledPerioderMedUtfallHvisAvslåttVilkår(oppgittFravær, vilkårene);

        assertThat(perioder).hasSize(2);
        var fraværPerioder = perioder.get(new Aktivitet(UttakArbeidType.ARBEIDSTAKER, Arbeidsgiver.virksomhet("000000000"), InternArbeidsforholdRef.nullRef()));
        var fraværPerioder1 = perioder.get(new Aktivitet(UttakArbeidType.ARBEIDSTAKER, Arbeidsgiver.virksomhet("000000001"), InternArbeidsforholdRef.nullRef()));
        assertThat(fraværPerioder).hasSize(2);
        assertThat(fraværPerioder.stream().filter(WrappedOppgittFraværPeriode::erAvslått)).hasSize(1);
        assertThat(fraværPerioder.stream().filter(it -> !it.erAvslått())).hasSize(1);
        assertThat(fraværPerioder1).hasSize(1);
        assertThat(fraværPerioder1.stream().filter(WrappedOppgittFraværPeriode::erAvslått)).hasSize(0);
        assertThat(fraværPerioder1.stream().filter(it -> !it.erAvslått())).hasSize(1);
    }

    @Test
    public void skal_ta_hensyn_til_overlapp_i_søkte_perioder() {
        var vilkårResultatBuilder = new VilkårResultatBuilder();
        vilkårResultatBuilder
            .leggTil(vilkårResultatBuilder.hentBuilderFor(VilkårType.OPPTJENINGSVILKÅRET)
                .leggTil(new VilkårPeriodeBuilder().medUtfall(Utfall.IKKE_OPPFYLT).medPeriode(LocalDate.now().minusDays(10), LocalDate.now())))
            .leggTil(vilkårResultatBuilder.hentBuilderFor(VilkårType.MEDLEMSKAPSVILKÅRET)
                .leggTil(new VilkårPeriodeBuilder().medUtfall(Utfall.IKKE_OPPFYLT).medPeriode(LocalDate.now().minusDays(15), LocalDate.now())));

        var vilkårene = vilkårResultatBuilder.build();

        var oppgittFravær = new OppgittFravær(new OppgittFraværPeriode(LocalDate.now().minusDays(10), LocalDate.now(), UttakArbeidType.ARBEIDSTAKER, Duration.ofHours(8)),
            new OppgittFraværPeriode(LocalDate.now().minusDays(30), LocalDate.now().minusDays(10), UttakArbeidType.ARBEIDSTAKER, Duration.ofHours(8)));

        var perioder = new MapOppgittFraværOgVilkårsResultat().utledPerioderMedUtfallHvisAvslåttVilkår(oppgittFravær, vilkårene);

        assertThat(perioder).hasSize(1);
        for (Map.Entry<Aktivitet, Set<WrappedOppgittFraværPeriode>> entries : perioder.entrySet()) {
            assertThat(entries.getValue()).hasSize(2);
            assertThat(entries.getValue().stream().filter(WrappedOppgittFraværPeriode::erAvslått)).hasSize(1);
            assertThat(entries.getValue().stream().filter(it -> !it.erAvslått())).hasSize(1);
        }
    }
}
