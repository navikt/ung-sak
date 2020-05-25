package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.PermisjonsbeskrivelseType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.k9.sak.domene.iay.modell.VersjonType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.Stillingsprosent;
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

        BehandlingReferanse behandlingReferanse = opprettRef(AktørId.dummy());
        var perioder = new MapOppgittFraværOgVilkårsResultat().utledPerioderMedUtfall(behandlingReferanse, oppgittFravær, new InntektArbeidYtelseGrunnlag(UUID.randomUUID(), LocalDateTime.now()), vilkårene);

        assertThat(perioder).hasSize(1);
        for (Map.Entry<Aktivitet, List<WrappedOppgittFraværPeriode>> entries : perioder.entrySet()) {
            assertThat(entries.getValue()).hasSize(2);
            assertThat(entries.getValue().stream().filter(WrappedOppgittFraværPeriode::getErAvslått)).hasSize(1);
            assertThat(entries.getValue().stream().filter(it -> !it.getErAvslått())).hasSize(1);
        }
    }

    @NotNull
    private BehandlingReferanse opprettRef(AktørId dummy) {
        return BehandlingReferanse.fra(FagsakYtelseType.OMP, null, null, dummy, null, null, null, null, null, java.util.Optional.empty(), null, null);
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

        var perioder = new MapOppgittFraværOgVilkårsResultat().utledPerioderMedUtfall(opprettRef(AktørId.dummy()), oppgittFravær, new InntektArbeidYtelseGrunnlag(UUID.randomUUID(), LocalDateTime.now()), vilkårene);

        assertThat(perioder).hasSize(1);
        for (Map.Entry<Aktivitet, List<WrappedOppgittFraværPeriode>> entries : perioder.entrySet()) {
            assertThat(entries.getValue()).hasSize(2);
            assertThat(entries.getValue().stream().filter(WrappedOppgittFraværPeriode::getErAvslått)).hasSize(1);
            assertThat(entries.getValue().stream().filter(it -> !it.getErAvslått())).hasSize(1);
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

        var perioder = new MapOppgittFraværOgVilkårsResultat().utledPerioderMedUtfall(opprettRef(AktørId.dummy()), oppgittFravær, new InntektArbeidYtelseGrunnlag(UUID.randomUUID(), LocalDateTime.now()), vilkårene);

        assertThat(perioder).hasSize(2);
        var fraværPerioder = perioder.get(new Aktivitet(Arbeidsgiver.virksomhet("000000000"), InternArbeidsforholdRef.nullRef()));
        var fraværPerioder1 = perioder.get(new Aktivitet(Arbeidsgiver.virksomhet("000000001"), InternArbeidsforholdRef.nullRef()));
        assertThat(fraværPerioder).hasSize(2);
        assertThat(fraværPerioder.stream().filter(WrappedOppgittFraværPeriode::getErAvslått)).hasSize(1);
        assertThat(fraværPerioder.stream().filter(it -> !it.getErAvslått())).hasSize(1);
        assertThat(fraværPerioder1).hasSize(1);
        assertThat(fraværPerioder1.stream().filter(WrappedOppgittFraværPeriode::getErAvslått)).hasSize(0);
        assertThat(fraværPerioder1.stream().filter(it -> !it.getErAvslått())).hasSize(1);
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

        var perioder = new MapOppgittFraværOgVilkårsResultat().utledPerioderMedUtfall(opprettRef(AktørId.dummy()), oppgittFravær, new InntektArbeidYtelseGrunnlag(UUID.randomUUID(), LocalDateTime.now()), vilkårene);

        assertThat(perioder).hasSize(1);
        for (Map.Entry<Aktivitet, List<WrappedOppgittFraværPeriode>> entries : perioder.entrySet()) {
            assertThat(entries.getValue()).hasSize(2);
            assertThat(entries.getValue().stream().filter(WrappedOppgittFraværPeriode::getErAvslått)).hasSize(1);
            assertThat(entries.getValue().stream().filter(it -> !it.getErAvslått())).hasSize(1);
        }
    }

    @Test
    public void skal_ta_hensyn_til_avslått_arbeidsforhold() {
        var vilkårResultatBuilder = new VilkårResultatBuilder();
        var dummy = AktørId.dummy();

        var vilkårene = vilkårResultatBuilder.build();

        var arbeidsgiver = Arbeidsgiver.virksomhet("123123123");
        var oppgittFravær = new OppgittFravær(new OppgittFraværPeriode(LocalDate.now().minusDays(10), LocalDate.now(), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, InternArbeidsforholdRef.nullRef(), Duration.ofHours(8)),
            new OppgittFraværPeriode(LocalDate.now().minusDays(30), LocalDate.now().minusDays(10), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, InternArbeidsforholdRef.nullRef(), Duration.ofHours(8)));
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt();
        var iayBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var aktørArbeidBuilder = iayBuilder.getAktørArbeidBuilder(dummy);
        var yaBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        iayBuilder.leggTilAktørArbeid(aktørArbeidBuilder
            .leggTilYrkesaktivitet(yaBuilder
                .medArbeidsgiver(arbeidsgiver)
                .leggTilAktivitetsAvtale(yaBuilder.getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(30), LocalDate.now().minusDays(20)), true))
                .leggTilAktivitetsAvtale(yaBuilder.getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(30), LocalDate.now().minusDays(20)), false)
                    .medProsentsats(Stillingsprosent.HUNDRED)
                    .medBeskrivelse("asd")
                    .medSisteLønnsendringsdato(LocalDate.now().minusDays(30)))));
        iayGrunnlag.medData(iayBuilder);

        var build = iayGrunnlag.build();
        var perioder = new MapOppgittFraværOgVilkårsResultat().utledPerioderMedUtfall(opprettRef(dummy), oppgittFravær, build, vilkårene);

        assertThat(perioder).hasSize(1);
        for (Map.Entry<Aktivitet, List<WrappedOppgittFraværPeriode>> entries : perioder.entrySet()) {
            assertThat(entries.getValue()).hasSize(2);
            assertThat(entries.getValue().stream().filter(WrappedOppgittFraværPeriode::getErAvslått)).hasSize(1);
            assertThat(entries.getValue().stream().filter(it -> !it.getErAvslått())).hasSize(1);
        }
    }

    @Test
    public void skal_ta_hensyn_til_permisjon() {
        var vilkårResultatBuilder = new VilkårResultatBuilder();
        var dummy = AktørId.dummy();

        var vilkårene = vilkårResultatBuilder.build();

        var arbeidsgiver = Arbeidsgiver.virksomhet("123123123");
        var oppgittFravær = new OppgittFravær(new OppgittFraværPeriode(LocalDate.now().minusDays(10), LocalDate.now(), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, InternArbeidsforholdRef.nullRef(), Duration.ofHours(8)),
            new OppgittFraværPeriode(LocalDate.now().minusDays(30), LocalDate.now().minusDays(10), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, InternArbeidsforholdRef.nullRef(), Duration.ofHours(8)));
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt();
        var iayBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var aktørArbeidBuilder = iayBuilder.getAktørArbeidBuilder(dummy);
        var yaBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        iayBuilder.leggTilAktørArbeid(aktørArbeidBuilder
            .leggTilYrkesaktivitet(yaBuilder
                .medArbeidsgiver(arbeidsgiver)
                .leggTilPermisjon(yaBuilder.getPermisjonBuilder()
                    .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.PERMITTERING)
                    .medProsentsats(BigDecimal.valueOf(100L))
                    .medPeriode(LocalDate.now().minusDays(10), LocalDate.now())
                    .build())
                .leggTilAktivitetsAvtale(yaBuilder.getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMed(LocalDate.now().minusDays(30)), true))
                .leggTilAktivitetsAvtale(yaBuilder.getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMed(LocalDate.now().minusDays(30)), false)
                    .medProsentsats(Stillingsprosent.HUNDRED)
                    .medBeskrivelse("asd")
                    .medSisteLønnsendringsdato(LocalDate.now().minusDays(30)))));
        iayGrunnlag.medData(iayBuilder);

        var build = iayGrunnlag.build();
        var perioder = new MapOppgittFraværOgVilkårsResultat().utledPerioderMedUtfall(opprettRef(dummy), oppgittFravær, build, vilkårene);

        assertThat(perioder).hasSize(1);
        for (Map.Entry<Aktivitet, List<WrappedOppgittFraværPeriode>> entries : perioder.entrySet()) {
            assertThat(entries.getValue()).hasSize(2);
            assertThat(entries.getValue().stream().filter(WrappedOppgittFraværPeriode::getErAvslått)).hasSize(1);
            assertThat(entries.getValue().stream().filter(it -> !it.getErAvslått())).hasSize(1);
        }
    }

    @Test
    public void skal_ta_hensyn_til_permisjon_under_100_prosent() {
        var vilkårResultatBuilder = new VilkårResultatBuilder();
        var dummy = AktørId.dummy();

        var vilkårene = vilkårResultatBuilder.build();

        var arbeidsgiver = Arbeidsgiver.virksomhet("123123123");
        var oppgittFravær = new OppgittFravær(new OppgittFraværPeriode(LocalDate.now().minusDays(10), LocalDate.now(), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, InternArbeidsforholdRef.nullRef(), Duration.ofHours(8)),
            new OppgittFraværPeriode(LocalDate.now().minusDays(30), LocalDate.now().minusDays(10), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, InternArbeidsforholdRef.nullRef(), Duration.ofHours(8)));
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt();
        var iayBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var aktørArbeidBuilder = iayBuilder.getAktørArbeidBuilder(dummy);
        var yaBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        iayBuilder.leggTilAktørArbeid(aktørArbeidBuilder
            .leggTilYrkesaktivitet(yaBuilder
                .medArbeidsgiver(arbeidsgiver)
                .leggTilPermisjon(yaBuilder.getPermisjonBuilder()
                    .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.PERMITTERING)
                    .medProsentsats(BigDecimal.valueOf(99L))
                    .medPeriode(LocalDate.now().minusDays(10), LocalDate.now())
                    .build())
                .leggTilAktivitetsAvtale(yaBuilder.getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMed(LocalDate.now().minusDays(30)), true))
                .leggTilAktivitetsAvtale(yaBuilder.getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMed(LocalDate.now().minusDays(30)), false)
                    .medProsentsats(Stillingsprosent.HUNDRED)
                    .medBeskrivelse("asd")
                    .medSisteLønnsendringsdato(LocalDate.now().minusDays(30)))));
        iayGrunnlag.medData(iayBuilder);

        var build = iayGrunnlag.build();
        var perioder = new MapOppgittFraværOgVilkårsResultat().utledPerioderMedUtfall(opprettRef(dummy), oppgittFravær, build, vilkårene);

        assertThat(perioder).hasSize(1);
        for (Map.Entry<Aktivitet, List<WrappedOppgittFraværPeriode>> entries : perioder.entrySet()) {
            assertThat(entries.getValue()).hasSize(1);
            assertThat(entries.getValue().stream().filter(WrappedOppgittFraværPeriode::getErAvslått)).hasSize(0);
            assertThat(entries.getValue().stream().filter(it -> !it.getErAvslått())).hasSize(1);
        }
    }
}
