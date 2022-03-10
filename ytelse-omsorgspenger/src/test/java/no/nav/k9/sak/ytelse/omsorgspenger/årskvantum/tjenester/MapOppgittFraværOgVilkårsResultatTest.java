package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

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
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Stillingsprosent;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.SamtidigKravStatus;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFravær;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

class MapOppgittFraværOgVilkårsResultatTest {

    private final DatoIntervallEntitet boundry = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(9));

    @Test
    void skal_ta_hensyn_til_avslåtte_vilkår() {
        var vilkårResultatBuilder = new VilkårResultatBuilder();
        vilkårResultatBuilder.leggTil(vilkårResultatBuilder.hentBuilderFor(VilkårType.OPPTJENINGSVILKÅRET)
            .leggTil(new VilkårPeriodeBuilder().medUtfall(Utfall.IKKE_OPPFYLT).medPeriode(LocalDate.now().minusDays(10), LocalDate.now())));

        var vilkårene = vilkårResultatBuilder.build();

        var oppgittFravær = new OppgittFravær(new OppgittFraværPeriode(LocalDate.now().minusDays(20), LocalDate.now(), UttakArbeidType.ARBEIDSTAKER, null));

        BehandlingReferanse behandlingReferanse = opprettRef(AktørId.dummy());
        var perioder1 = mapTilWrappedPeriode(oppgittFravær);
        var perioder = new MapOppgittFraværOgVilkårsResultat().utledPerioderMedUtfall(behandlingReferanse, new InntektArbeidYtelseGrunnlag(UUID.randomUUID(), LocalDateTime.now()), Collections.emptyNavigableMap(), vilkårene, boundry, perioder1);

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

    private Set<no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.WrappedOppgittFraværPeriode> mapTilWrappedPeriode(OppgittFravær oppgittFravær) {
        return oppgittFravær.getPerioder().stream().map(it -> new no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.WrappedOppgittFraværPeriode(it, LocalDate.now().atStartOfDay(), KravDokumentType.INNTEKTSMELDING, Utfall.OPPFYLT, SamtidigKravStatus.refusjonskravFinnes())).collect(Collectors.toSet());
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

        var oppgittFravær = new OppgittFravær(new OppgittFraværPeriode(LocalDate.now().minusDays(10), LocalDate.now(), UttakArbeidType.ARBEIDSTAKER, null),
            new OppgittFraværPeriode(LocalDate.now().minusDays(30), LocalDate.now().minusDays(20), UttakArbeidType.ARBEIDSTAKER, null));

        var perioder = new MapOppgittFraværOgVilkårsResultat().utledPerioderMedUtfall(opprettRef(AktørId.dummy()), new InntektArbeidYtelseGrunnlag(UUID.randomUUID(), LocalDateTime.now()), Collections.emptyNavigableMap(), vilkårene, boundry, mapTilWrappedPeriode(oppgittFravær));

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

        var jpDummy = new JournalpostId(123L);
        var oppgittFravær = new OppgittFravær(new OppgittFraværPeriode(jpDummy, LocalDate.now().minusDays(20), LocalDate.now(), UttakArbeidType.ARBEIDSTAKER, Arbeidsgiver.virksomhet("000000000"), InternArbeidsforholdRef.nullRef(), null, FraværÅrsak.UDEFINERT, SøknadÅrsak.UDEFINERT),
            new OppgittFraværPeriode(jpDummy, LocalDate.now().minusDays(30), LocalDate.now().minusDays(20), UttakArbeidType.ARBEIDSTAKER, Arbeidsgiver.virksomhet("000000001"), InternArbeidsforholdRef.nullRef(), null, FraværÅrsak.UDEFINERT, SøknadÅrsak.UDEFINERT));

        var perioder = new MapOppgittFraværOgVilkårsResultat().utledPerioderMedUtfall(opprettRef(AktørId.dummy()), new InntektArbeidYtelseGrunnlag(UUID.randomUUID(), LocalDateTime.now()), Collections.emptyNavigableMap(), vilkårene, boundry, mapTilWrappedPeriode(oppgittFravær));

        assertThat(perioder).hasSize(2);
        var fraværPerioder = perioder.get(new Aktivitet(UttakArbeidType.ARBEIDSTAKER, Arbeidsgiver.virksomhet("000000000"), InternArbeidsforholdRef.nullRef()));
        var fraværPerioder1 = perioder.get(new Aktivitet(UttakArbeidType.ARBEIDSTAKER, Arbeidsgiver.virksomhet("000000001"), InternArbeidsforholdRef.nullRef()));
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

        var jpDummy = new JournalpostId(123L);
        var oppgittFravær = new OppgittFravær(new OppgittFraværPeriode(jpDummy, LocalDate.now().minusDays(20), LocalDate.now(), UttakArbeidType.ARBEIDSTAKER, Arbeidsgiver.virksomhet("000000000"), InternArbeidsforholdRef.nullRef(), null, FraværÅrsak.UDEFINERT, SøknadÅrsak.UDEFINERT),
            new OppgittFraværPeriode(jpDummy, LocalDate.now().minusDays(30), LocalDate.now().minusDays(20), UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, Arbeidsgiver.virksomhet("000000000"), InternArbeidsforholdRef.nullRef(), null, FraværÅrsak.UDEFINERT, SøknadÅrsak.UDEFINERT));

        var perioder = new MapOppgittFraværOgVilkårsResultat().utledPerioderMedUtfall(opprettRef(AktørId.dummy()), new InntektArbeidYtelseGrunnlag(UUID.randomUUID(), LocalDateTime.now()), Collections.emptyNavigableMap(), vilkårene, boundry, mapTilWrappedPeriode(oppgittFravær));

        assertThat(perioder).hasSize(2);
        var fraværPerioder = perioder.get(new Aktivitet(UttakArbeidType.ARBEIDSTAKER, Arbeidsgiver.virksomhet("000000000"), InternArbeidsforholdRef.nullRef()));
        var fraværPerioder1 = perioder.get(new Aktivitet(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, Arbeidsgiver.virksomhet("000000000"), InternArbeidsforholdRef.nullRef()));
        assertThat(fraværPerioder).hasSize(2);
        assertThat(fraværPerioder.stream().filter(it -> it.getErAvslåttInngangsvilkår() != null).filter(WrappedOppgittFraværPeriode::getErAvslåttInngangsvilkår)).hasSize(1);
        assertThat(fraværPerioder.stream().filter(it -> it.getErAvslåttInngangsvilkår() == null)).hasSize(1);
        assertThat(fraværPerioder1).hasSize(1);
        assertThat(fraværPerioder1.stream().filter(it -> it.getErAvslåttInngangsvilkår() != null).filter(WrappedOppgittFraværPeriode::getErAvslåttInngangsvilkår)).isEmpty();
        assertThat(fraværPerioder1.stream().filter(it -> it.getErAvslåttInngangsvilkår() == null)).hasSize(1);
    }

    @Test
    void skal_ta_hensyn_til_overlapp_i_søkte_perioder() {
        var vilkårResultatBuilder = new VilkårResultatBuilder();
        vilkårResultatBuilder
            .leggTil(vilkårResultatBuilder.hentBuilderFor(VilkårType.OPPTJENINGSVILKÅRET)
                .leggTil(new VilkårPeriodeBuilder().medUtfall(Utfall.IKKE_OPPFYLT).medPeriode(LocalDate.now().minusDays(10), LocalDate.now())))
            .leggTil(vilkårResultatBuilder.hentBuilderFor(VilkårType.MEDLEMSKAPSVILKÅRET)
                .leggTil(new VilkårPeriodeBuilder().medUtfall(Utfall.IKKE_OPPFYLT).medPeriode(LocalDate.now().minusDays(15), LocalDate.now())));

        var vilkårene = vilkårResultatBuilder.build();

        var oppgittFravær = new OppgittFravær(new OppgittFraværPeriode(LocalDate.now().minusDays(10), LocalDate.now(), UttakArbeidType.ARBEIDSTAKER, null),
            new OppgittFraværPeriode(LocalDate.now().minusDays(30), LocalDate.now().minusDays(10), UttakArbeidType.ARBEIDSTAKER, null));

        var perioder = new MapOppgittFraværOgVilkårsResultat().utledPerioderMedUtfall(opprettRef(AktørId.dummy()), new InntektArbeidYtelseGrunnlag(UUID.randomUUID(), LocalDateTime.now()), Collections.emptyNavigableMap(), vilkårene, boundry, mapTilWrappedPeriode(oppgittFravær));

        assertThat(perioder).hasSize(1);
        for (Map.Entry<Aktivitet, List<WrappedOppgittFraværPeriode>> entries : perioder.entrySet()) {
            assertThat(entries.getValue()).hasSize(2);
            assertThat(entries.getValue().stream().filter(it -> it.getErAvslåttInngangsvilkår() != null).filter(WrappedOppgittFraværPeriode::getErAvslåttInngangsvilkår)).hasSize(1);
            assertThat(entries.getValue().stream().filter(it -> it.getErAvslåttInngangsvilkår() == null)).hasSize(1);
        }
    }

    @Test
    void skal_ta_hensyn_til_avslått_arbeidsforhold() {
        var vilkårResultatBuilder = new VilkårResultatBuilder();
        var aktørDummy = AktørId.dummy();
        var jpDummy = new JournalpostId(123L);

        var vilkårene = vilkårResultatBuilder.build();

        var arbeidsgiver = Arbeidsgiver.virksomhet("123123123");
        var oppgittFravær = new OppgittFravær(new OppgittFraværPeriode(jpDummy, LocalDate.now().minusDays(10), LocalDate.now(), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, InternArbeidsforholdRef.nullRef(), null, FraværÅrsak.UDEFINERT, SøknadÅrsak.UDEFINERT),
            new OppgittFraværPeriode(jpDummy, LocalDate.now().minusDays(30), LocalDate.now().minusDays(10), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, InternArbeidsforholdRef.nullRef(), null, FraværÅrsak.UDEFINERT, SøknadÅrsak.UDEFINERT));
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
        var perioder = new MapOppgittFraværOgVilkårsResultat().utledPerioderMedUtfall(opprettRef(aktørDummy), build, Collections.emptyNavigableMap(), vilkårene, boundry, mapTilWrappedPeriode(oppgittFravær));

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
    void skal_ta_hensyn_til_permisjon() {
        var vilkårResultatBuilder = new VilkårResultatBuilder();
        var aktørDummy = AktørId.dummy();
        var jpDummy = new JournalpostId(123L);

        var vilkårene = vilkårResultatBuilder.build();

        var arbeidsgiver = Arbeidsgiver.virksomhet("123123123");
        var oppgittFravær = new OppgittFravær(new OppgittFraværPeriode(jpDummy, LocalDate.now().minusDays(10), LocalDate.now(), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, InternArbeidsforholdRef.nullRef(), null, FraværÅrsak.UDEFINERT, SøknadÅrsak.UDEFINERT),
            new OppgittFraværPeriode(jpDummy, LocalDate.now().minusDays(30), LocalDate.now().minusDays(10), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, InternArbeidsforholdRef.nullRef(), null, FraværÅrsak.UDEFINERT, SøknadÅrsak.UDEFINERT));
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt();
        var iayBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var aktørArbeidBuilder = iayBuilder.getAktørArbeidBuilder(aktørDummy);
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
        var perioder = new MapOppgittFraværOgVilkårsResultat().utledPerioderMedUtfall(opprettRef(aktørDummy), build, Collections.emptyNavigableMap(), vilkårene, boundry, mapTilWrappedPeriode(oppgittFravær));

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

        var arbeidsgiver = Arbeidsgiver.virksomhet("123123123");
        var oppgittFravær = new OppgittFravær(new OppgittFraværPeriode(jpDummy, LocalDate.now().minusDays(10), LocalDate.now(), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, InternArbeidsforholdRef.nullRef(), null, FraværÅrsak.UDEFINERT, SøknadÅrsak.UDEFINERT),
            new OppgittFraværPeriode(jpDummy, LocalDate.now().minusDays(30), LocalDate.now().minusDays(10), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, InternArbeidsforholdRef.nullRef(), null, FraværÅrsak.UDEFINERT, SøknadÅrsak.UDEFINERT));
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
        var perioder = new MapOppgittFraværOgVilkårsResultat().utledPerioderMedUtfall(opprettRef(dummy), build, Collections.emptyNavigableMap(), vilkårene, boundry, mapTilWrappedPeriode(oppgittFravær));

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

        var arbeidsgiver = Arbeidsgiver.virksomhet("123123123");
        var oppgittFravær = new OppgittFravær(new OppgittFraværPeriode(jpDummy, LocalDate.now().minusDays(10), LocalDate.now(), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, InternArbeidsforholdRef.nullRef(), null, FraværÅrsak.UDEFINERT, SøknadÅrsak.UDEFINERT),
            new OppgittFraværPeriode(jpDummy, LocalDate.now().minusDays(30), LocalDate.now().minusDays(10), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, InternArbeidsforholdRef.nullRef(), null, FraværÅrsak.UDEFINERT, SøknadÅrsak.UDEFINERT));
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
        var perioder = new MapOppgittFraværOgVilkårsResultat().utledPerioderMedUtfall(opprettRef(aktørDummy), build, Collections.emptyNavigableMap(), vilkårene, boundry, mapTilWrappedPeriode(oppgittFravær));

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
        var oppgittFravær = new OppgittFravær(new OppgittFraværPeriode(jpDummy, LocalDate.now().minusDays(1), LocalDate.now(), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, InternArbeidsforholdRef.nullRef(), Duration.ofHours(2), FraværÅrsak.UDEFINERT, SøknadÅrsak.UDEFINERT));
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
        var perioder = new MapOppgittFraværOgVilkårsResultat().utledPerioderMedUtfall(opprettRef(aktørDummy), build, Collections.emptyNavigableMap(), vilkårene, boundry, mapTilWrappedPeriode(oppgittFravær));

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
        var oppgittFravær = new OppgittFravær(
            new OppgittFraværPeriode(jpDummy, LocalDate.now().minusDays(10), LocalDate.now(), UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, arbeidsgiver, InternArbeidsforholdRef.nullRef(), null, FraværÅrsak.ORDINÆRT_FRAVÆR, SøknadÅrsak.UDEFINERT));

        var aktivitetPeriodeSN = OpptjeningAktivitetPeriode.Builder.ny()
            .medOpptjeningsnøkkel(new Opptjeningsnøkkel(InternArbeidsforholdRef.nullRef(), arbeidsgiver.getArbeidsgiverOrgnr(), null))
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(10), LocalDate.now()))
            .medVurderingsStatus(VurderingsStatus.FERDIG_VURDERT_GODKJENT)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.NÆRING)
            .build();
        var opptjeningAktivitetPerioder = new TreeMap<DatoIntervallEntitet, List<OpptjeningAktivitetPeriode>>();
        opptjeningAktivitetPerioder.put(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(10), LocalDate.now()), List.of(aktivitetPeriodeSN));

        var perioder = new MapOppgittFraværOgVilkårsResultat().utledPerioderMedUtfall(opprettRef(aktørDummy), iayGrunnlagTomt, opptjeningAktivitetPerioder, vilkårene, boundry, mapTilWrappedPeriode(oppgittFravær));

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

        var oppgittFravær = new OppgittFravær(
            new OppgittFraværPeriode(jpDummy, LocalDate.now().minusDays(10), LocalDate.now(), UttakArbeidType.FRILANSER, null, InternArbeidsforholdRef.nullRef(), null, FraværÅrsak.ORDINÆRT_FRAVÆR, SøknadÅrsak.UDEFINERT));
        var iayGrunnlagTomt = InntektArbeidYtelseGrunnlagBuilder.nytt().build();
        var aktivitetPeriodeFL = OpptjeningAktivitetPeriode.Builder.ny()
            .medOpptjeningsnøkkel(new Opptjeningsnøkkel(InternArbeidsforholdRef.nullRef(), null, aktørDummy.getAktørId()))
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(10), LocalDate.now()))
            .medVurderingsStatus(VurderingsStatus.FERDIG_VURDERT_GODKJENT)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.FRILANS)
            .build();
        var opptjeningAktivitetPerioder = new TreeMap<DatoIntervallEntitet, List<OpptjeningAktivitetPeriode>>();
        opptjeningAktivitetPerioder.put(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(10), LocalDate.now()), List.of(aktivitetPeriodeFL));

        var perioder = new MapOppgittFraværOgVilkårsResultat().utledPerioderMedUtfall(opprettRef(aktørDummy), iayGrunnlagTomt, opptjeningAktivitetPerioder, vilkårene, boundry, mapTilWrappedPeriode(oppgittFravær));

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

        var oppgittFravær = new OppgittFravær(new OppgittFraværPeriode(LocalDate.now().minusDays(20), LocalDate.now(), UttakArbeidType.ARBEIDSTAKER, null));

        BehandlingReferanse behandlingReferanse = opprettRef(AktørId.dummy());
        var fagsakInterval = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().plusMonths(3), LocalDate.now().plusMonths(9));
        var perioder = new MapOppgittFraværOgVilkårsResultat().utledPerioderMedUtfall(behandlingReferanse, new InntektArbeidYtelseGrunnlag(UUID.randomUUID(), LocalDateTime.now()), Collections.emptyNavigableMap(), vilkårene, fagsakInterval, mapTilWrappedPeriode(oppgittFravær));

        assertThat(perioder).hasSize(1);
        for (Map.Entry<Aktivitet, List<WrappedOppgittFraværPeriode>> entries : perioder.entrySet()) {
            assertThat(entries.getValue()).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getErAvslåttInngangsvilkår() != null).filter(WrappedOppgittFraværPeriode::getErAvslåttInngangsvilkår)).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getErIPermisjon() != null).filter(WrappedOppgittFraværPeriode::getErIPermisjon)).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getArbeidStatus() != null).filter(it -> ArbeidStatus.AVSLUTTET.equals(it.getArbeidStatus()))).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getArbeidStatus() != null).filter(it -> ArbeidStatus.IKKE_EKSISTERENDE.equals(it.getArbeidStatus()))).isEmpty();
            assertThat(entries.getValue().stream().filter(it -> it.getErAvslåttInngangsvilkår() == null)).isEmpty();
        }
    }

    @Test
    void skal_ikke_slå_sammen_ved_forskjellig_innsendingstidspunkt() {
        var vilkårResultatBuilder = new VilkårResultatBuilder();
        vilkårResultatBuilder.leggTil(vilkårResultatBuilder.hentBuilderFor(VilkårType.OPPTJENINGSVILKÅRET)
            .leggTil(new VilkårPeriodeBuilder()
                .medUtfall(Utfall.IKKE_OPPFYLT)
                .medPeriode(LocalDate.now().minusDays(10), LocalDate.now())));

        var vilkårene = vilkårResultatBuilder.build();

        var arbeidsgiver = Arbeidsgiver.virksomhet("000000000");
        var arbeidsforholdRef = InternArbeidsforholdRef.nyRef();
        var jpId = new JournalpostId(123L);
        var oppgittFravær1 = new OppgittFraværPeriode(jpId, LocalDate.now().minusDays(10), LocalDate.now().minusDays(8), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, arbeidsforholdRef, null, FraværÅrsak.UDEFINERT, SøknadÅrsak.UDEFINERT);
        var oppgittFravær2 = new OppgittFraværPeriode(jpId, LocalDate.now().minusDays(7), LocalDate.now(), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, arbeidsforholdRef, null, FraværÅrsak.UDEFINERT, SøknadÅrsak.UDEFINERT);

        var søktePerioder = new HashSet<no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.WrappedOppgittFraværPeriode>();
        int i = 0;
        for (OppgittFraværPeriode oppgittFravær : Set.of(oppgittFravær1, oppgittFravær2)) {
            søktePerioder.add(new no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.WrappedOppgittFraværPeriode(oppgittFravær, LocalDateTime.now().minusDays(10).plusDays(i++), KravDokumentType.INNTEKTSMELDING, Utfall.OPPFYLT, SamtidigKravStatus.refusjonskravFinnes()));
        }

        BehandlingReferanse behandlingReferanse = opprettRef(AktørId.dummy());
        var fagsakInterval = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(9));
        var perioder = new MapOppgittFraværOgVilkårsResultat().utledPerioderMedUtfall(behandlingReferanse, new InntektArbeidYtelseGrunnlag(UUID.randomUUID(), LocalDateTime.now()), Collections.emptyNavigableMap(), vilkårene, fagsakInterval, søktePerioder);

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
}
