package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.PermisjonsbeskrivelseType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.uttak.FraværÅrsak;
import no.nav.k9.kodeverk.uttak.SøknadÅrsak;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.iay.modell.VersjonType;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetPeriode;
import no.nav.k9.sak.domene.opptjening.VurderingsStatus;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Stillingsprosent;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.AktivitetTypeArbeidsgiver;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.OppgittFraværHolder;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.OppgittFraværVerdi;

class MapOppgittFraværOgVilkårsResultatTest {

    private final DatoIntervallEntitet boundry = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(9));

    private Arbeidsgiver virksomhet1 = Arbeidsgiver.virksomhet("000000001");

    private MapOppgittFraværOgVilkårsResultat mapper = new MapOppgittFraværOgVilkårsResultat(true);

    @Test
    void skal_ta_hensyn_til_avslåtte_vilkår() {
        var vilkårResultatBuilder = new VilkårResultatBuilder();
        vilkårResultatBuilder.leggTil(vilkårResultatBuilder.hentBuilderFor(VilkårType.OPPTJENINGSVILKÅRET)
            .leggTil(new VilkårPeriodeBuilder().medUtfall(Utfall.IKKE_OPPFYLT).medPeriode(LocalDate.now().minusDays(10), LocalDate.now())));

        var vilkårene = vilkårResultatBuilder.build();

        LocalDate refusjonskravFom = LocalDate.now().minusDays(20);
        LocalDate refusjonskravTom = LocalDate.now();

        BehandlingReferanse behandlingReferanse = opprettRef(AktørId.dummy());
        var fraværperioder = fraværEttRefusjonskrav(virksomhet1, refusjonskravFom, refusjonskravTom, null);
        var perioder = mapper.utledPerioderMedUtfall(behandlingReferanse, new InntektArbeidYtelseGrunnlag(UUID.randomUUID(), LocalDateTime.now()), Collections.emptyNavigableMap(), vilkårene, boundry, fraværperioder);

        assertThat(perioder).hasSize(1);
        for (Map.Entry<Aktivitet, List<WrappedOppgittFraværPeriode>> entries : perioder.entrySet()) {
            assertThat(entries.getValue()).hasSize(2);
            assertThat(entries.getValue().stream().filter(it -> it.getErAvslåttInngangsvilkår() != null).filter(WrappedOppgittFraværPeriode::getErAvslåttInngangsvilkår)).hasSize(1);
            assertThat(entries.getValue().stream().filter(it -> it.getErIPermisjon() != null).filter(WrappedOppgittFraværPeriode::getErIPermisjon)).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getArbeidStatus() != null).filter(it -> ArbeidStatus.AVSLUTTET.equals(it.getArbeidStatus()))).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getArbeidStatus() != null).filter(it -> ArbeidStatus.IKKE_EKSISTERENDE.equals(it.getArbeidStatus()))).hasSize(2);
            assertThat(entries.getValue().stream().filter(it -> it.getErAvslåttInngangsvilkår() == null)).hasSize(1);
        }
    }

    private BehandlingReferanse opprettRef(AktørId dummy) {
        return BehandlingReferanse.fra(FagsakYtelseType.OMP, null, null, dummy, null, null, null, null, null, null, java.util.Optional.empty(), null, null, null);
    }

    @Test
    void skal_ta_hensyn_til_flere_avslåtte_vilkår() {
        var vilkårResultatBuilder = new VilkårResultatBuilder();
        vilkårResultatBuilder
            .leggTil(vilkårResultatBuilder.hentBuilderFor(VilkårType.OPPTJENINGSVILKÅRET)
                .leggTil(new VilkårPeriodeBuilder().medUtfall(Utfall.IKKE_OPPFYLT).medPeriode(LocalDate.now().minusDays(10), LocalDate.now())))
            .leggTil(vilkårResultatBuilder.hentBuilderFor(VilkårType.MEDLEMSKAPSVILKÅRET)
                .leggTil(new VilkårPeriodeBuilder().medUtfall(Utfall.IKKE_OPPFYLT).medPeriode(LocalDate.now().minusDays(15), LocalDate.now())));

        var vilkårene = vilkårResultatBuilder.build();

        List<LocalDateInterval> kravperioder = List.of(
            new LocalDateInterval(LocalDate.now().minusDays(10), LocalDate.now()),
            new LocalDateInterval(LocalDate.now().minusDays(30), LocalDate.now().minusDays(20))
        );
        var fraværperioder = fraværRefusjonskravHeleDager(virksomhet1, kravperioder);
        var perioder = mapper.utledPerioderMedUtfall(opprettRef(AktørId.dummy()), new InntektArbeidYtelseGrunnlag(UUID.randomUUID(), LocalDateTime.now()), Collections.emptyNavigableMap(), vilkårene, boundry, fraværperioder);

        assertThat(perioder).hasSize(1);
        for (Map.Entry<Aktivitet, List<WrappedOppgittFraværPeriode>> entries : perioder.entrySet()) {
            assertThat(entries.getValue()).hasSize(2);
            assertThat(entries.getValue().stream().filter(it -> it.getErAvslåttInngangsvilkår() != null).filter(WrappedOppgittFraværPeriode::getErAvslåttInngangsvilkår)).hasSize(1);
            assertThat(entries.getValue().stream().filter(it -> it.getErIPermisjon() != null).filter(WrappedOppgittFraværPeriode::getErIPermisjon)).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getArbeidStatus() != null).filter(it -> ArbeidStatus.AVSLUTTET.equals(it.getArbeidStatus()))).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getArbeidStatus() != null).filter(it -> ArbeidStatus.IKKE_EKSISTERENDE.equals(it.getArbeidStatus()))).hasSize(2);
            assertThat(entries.getValue().stream().filter(it -> it.getErAvslåttInngangsvilkår() == null)).hasSize(1);
        }
    }

    @Test
    void skal_ta_hensyn_overlapp_flere_arbeidsgivere() {
        var vilkårResultatBuilder = new VilkårResultatBuilder();
        vilkårResultatBuilder
            .leggTil(vilkårResultatBuilder.hentBuilderFor(VilkårType.OPPTJENINGSVILKÅRET)
                .leggTil(new VilkårPeriodeBuilder().medUtfall(Utfall.IKKE_OPPFYLT).medPeriode(LocalDate.now().minusDays(10), LocalDate.now())))
            .leggTil(vilkårResultatBuilder.hentBuilderFor(VilkårType.MEDLEMSKAPSVILKÅRET)
                .leggTil(new VilkårPeriodeBuilder().medUtfall(Utfall.IKKE_OPPFYLT).medPeriode(LocalDate.now().minusDays(15), LocalDate.now())));

        var vilkårene = vilkårResultatBuilder.build();

        Arbeidsgiver virksomhet1 = Arbeidsgiver.virksomhet("000000001");
        Arbeidsgiver virksomhet2 = Arbeidsgiver.virksomhet("000000002");

        var fraværsperioder = new LinkedHashMap<AktivitetTypeArbeidsgiver, LocalDateTimeline<OppgittFraværHolder>>();
        fraværsperioder.putAll(fraværEttRefusjonskrav(virksomhet1, LocalDate.now().minusDays(20), LocalDate.now(), null));
        fraværsperioder.putAll(fraværEttRefusjonskrav(virksomhet2, LocalDate.now().minusDays(30), LocalDate.now().minusDays(20), null));

        var perioder = mapper.utledPerioderMedUtfall(opprettRef(AktørId.dummy()), new InntektArbeidYtelseGrunnlag(UUID.randomUUID(), LocalDateTime.now()), Collections.emptyNavigableMap(), vilkårene, boundry, fraværsperioder);

        assertThat(perioder).hasSize(2);
        var fraværPerioder = perioder.get(new Aktivitet(UttakArbeidType.ARBEIDSTAKER, virksomhet1, InternArbeidsforholdRef.nullRef()));
        var fraværPerioder1 = perioder.get(new Aktivitet(UttakArbeidType.ARBEIDSTAKER, virksomhet2, InternArbeidsforholdRef.nullRef()));
        assertThat(fraværPerioder).hasSize(2);
        assertThat(fraværPerioder.stream().filter(it -> it.getErAvslåttInngangsvilkår() != null).filter(WrappedOppgittFraværPeriode::getErAvslåttInngangsvilkår)).hasSize(1);
        assertThat(fraværPerioder.stream().filter(it -> it.getErAvslåttInngangsvilkår() == null)).hasSize(1);
        assertThat(fraværPerioder1).hasSize(1);
        assertThat(fraværPerioder1.stream().filter(it -> it.getErAvslåttInngangsvilkår() != null).filter(WrappedOppgittFraværPeriode::getErAvslåttInngangsvilkår)).isEmpty();
        assertThat(fraværPerioder1.stream().filter(it -> it.getErAvslåttInngangsvilkår() == null)).hasSize(1);
    }

    @Test
    void skal_ta_hensyn_overlapp_flere_aktivitettyper() {
        var vilkårResultatBuilder = new VilkårResultatBuilder();
        vilkårResultatBuilder
            .leggTil(vilkårResultatBuilder.hentBuilderFor(VilkårType.OPPTJENINGSVILKÅRET)
                .leggTil(new VilkårPeriodeBuilder().medUtfall(Utfall.IKKE_OPPFYLT).medPeriode(LocalDate.now().minusDays(10), LocalDate.now())))
            .leggTil(vilkårResultatBuilder.hentBuilderFor(VilkårType.MEDLEMSKAPSVILKÅRET)
                .leggTil(new VilkårPeriodeBuilder().medUtfall(Utfall.IKKE_OPPFYLT).medPeriode(LocalDate.now().minusDays(15), LocalDate.now())));

        var vilkårene = vilkårResultatBuilder.build();

        var fraværsperioder = new LinkedHashMap<AktivitetTypeArbeidsgiver, LocalDateTimeline<OppgittFraværHolder>>();
        fraværsperioder.putAll(fraværEttRefusjonskrav(virksomhet1, LocalDate.now().minusDays(20), LocalDate.now(), null));
        fraværsperioder.putAll(fraværEnSøknad(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, virksomhet1, LocalDate.now().minusDays(30), LocalDate.now().minusDays(20), null));

        var perioder = mapper.utledPerioderMedUtfall(opprettRef(AktørId.dummy()), new InntektArbeidYtelseGrunnlag(UUID.randomUUID(), LocalDateTime.now()), Collections.emptyNavigableMap(), vilkårene, boundry, fraværsperioder);

        assertThat(perioder).hasSize(2);
        var fraværPerioder = perioder.get(new Aktivitet(UttakArbeidType.ARBEIDSTAKER, virksomhet1, InternArbeidsforholdRef.nullRef()));
        var fraværPerioder1 = perioder.get(new Aktivitet(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, virksomhet1, InternArbeidsforholdRef.nullRef()));
        assertThat(fraværPerioder).hasSize(2);
        assertThat(fraværPerioder.stream().filter(it -> it.getErAvslåttInngangsvilkår() != null).filter(WrappedOppgittFraværPeriode::getErAvslåttInngangsvilkår)).hasSize(1);
        assertThat(fraværPerioder.stream().filter(it -> it.getErAvslåttInngangsvilkår() == null)).hasSize(1);
        assertThat(fraværPerioder1).hasSize(1);
        assertThat(fraværPerioder1.stream().filter(it -> it.getErAvslåttInngangsvilkår() != null).filter(WrappedOppgittFraværPeriode::getErAvslåttInngangsvilkår)).isEmpty();
        assertThat(fraværPerioder1.stream().filter(it -> it.getErAvslåttInngangsvilkår() == null)).hasSize(1);
    }

    @Test
    void skal_ta_hensyn_til_avslått_arbeidsforhold() {
        var vilkårResultatBuilder = new VilkårResultatBuilder();
        var aktørDummy = AktørId.dummy();

        var vilkårene = vilkårResultatBuilder.build();

        var fraværsperioder = fraværEttRefusjonskrav(virksomhet1, LocalDate.now().minusDays(30), LocalDate.now(), null);

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt();
        var iayBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var aktørArbeidBuilder = iayBuilder.getAktørArbeidBuilder(aktørDummy);
        var yaBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        iayBuilder.leggTilAktørArbeid(aktørArbeidBuilder
            .leggTilYrkesaktivitet(yaBuilder
                .medArbeidsgiver(virksomhet1)
                .leggTilAktivitetsAvtale(yaBuilder.getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(30), LocalDate.now().minusDays(20)), true))
                .leggTilAktivitetsAvtale(yaBuilder.getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(30), LocalDate.now().minusDays(20)), false)
                    .medProsentsats(Stillingsprosent.HUNDRED)
                    .medBeskrivelse("asd")
                    .medSisteLønnsendringsdato(LocalDate.now().minusDays(30)))));
        iayGrunnlag.medData(iayBuilder);

        var build = iayGrunnlag.build();
        var perioder = mapper.utledPerioderMedUtfall(opprettRef(aktørDummy), build, Collections.emptyNavigableMap(), vilkårene, boundry, fraværsperioder);

        assertThat(perioder).hasSize(1);
        for (Map.Entry<Aktivitet, List<WrappedOppgittFraværPeriode>> entries : perioder.entrySet()) {
            assertThat(entries.getValue()).hasSize(2);
            assertThat(entries.getValue().stream().filter(it -> it.getErAvslåttInngangsvilkår() != null).filter(WrappedOppgittFraværPeriode::getErAvslåttInngangsvilkår)).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getArbeidStatus() != null).filter(it -> ArbeidStatus.AVSLUTTET.equals(it.getArbeidStatus()))).hasSize(1);
            assertThat(entries.getValue().stream().filter(it -> it.getArbeidStatus() != null).filter(it -> ArbeidStatus.IKKE_EKSISTERENDE.equals(it.getArbeidStatus()))).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getErIPermisjon() != null).filter(WrappedOppgittFraværPeriode::getErIPermisjon)).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getErAvslåttInngangsvilkår() == null)).hasSize(2);
        }
    }

    @Test
    void skal_ta_hensyn_til_nyoppstartet_i_arbeidsforhold_når_søknadsårsak_er_NYOPPSTARTET_I_ARBEIDSFORHOLD() {
        var vilkårResultatBuilder = new VilkårResultatBuilder();
        var aktørDummy = AktørId.dummy();

        var vilkårene = vilkårResultatBuilder.build();

        LocalDate søknadFom = LocalDate.now().minusWeeks(5);
        LocalDate søknadTom = LocalDate.now();
        var fraværsperioder = fraværEnSøknad(UttakArbeidType.ARBEIDSTAKER, virksomhet1, søknadFom, søknadTom, null);

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt();
        var iayBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var aktørArbeidBuilder = iayBuilder.getAktørArbeidBuilder(aktørDummy);
        var yaBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        LocalDate arbeidsavtaleStart = LocalDate.now().minusWeeks(6);
        iayBuilder.leggTilAktørArbeid(aktørArbeidBuilder
            .leggTilYrkesaktivitet(yaBuilder
                .medArbeidsgiver(virksomhet1)
                .leggTilAktivitetsAvtale(yaBuilder.getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(arbeidsavtaleStart, LocalDate.now()), true))
            ));
        iayGrunnlag.medData(iayBuilder);

        var build = iayGrunnlag.build();
        var resultatet = mapper.utledPerioderMedUtfall(opprettRef(aktørDummy), build, Collections.emptyNavigableMap(), vilkårene, boundry, fraværsperioder);

        Aktivitet aktivitetId = new Aktivitet(UttakArbeidType.ARBEIDSTAKER, virksomhet1, InternArbeidsforholdRef.nullRef());
        assertThat(resultatet.keySet()).containsOnly(aktivitetId);
        List<WrappedOppgittFraværPeriode> periodene = resultatet.get(aktivitetId);
        assertThat(periodene).hasSize(2);
        assertThat(periodene.get(0).getPeriode().getFom()).isEqualTo(søknadFom);
        assertThat(periodene.get(0).getPeriode().getTom()).isEqualTo(arbeidsavtaleStart.plusWeeks(4).minusDays(1));
        assertThat(periodene.get(0).getUtfallNyoppstartetVilkår()).isEqualTo(no.nav.k9.aarskvantum.kontrakter.Utfall.INNVILGET);
        assertThat(periodene.get(1).getPeriode().getFom()).isEqualTo(arbeidsavtaleStart.plusWeeks(4));
        assertThat(periodene.get(1).getPeriode().getTom()).isEqualTo(søknadTom);
        assertThat(periodene.get(1).getUtfallNyoppstartetVilkår()).isEqualTo(no.nav.k9.aarskvantum.kontrakter.Utfall.AVSLÅTT);
    }

    @Test
    void skal_ta_hensyn_til_permisjon() {
        var vilkårResultatBuilder = new VilkårResultatBuilder();
        var aktørDummy = AktørId.dummy();
        var jpDummy = new JournalpostId(123L);

        var vilkårene = vilkårResultatBuilder.build();

        var fraværsperioder = fraværEttRefusjonskrav(virksomhet1, LocalDate.now().minusDays(30), LocalDate.now(), null);

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt();
        var iayBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var aktørArbeidBuilder = iayBuilder.getAktørArbeidBuilder(aktørDummy);
        var yaBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        iayBuilder.leggTilAktørArbeid(aktørArbeidBuilder
            .leggTilYrkesaktivitet(yaBuilder
                .medArbeidsgiver(virksomhet1)
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
        var perioder = mapper.utledPerioderMedUtfall(opprettRef(aktørDummy), build, Collections.emptyNavigableMap(), vilkårene, boundry, fraværsperioder);

        assertThat(perioder).hasSize(1);
        for (Map.Entry<Aktivitet, List<WrappedOppgittFraværPeriode>> entries : perioder.entrySet()) {
            assertThat(entries.getValue()).hasSize(2);
            assertThat(entries.getValue().stream().filter(it -> it.getErAvslåttInngangsvilkår() != null).filter(WrappedOppgittFraværPeriode::getErAvslåttInngangsvilkår)).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getErIPermisjon() != null).filter(WrappedOppgittFraværPeriode::getErIPermisjon)).hasSize(1);
            assertThat(entries.getValue().stream().filter(it -> it.getArbeidStatus() != null).filter(it -> ArbeidStatus.AVSLUTTET.equals(it.getArbeidStatus()))).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getArbeidStatus() != null).filter(it -> ArbeidStatus.IKKE_EKSISTERENDE.equals(it.getArbeidStatus()))).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getErAvslåttInngangsvilkår() == null)).hasSize(2);
        }
    }

    @Test
    void skal_ta_hensyn_til_permisjon_under_100_prosent() {
        var vilkårResultatBuilder = new VilkårResultatBuilder();
        var dummy = AktørId.dummy();
        var jpDummy = new JournalpostId(123L);

        var vilkårene = vilkårResultatBuilder.build();

        var fraværsperioder = fraværEttRefusjonskrav(virksomhet1, LocalDate.now().minusDays(30), LocalDate.now(), null);
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt();
        var iayBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var aktørArbeidBuilder = iayBuilder.getAktørArbeidBuilder(dummy);
        var yaBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        iayBuilder.leggTilAktørArbeid(aktørArbeidBuilder
            .leggTilYrkesaktivitet(yaBuilder
                .medArbeidsgiver(virksomhet1)
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
        var perioder = mapper.utledPerioderMedUtfall(opprettRef(dummy), build, Collections.emptyNavigableMap(), vilkårene, boundry, fraværsperioder);

        assertThat(perioder).hasSize(1);
        for (Map.Entry<Aktivitet, List<WrappedOppgittFraværPeriode>> entries : perioder.entrySet()) {
            assertThat(entries.getValue()).hasSize(1);
            assertThat(entries.getValue().stream().filter(it -> it.getErAvslåttInngangsvilkår() != null).filter(WrappedOppgittFraværPeriode::getErAvslåttInngangsvilkår)).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getErIPermisjon() != null).filter(WrappedOppgittFraværPeriode::getErIPermisjon)).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getArbeidStatus() != null).filter(it -> ArbeidStatus.AVSLUTTET.equals(it.getArbeidStatus()))).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getArbeidStatus() != null).filter(it -> ArbeidStatus.IKKE_EKSISTERENDE.equals(it.getArbeidStatus()))).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getErAvslåttInngangsvilkår() == null)).hasSize(1);
        }
    }

    @Test
    void skal_ta_hensyn_til_avslått_arbeidsforhold_med_flere_ansettelsesperioder() {
        var vilkårResultatBuilder = new VilkårResultatBuilder();
        var aktørDummy = AktørId.dummy();
        var jpDummy = new JournalpostId(123L);

        var vilkårene = vilkårResultatBuilder.build();

        var arbeidsgiver = virksomhet1;
        var fraværsperioder = fraværEttRefusjonskrav(virksomhet1, LocalDate.now().minusDays(30), LocalDate.now(), null);

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt();
        var iayBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var aktørArbeidBuilder = iayBuilder.getAktørArbeidBuilder(aktørDummy);
        var yaBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var yaBuilder2 = aktørArbeidBuilder.getYrkesaktivitetBuilderForType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        iayBuilder.leggTilAktørArbeid(aktørArbeidBuilder
            .leggTilYrkesaktivitet(yaBuilder
                .medArbeidsgiver(arbeidsgiver)
                .medArbeidsforholdId(InternArbeidsforholdRef.ref(UUID.randomUUID()))
                .leggTilAktivitetsAvtale(yaBuilder.getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(90), LocalDate.now().minusDays(31)), true))
                .leggTilAktivitetsAvtale(yaBuilder.getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(30), LocalDate.now().minusDays(20)), true))
                .leggTilAktivitetsAvtale(yaBuilder.getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(30), LocalDate.now().minusDays(20)), false)
                    .medProsentsats(Stillingsprosent.HUNDRED)
                    .medBeskrivelse("asd")
                    .medSisteLønnsendringsdato(LocalDate.now().minusDays(30))))
            .leggTilYrkesaktivitet(yaBuilder2
                .medArbeidsgiver(arbeidsgiver)
                .medArbeidsforholdId(InternArbeidsforholdRef.ref(UUID.randomUUID()))
                .leggTilAktivitetsAvtale(yaBuilder2.getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(90), LocalDate.now().minusDays(31)), true))
                .leggTilAktivitetsAvtale(yaBuilder2.getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(90), LocalDate.now().minusDays(20)), false)
                    .medProsentsats(Stillingsprosent.HUNDRED)
                    .medBeskrivelse("asd")
                    .medSisteLønnsendringsdato(LocalDate.now().minusDays(30)))));
        iayGrunnlag.medData(iayBuilder);

        var build = iayGrunnlag.build();
        var perioder = mapper.utledPerioderMedUtfall(opprettRef(aktørDummy), build, Collections.emptyNavigableMap(), vilkårene, boundry, fraværsperioder);

        assertThat(perioder).hasSize(1);
        for (Map.Entry<Aktivitet, List<WrappedOppgittFraværPeriode>> entries : perioder.entrySet()) {
            assertThat(entries.getValue()).hasSize(2);
            assertThat(entries.getValue().stream().filter(it -> it.getErAvslåttInngangsvilkår() != null).filter(WrappedOppgittFraværPeriode::getErAvslåttInngangsvilkår)).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getArbeidStatus() != null).filter(it -> ArbeidStatus.AVSLUTTET.equals(it.getArbeidStatus()))).hasSize(1);
            assertThat(entries.getValue().stream().filter(it -> it.getArbeidStatus() != null).filter(it -> ArbeidStatus.IKKE_EKSISTERENDE.equals(it.getArbeidStatus()))).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getErIPermisjon() != null).filter(WrappedOppgittFraværPeriode::getErIPermisjon)).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getErAvslåttInngangsvilkår() == null)).hasSize(2);
        }
    }

    @Test
    void skal_ta_hensyn_til_delvis_fravær() {
        var vilkårResultatBuilder = new VilkårResultatBuilder();
        var aktørDummy = AktørId.dummy();
        var jpDummy = new JournalpostId(123L);

        var vilkårene = vilkårResultatBuilder.build();

        var arbeidsgiver = Arbeidsgiver.virksomhet("123123123");
        var fraværsperioder = fraværEttRefusjonskrav(arbeidsgiver, LocalDate.now().minusDays(1), LocalDate.now(), Duration.ofHours(2));
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt();
        var iayBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var aktørArbeidBuilder = iayBuilder.getAktørArbeidBuilder(aktørDummy);
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
        var perioder = mapper.utledPerioderMedUtfall(opprettRef(aktørDummy), build, Collections.emptyNavigableMap(), vilkårene, boundry, fraværsperioder);

        assertThat(perioder).hasSize(1);
        for (Map.Entry<Aktivitet, List<WrappedOppgittFraværPeriode>> entries : perioder.entrySet()) {
            assertThat(entries.getValue()).hasSize(2);
            assertThat(entries.getValue().stream().filter(it -> it.getErAvslåttInngangsvilkår() != null).filter(WrappedOppgittFraværPeriode::getErAvslåttInngangsvilkår)).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getArbeidStatus() != null).filter(it -> ArbeidStatus.AVSLUTTET.equals(it.getArbeidStatus()))).hasSize(2);
            assertThat(entries.getValue().stream().filter(it -> it.getArbeidStatus() != null).filter(it -> ArbeidStatus.IKKE_EKSISTERENDE.equals(it.getArbeidStatus()))).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getErIPermisjon() != null).filter(WrappedOppgittFraværPeriode::getErIPermisjon)).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getErAvslåttInngangsvilkår() == null)).hasSize(2);
        }
    }

    @Test
    void skal_ta_hensyn_til_selvstendig_næringsdrivende() {
        var vilkårResultatBuilder = new VilkårResultatBuilder();
        var aktørDummy = AktørId.dummy();
        var jpDummy = new JournalpostId(123L);

        var vilkårene = vilkårResultatBuilder.build();
        var iayGrunnlagTomt = InntektArbeidYtelseGrunnlagBuilder.nytt().build();

        var arbeidsgiver = Arbeidsgiver.virksomhet("123123123");
        var fraværsperioder = fraværEnSøknad(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, arbeidsgiver, LocalDate.now().minusDays(10), LocalDate.now(), null);

        var aktivitetPeriodeSN = OpptjeningAktivitetPeriode.Builder.ny()
            .medOpptjeningsnøkkel(new Opptjeningsnøkkel(InternArbeidsforholdRef.nullRef(), arbeidsgiver.getArbeidsgiverOrgnr(), null))
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(10), LocalDate.now()))
            .medVurderingsStatus(VurderingsStatus.FERDIG_VURDERT_GODKJENT)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.NÆRING)
            .build();
        var opptjeningAktivitetPerioder = new TreeMap<DatoIntervallEntitet, List<OpptjeningAktivitetPeriode>>();
        opptjeningAktivitetPerioder.put(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(10), LocalDate.now()), List.of(aktivitetPeriodeSN));

        var perioder = mapper.utledPerioderMedUtfall(opprettRef(aktørDummy), iayGrunnlagTomt, opptjeningAktivitetPerioder, vilkårene, boundry, fraværsperioder);

        assertThat(perioder).hasSize(1);
        for (Map.Entry<Aktivitet, List<WrappedOppgittFraværPeriode>> entries : perioder.entrySet()) {
            assertThat(entries.getValue()).hasSize(1);
            assertThat(entries.getValue().stream().filter(it -> it.getErAvslåttInngangsvilkår() != null).filter(WrappedOppgittFraværPeriode::getErAvslåttInngangsvilkår)).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getArbeidStatus() != null).filter(it -> ArbeidStatus.AKTIVT.equals(it.getArbeidStatus()))).hasSize(1);
            assertThat(entries.getValue().stream().filter(it -> it.getArbeidStatus() != null).filter(it -> ArbeidStatus.AVSLUTTET.equals(it.getArbeidStatus()))).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getArbeidStatus() != null).filter(it -> ArbeidStatus.IKKE_EKSISTERENDE.equals(it.getArbeidStatus()))).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getErIPermisjon() != null).filter(WrappedOppgittFraværPeriode::getErIPermisjon)).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getErAvslåttInngangsvilkår() == null)).hasSize(1);
        }
    }

    @Test
    void skal_ta_hensyn_til_frilans() {
        var vilkårResultatBuilder = new VilkårResultatBuilder();
        var aktørDummy = AktørId.dummy();
        var jpDummy = new JournalpostId(123L);

        var vilkårene = vilkårResultatBuilder.build();

        var fraværsperioder = fraværEnSøknad(UttakArbeidType.FRILANSER, null, LocalDate.now().minusDays(10), LocalDate.now(), null);

        var iayGrunnlagTomt = InntektArbeidYtelseGrunnlagBuilder.nytt().build();
        var aktivitetPeriodeFL = OpptjeningAktivitetPeriode.Builder.ny()
            .medOpptjeningsnøkkel(new Opptjeningsnøkkel(InternArbeidsforholdRef.nullRef(), null, aktørDummy.getAktørId()))
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(10), LocalDate.now()))
            .medVurderingsStatus(VurderingsStatus.FERDIG_VURDERT_GODKJENT)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.FRILANS)
            .build();
        var opptjeningAktivitetPerioder = new TreeMap<DatoIntervallEntitet, List<OpptjeningAktivitetPeriode>>();
        opptjeningAktivitetPerioder.put(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(10), LocalDate.now()), List.of(aktivitetPeriodeFL));

        var perioder = mapper.utledPerioderMedUtfall(opprettRef(aktørDummy), iayGrunnlagTomt, opptjeningAktivitetPerioder, vilkårene, boundry, fraværsperioder);

        assertThat(perioder).hasSize(1);
        for (Map.Entry<Aktivitet, List<WrappedOppgittFraværPeriode>> entries : perioder.entrySet()) {
            assertThat(entries.getValue()).hasSize(1);
            assertThat(entries.getValue().stream().filter(it -> it.getErAvslåttInngangsvilkår() != null).filter(WrappedOppgittFraværPeriode::getErAvslåttInngangsvilkår)).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getArbeidStatus() != null).filter(it -> ArbeidStatus.AKTIVT.equals(it.getArbeidStatus()))).hasSize(1);
            assertThat(entries.getValue().stream().filter(it -> it.getArbeidStatus() != null).filter(it -> ArbeidStatus.AVSLUTTET.equals(it.getArbeidStatus()))).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getArbeidStatus() != null).filter(it -> ArbeidStatus.IKKE_EKSISTERENDE.equals(it.getArbeidStatus()))).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getErIPermisjon() != null).filter(WrappedOppgittFraværPeriode::getErIPermisjon)).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getErAvslåttInngangsvilkår() == null)).hasSize(1);
        }
    }

    @Test
    void skal_filtrere_bort_perioder_utenfor_fagsaksinterval() {
        var vilkårResultatBuilder = new VilkårResultatBuilder();
        vilkårResultatBuilder.leggTil(vilkårResultatBuilder.hentBuilderFor(VilkårType.OPPTJENINGSVILKÅRET)
            .leggTil(new VilkårPeriodeBuilder().medUtfall(Utfall.IKKE_OPPFYLT).medPeriode(LocalDate.now().minusDays(10), LocalDate.now())));

        var vilkårene = vilkårResultatBuilder.build();

        var fraværsperioder = fraværEttRefusjonskrav(virksomhet1, LocalDate.now().minusDays(20), LocalDate.now(), null);

        BehandlingReferanse behandlingReferanse = opprettRef(AktørId.dummy());
        var fagsakInterval = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().plusMonths(3), LocalDate.now().plusMonths(9));
        var perioder = mapper.utledPerioderMedUtfall(behandlingReferanse, new InntektArbeidYtelseGrunnlag(UUID.randomUUID(), LocalDateTime.now()), Collections.emptyNavigableMap(), vilkårene, fagsakInterval, fraværsperioder);

        assertThat(perioder).isEmpty();
    }

    @Test
    void skal_ikke_slå_sammen_ved_forskjellig_innsendingstidspunkt() {
        var vilkårResultatBuilder = new VilkårResultatBuilder();
        vilkårResultatBuilder.leggTil(vilkårResultatBuilder.hentBuilderFor(VilkårType.OPPTJENINGSVILKÅRET)
            .leggTil(new VilkårPeriodeBuilder()
                .medUtfall(Utfall.IKKE_OPPFYLT)
                .medPeriode(LocalDate.now().minusDays(10), LocalDate.now())));

        var vilkårene = vilkårResultatBuilder.build();

        var fraværsperioder = fraværRefusjonskravHeleDager(virksomhet1, Map.of(
            new LocalDateInterval(LocalDate.now().minusDays(10), LocalDate.now().minusDays(8)), LocalDateTime.now().minusDays(10),
            new LocalDateInterval(LocalDate.now().minusDays(7), LocalDate.now()), LocalDateTime.now().minusDays(9)
        ));

        BehandlingReferanse behandlingReferanse = opprettRef(AktørId.dummy());
        var fagsakInterval = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(9));
        var perioder = mapper.utledPerioderMedUtfall(behandlingReferanse, new InntektArbeidYtelseGrunnlag(UUID.randomUUID(), LocalDateTime.now()), Collections.emptyNavigableMap(), vilkårene, fagsakInterval, fraværsperioder);

        assertThat(perioder).hasSize(1);
        for (Map.Entry<Aktivitet, List<WrappedOppgittFraværPeriode>> entries : perioder.entrySet()) {
            assertThat(entries.getValue()).hasSize(2);
            assertThat(entries.getValue().stream().filter(it -> it.getErAvslåttInngangsvilkår() != null).filter(WrappedOppgittFraværPeriode::getErAvslåttInngangsvilkår)).hasSize(2);
            assertThat(entries.getValue().stream().filter(it -> it.getErIPermisjon() != null).filter(WrappedOppgittFraværPeriode::getErIPermisjon)).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getArbeidStatus() != null).filter(it -> ArbeidStatus.AVSLUTTET.equals(it.getArbeidStatus()))).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getArbeidStatus() != null).filter(it -> ArbeidStatus.IKKE_EKSISTERENDE.equals(it.getArbeidStatus()))).hasSize(2);
            assertThat(entries.getValue().stream().filter(it -> it.getErAvslåttInngangsvilkår() == null)).isEmpty();
        }
    }

    private Map<AktivitetTypeArbeidsgiver, LocalDateTimeline<OppgittFraværHolder>> fraværEttRefusjonskrav(Arbeidsgiver arbeidsgiver, LocalDate fom, LocalDate tom, Duration fraværPrDag) {
        LocalDateTime innsendingstidspunkt = LocalDateTime.now();
        FraværÅrsak fraværÅrsak = FraværÅrsak.UDEFINERT;
        SøknadÅrsak søknadÅrsak = SøknadÅrsak.UDEFINERT;
        Utfall søknadsfristUtfall = Utfall.OPPFYLT;
        return Map.of(
            new AktivitetTypeArbeidsgiver(UttakArbeidType.ARBEIDSTAKER, arbeidsgiver),
            new LocalDateTimeline<>(fom, tom, OppgittFraværHolder.fraRefusjonskrav(InternArbeidsforholdRef.nullRef(), new OppgittFraværVerdi(innsendingstidspunkt, fraværPrDag, fraværÅrsak, søknadÅrsak, søknadsfristUtfall))));
    }

    private Map<AktivitetTypeArbeidsgiver, LocalDateTimeline<OppgittFraværHolder>> fraværEnSøknad(UttakArbeidType aktivitetType, Arbeidsgiver arbeidsgiver, LocalDate fom, LocalDate tom, Duration fraværPrDag) {
        LocalDateTime innsendingstidspunkt = LocalDateTime.now();
        FraværÅrsak fraværÅrsak = FraværÅrsak.ORDINÆRT_FRAVÆR;
        SøknadÅrsak søknadÅrsak = aktivitetType == UttakArbeidType.ARBEIDSTAKER ? SøknadÅrsak.NYOPPSTARTET_HOS_ARBEIDSGIVER : SøknadÅrsak.UDEFINERT;
        Utfall søknadsfristUtfall = Utfall.OPPFYLT;
        return Map.of(
            new AktivitetTypeArbeidsgiver(aktivitetType, arbeidsgiver),
            new LocalDateTimeline<>(fom, tom, OppgittFraværHolder.fraSøknad(new OppgittFraværVerdi(innsendingstidspunkt, fraværPrDag, fraværÅrsak, søknadÅrsak, søknadsfristUtfall))));
    }

    private Map<AktivitetTypeArbeidsgiver, LocalDateTimeline<OppgittFraværHolder>> fraværRefusjonskravHeleDager(Arbeidsgiver arbeidsgiver, List<LocalDateInterval> perioder) {
        LocalDateTime innsendingstidspunkt = LocalDateTime.now();
        FraværÅrsak fraværÅrsak = FraværÅrsak.UDEFINERT;
        SøknadÅrsak søknadÅrsak = SøknadÅrsak.UDEFINERT;
        Utfall søknadsfristUtfall = Utfall.OPPFYLT;
        return Map.of(
            new AktivitetTypeArbeidsgiver(UttakArbeidType.ARBEIDSTAKER, arbeidsgiver),
            new LocalDateTimeline<>(perioder.stream().map(p -> new LocalDateSegment<>(p, OppgittFraværHolder.fraRefusjonskrav(InternArbeidsforholdRef.nullRef(), new OppgittFraværVerdi(innsendingstidspunkt, null, fraværÅrsak, søknadÅrsak, søknadsfristUtfall)))).toList()));
    }

    private Map<AktivitetTypeArbeidsgiver, LocalDateTimeline<OppgittFraværHolder>> fraværRefusjonskravHeleDager(Arbeidsgiver arbeidsgiver, Map<LocalDateInterval, LocalDateTime> perioderMedInnsendingstidspunkter) {
        FraværÅrsak fraværÅrsak = FraværÅrsak.UDEFINERT;
        SøknadÅrsak søknadÅrsak = SøknadÅrsak.UDEFINERT;
        Utfall søknadsfristUtfall = Utfall.OPPFYLT;
        return Map.of(
            new AktivitetTypeArbeidsgiver(UttakArbeidType.ARBEIDSTAKER, arbeidsgiver),
            new LocalDateTimeline<>(perioderMedInnsendingstidspunkter.entrySet().stream()
                .map(e -> new LocalDateSegment<>(e.getKey(), OppgittFraværHolder.fraRefusjonskrav(InternArbeidsforholdRef.nullRef(), new OppgittFraværVerdi(e.getValue(), null, fraværÅrsak, søknadÅrsak, søknadsfristUtfall)))).toList()));
    }
}
