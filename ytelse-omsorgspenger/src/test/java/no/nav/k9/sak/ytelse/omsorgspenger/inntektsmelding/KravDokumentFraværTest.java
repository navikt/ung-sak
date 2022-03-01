package no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.uttak.FraværÅrsak;
import no.nav.k9.kodeverk.uttak.SøknadÅrsak;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.domene.iay.modell.PeriodeAndel;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

class KravDokumentFraværTest {

    @Test
    void skal_bygge_tidslinje_av_fravær_fra_inntektsmeldinger() {
        var inntektsmelding1 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medArbeidsforholdId(EksternArbeidsforholdRef.nullRef())
            .medJournalpostId("1")
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(10))
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("AR123")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(30), LocalDate.now().minusDays(25))))
            .medRefusjon(BigDecimal.TEN)
            .build();
        var inntektsmelding2 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000001"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medArbeidsforholdId(EksternArbeidsforholdRef.nullRef())
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(9))
            .medJournalpostId("2")
            .medBeløp(BigDecimal.ONE)
            .medKanalreferanse("AR124")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(26), LocalDate.now().minusDays(25))))
            .medRefusjon(BigDecimal.ONE)
            .build();
        var inntektsmelding3 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medArbeidsforholdId(EksternArbeidsforholdRef.nullRef())
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(9))
            .medJournalpostId("3")
            .medBeløp(BigDecimal.ONE)
            .medKanalreferanse("AR125")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(30), LocalDate.now().minusDays(25))))
            .medRefusjon(BigDecimal.ONE)
            .build();
        var inntektsmelding4 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medArbeidsforholdId(EksternArbeidsforholdRef.nullRef())
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(10))
            .medJournalpostId("4")
            .medBeløp(BigDecimal.ONE)
            .medKanalreferanse("AR126")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now(), LocalDate.now().plusDays(5))))
            .medRefusjon(BigDecimal.ONE)
            .build();
        var input = Map.ofEntries(
            mapTilKravdok(inntektsmelding1), mapTilKravdok(inntektsmelding2), mapTilKravdok(inntektsmelding3), mapTilKravdok(inntektsmelding4));

        List<WrappedOppgittFraværPeriode> oppgittFraværPeriode = new KravDokumentFravær().trekkUtAlleFraværOgValiderOverlapp(input);

        assertThat(oppgittFraværPeriode).hasSize(3);
        assertThat(oppgittFraværPeriode.stream().map(WrappedOppgittFraværPeriode::getPeriode).filter(it -> it.getArbeidsgiver().getOrgnr().equals("000000000"))).hasSize(2);
        assertThat(oppgittFraværPeriode.stream().map(WrappedOppgittFraværPeriode::getPeriode).filter(it -> it.getArbeidsgiver().getOrgnr().equals("000000001"))).hasSize(1);
        oppgittFraværPeriode.forEach(
            fp -> assertThat(fp.getSamtidigeKrav()).isEqualTo(SamtidigKravStatus.refusjonskravFinnes())
        );
    }

    @Test
    void skal_rydde_i_berørte_tidslinjer() {
        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        var inntektsmelding1 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(arbeidsforholdId)
            .medArbeidsforholdId(EksternArbeidsforholdRef.ref("asdf"))
            .medJournalpostId("1")
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(10))
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("AR123")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(30), LocalDate.now().minusDays(25))))
            .medRefusjon(BigDecimal.TEN)
            .build();
        var inntektsmelding2 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medArbeidsforholdId(EksternArbeidsforholdRef.nullRef())
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(9))
            .medJournalpostId("2")
            .medBeløp(BigDecimal.ONE)
            .medKanalreferanse("AR124")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(28), LocalDate.now().minusDays(25))))
            .medRefusjon(BigDecimal.ONE)
            .build();
        var input = Map.ofEntries(
            mapTilKravdok(inntektsmelding1), mapTilKravdok(inntektsmelding2));

        List<WrappedOppgittFraværPeriode> oppgittFraværPeriode = new KravDokumentFravær().trekkUtAlleFraværOgValiderOverlapp(input);

        assertThat(oppgittFraværPeriode).hasSize(2);
        assertThat(oppgittFraværPeriode.stream().map(WrappedOppgittFraværPeriode::getPeriode).filter(it -> it.getArbeidsgiver().getOrgnr().equals("000000000") && it.getArbeidsforholdRef().equals(arbeidsforholdId))).hasSize(1);
        assertThat(oppgittFraværPeriode.stream()
            .map(WrappedOppgittFraværPeriode::getPeriode)
            .filter(it -> it.getArbeidsgiver().getOrgnr().equals("000000000") && it.getArbeidsforholdRef().equals(arbeidsforholdId))
            .map(OppgittFraværPeriode::getPeriode))
            .contains(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(30), LocalDate.now().minusDays(29)));
        assertThat(oppgittFraværPeriode.stream()
            .map(WrappedOppgittFraværPeriode::getPeriode)
            .filter(it -> it.getArbeidsgiver().getOrgnr().equals("000000000") && it.getArbeidsforholdRef().equals(InternArbeidsforholdRef.nullRef())))
            .hasSize(1);
        assertThat(oppgittFraværPeriode.stream()
            .map(WrappedOppgittFraværPeriode::getPeriode)
            .filter(it -> it.getArbeidsgiver().getOrgnr().equals("000000000") && it.getArbeidsforholdRef().equals(InternArbeidsforholdRef.nullRef()))
            .map(OppgittFraværPeriode::getPeriode))
            .contains(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(28), LocalDate.now().minusDays(25)));
    }

    @Test
    void skal_rydde_i_berørte_tidslinjer_reverse() {
        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        var inntektsmelding1 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(arbeidsforholdId)
            .medArbeidsforholdId(EksternArbeidsforholdRef.ref("asdf"))
            .medJournalpostId("1")
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(9))
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("AR124")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(30), LocalDate.now().minusDays(25))))
            .medRefusjon(BigDecimal.TEN)
            .build();
        var inntektsmelding2 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medArbeidsforholdId(EksternArbeidsforholdRef.nullRef())
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(10))
            .medJournalpostId("2")
            .medBeløp(BigDecimal.ONE)
            .medKanalreferanse("AR123")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(28), LocalDate.now().minusDays(25))))
            .medRefusjon(BigDecimal.ONE)
            .build();
        var input = Map.ofEntries(
            mapTilKravdok(inntektsmelding1), mapTilKravdok(inntektsmelding2));

        List<WrappedOppgittFraværPeriode> oppgittFraværPeriode = new KravDokumentFravær().trekkUtAlleFraværOgValiderOverlapp(input);

        assertThat(oppgittFraværPeriode).hasSize(1);
        assertThat(oppgittFraværPeriode.stream()
            .map(WrappedOppgittFraværPeriode::getPeriode)
            .filter(it -> it.getArbeidsgiver().getOrgnr().equals("000000000") && it.getArbeidsforholdRef().equals(arbeidsforholdId))).hasSize(1);
        assertThat(oppgittFraværPeriode.stream()
            .map(WrappedOppgittFraværPeriode::getPeriode)
            .filter(it -> it.getArbeidsgiver().getOrgnr().equals("000000000") && it.getArbeidsforholdRef().equals(arbeidsforholdId))
            .map(OppgittFraværPeriode::getPeriode))
            .contains(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(30), LocalDate.now().minusDays(25)));
        assertThat(oppgittFraværPeriode.stream()
            .map(WrappedOppgittFraværPeriode::getPeriode)
            .filter(it -> it.getArbeidsgiver().getOrgnr().equals("000000000") && it.getArbeidsforholdRef().equals(InternArbeidsforholdRef.nullRef())))
            .hasSize(0);
    }

    @Test
    void skal_rydde_i_berørte_tidslinjer_reverse_2() {
        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        var inntektsmelding1 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(arbeidsforholdId)
            .medArbeidsforholdId(EksternArbeidsforholdRef.ref("asdf"))
            .medJournalpostId("1")
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(9))
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("AR124")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(30), LocalDate.now().minusDays(25))))
            .medRefusjon(BigDecimal.TEN)
            .build();
        var inntektsmelding2 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medArbeidsforholdId(EksternArbeidsforholdRef.nullRef())
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(10))
            .medJournalpostId("2")
            .medBeløp(BigDecimal.ONE)
            .medKanalreferanse("AR123")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(28), LocalDate.now().minusDays(22))))
            .medRefusjon(BigDecimal.ONE)
            .build();
        var input = Map.ofEntries(
            mapTilKravdok(inntektsmelding1), mapTilKravdok(inntektsmelding2));

        List<WrappedOppgittFraværPeriode> oppgittFraværPeriode = new KravDokumentFravær().trekkUtAlleFraværOgValiderOverlapp(input);

        assertThat(oppgittFraværPeriode).hasSize(2);
        assertThat(oppgittFraværPeriode.stream()
            .map(WrappedOppgittFraværPeriode::getPeriode)
            .filter(it -> it.getArbeidsgiver().getOrgnr().equals("000000000") && it.getArbeidsforholdRef().equals(arbeidsforholdId))).hasSize(1);
        assertThat(oppgittFraværPeriode.stream()
            .map(WrappedOppgittFraværPeriode::getPeriode)
            .filter(it -> it.getArbeidsgiver().getOrgnr().equals("000000000") && it.getArbeidsforholdRef().equals(arbeidsforholdId))
            .map(OppgittFraværPeriode::getPeriode))
            .contains(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(30), LocalDate.now().minusDays(25)));
        assertThat(oppgittFraværPeriode.stream()
            .map(WrappedOppgittFraværPeriode::getPeriode)
            .filter(it -> it.getArbeidsgiver().getOrgnr().equals("000000000") && it.getArbeidsforholdRef().equals(InternArbeidsforholdRef.nullRef())))
            .hasSize(1);
        assertThat(oppgittFraværPeriode.stream()
            .map(WrappedOppgittFraværPeriode::getPeriode)
            .filter(it -> it.getArbeidsgiver().getOrgnr().equals("000000000") && it.getArbeidsforholdRef().equals(InternArbeidsforholdRef.nullRef()))
            .map(OppgittFraværPeriode::getPeriode))
            .contains(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(24), LocalDate.now().minusDays(22)));
    }

    @Test
    void skal_takle_søknader_om_utbetaling_til_frilanser() {
        JournalpostId journalpost1 = new JournalpostId("1");
        JournalpostId journalpost2 = new JournalpostId("2");
        LocalDateTime nå = LocalDateTime.now();
        LocalDate idag = nå.toLocalDate();
        var kravDok1 = new KravDokument(journalpost1, nå.minusMinutes(15), KravDokumentType.SØKNAD);
        var kravDok2 = new KravDokument(journalpost2, nå.minusMinutes(5), KravDokumentType.SØKNAD);

        var input = Map.of(
            kravDok1, List.of(lagSøknadsperiode(journalpost1, idag.minusDays(10), idag.minusDays(9), UttakArbeidType.FRILANSER)),
            kravDok2, List.of(lagSøknadsperiode(journalpost2, idag.minusDays(5), idag.minusDays(5), UttakArbeidType.FRILANSER)));

        List<WrappedOppgittFraværPeriode> resultat = new KravDokumentFravær().trekkUtAlleFraværOgValiderOverlapp(input);

        assertThat(resultat).hasSize(2);
        WrappedOppgittFraværPeriode fp1 = resultat.get(0);
        assertThat(fp1.getPeriode().getAktivitetType()).isEqualTo(UttakArbeidType.FRILANSER);
        assertThat(fp1.getPeriode().getFom()).isEqualTo(idag.minusDays(10));
        assertThat(fp1.getPeriode().getTom()).isEqualTo(idag.minusDays(9));
        assertThat(fp1.getSamtidigeKrav()).isEqualTo(SamtidigKravStatus.søknadFinnes());
        WrappedOppgittFraværPeriode fp2 = resultat.get(1);
        assertThat(fp2.getPeriode().getAktivitetType()).isEqualTo(UttakArbeidType.FRILANSER);
        assertThat(fp2.getPeriode().getFom()).isEqualTo(idag.minusDays(5));
        assertThat(fp2.getPeriode().getTom()).isEqualTo(idag.minusDays(5));
        assertThat(fp2.getSamtidigeKrav()).isEqualTo(SamtidigKravStatus.søknadFinnes());
    }

    @Test
    void skal_prioritere_fravær_fra_im_over_fravær_fra_søknad() {
        Duration fraværIm = Duration.ofHours(4);
        Duration fraværSøknad = null;
        // IM mottas først, men prioriteres likevel over søknad
        var innsendingIm = LocalDateTime.now().minusDays(2);
        var innsendingsSøknad = LocalDateTime.now().minusDays(1);

        var im = InntektsmeldingBuilder.builder()
            .medJournalpostId("1")
            .medInnsendingstidspunkt(innsendingIm)
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(10), LocalDate.now(), fraværIm)))
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medArbeidsforholdId(EksternArbeidsforholdRef.nullRef())
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("AR123")
            .medRefusjon(BigDecimal.TEN)
            .build();
        var kravDokIm = mapTilKravdok(im).getKey();
        var fraværsperioderIm = mapTilKravdok(im).getValue();

        var jpSøknad = new JournalpostId("2");
        var kravDokSøknad = new KravDokument(jpSøknad, innsendingsSøknad, KravDokumentType.SØKNAD);
        var fraværsperioderSøknad = List.of(
            lagSøknadsperiode(jpSøknad, LocalDate.now().minusDays(10), LocalDate.now(), fraværSøknad, UttakArbeidType.ARBEIDSTAKER, im.getArbeidsgiver()));

        var input = Map.of(
            kravDokIm, fraværsperioderIm,
            kravDokSøknad, fraværsperioderSøknad);

        List<WrappedOppgittFraværPeriode> resultat = new KravDokumentFravær().trekkUtAlleFraværOgValiderOverlapp(input);

        assertThat(resultat).hasSize(1);
        WrappedOppgittFraværPeriode fp1 = resultat.get(0);
        assertThat(fp1.getPeriode().getFraværPerDag()).isEqualTo(fraværIm);
        assertThat(fp1.getPeriode().getFom()).isEqualTo(LocalDate.now().minusDays(10));
        assertThat(fp1.getPeriode().getTom()).isEqualTo(LocalDate.now());
        assertThat(fp1.getSamtidigeKrav()).isEqualTo(SamtidigKravStatus.refusjonskravOgSøknadFinnes());
    }

    @Test
    void skal_prioritere_fravær_fra_im_over_fravær_fra_søknad_også_når_arbeidsgiver_opplyser_arbeidsforhold_og_kopiere_over_søknadsårsaker_fra_søknad() {
        Duration fraværIm = Duration.ofHours(3);
        Duration fraværSøknad = Duration.ofHours(6);
        // IM mottas først, men prioriteres likevel over søknad
        var innsendingIm = LocalDateTime.now().minusDays(2);
        var innsendingsSøknad = LocalDateTime.now().minusDays(1);

        Periode søknadsperiode = new Periode(LocalDate.now().minusDays(10), LocalDate.now());

        var im = InntektsmeldingBuilder.builder()
            .medJournalpostId("1")
            .medInnsendingstidspunkt(innsendingIm)
            .medOppgittFravær(List.of(new PeriodeAndel(søknadsperiode.getFom(), søknadsperiode.getTom(), fraværIm)))
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
            .medArbeidsforholdId(EksternArbeidsforholdRef.ref("ref1"))
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("AR123")
            .medRefusjon(BigDecimal.TEN)
            .build();
        var kravDokIm = mapTilKravdok(im).getKey();
        var fraværsperioderIm = mapTilKravdok(im).getValue();

        var jpSøknad = new JournalpostId("2");
        var kravDokSøknad = new KravDokument(jpSøknad, innsendingsSøknad, KravDokumentType.SØKNAD);
        var fraværsperioderSøknad = List.of(
            lagSøknadsperiode(jpSøknad, søknadsperiode.getFom(), søknadsperiode.getTom(), fraværSøknad, UttakArbeidType.ARBEIDSTAKER, im.getArbeidsgiver(), FraværÅrsak.SMITTEVERNHENSYN, SøknadÅrsak.NYOPPSTARTET_HOS_ARBEIDSGIVER));

        var input = Map.of(
            kravDokIm, fraværsperioderIm,
            kravDokSøknad, fraværsperioderSøknad);

        List<WrappedOppgittFraværPeriode> resultat = new KravDokumentFravær().trekkUtAlleFraværOgValiderOverlapp(input);

        assertThat(resultat).hasSize(1);
        WrappedOppgittFraværPeriode fp1 = resultat.get(0);
        assertThat(fp1.getKravDokumentType()).isEqualTo(KravDokumentType.INNTEKTSMELDING);
        assertThat(fp1.getPeriode().getFraværÅrsak()).isEqualTo(FraværÅrsak.SMITTEVERNHENSYN);
        assertThat(fp1.getPeriode().getSøknadÅrsak()).isEqualTo(SøknadÅrsak.NYOPPSTARTET_HOS_ARBEIDSGIVER);
        assertThat(fp1.getPeriode().getFraværPerDag()).isEqualTo(fraværIm);
        assertThat(fp1.getPeriode().getFom()).isEqualTo(LocalDate.now().minusDays(10));
        assertThat(fp1.getPeriode().getTom()).isEqualTo(LocalDate.now());
        assertThat(fp1.getSamtidigeKrav()).isEqualTo(SamtidigKravStatus.refusjonskravOgSøknadFinnes());

    }

    @Test
    void skal_slippe_gjennom_søknad_der_den_ikke_overlapper_IM_og_for_overlapp_skal_IM_prioriteres() {
        Duration fraværIm = Duration.ofHours(3);
        Duration fraværSøknad = Duration.ofHours(6);
        // IM mottas først, men prioriteres likevel over søknad
        var innsendingIm = LocalDateTime.now().minusDays(2);
        var innsendingsSøknad = LocalDateTime.now().minusDays(1);

        LocalDate idag = LocalDate.now();
        Periode søknadsperiodeSøknad = new Periode(idag.minusDays(10), idag.minusDays(2));
        Periode søknadsperiodeIm = new Periode(idag.minusDays(8), idag);

        var im = InntektsmeldingBuilder.builder()
            .medJournalpostId("1")
            .medInnsendingstidspunkt(innsendingIm)
            .medOppgittFravær(List.of(new PeriodeAndel(søknadsperiodeIm.getFom(), søknadsperiodeIm.getTom(), fraværIm)))
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
            .medArbeidsforholdId(EksternArbeidsforholdRef.ref("ref1"))
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("AR123")
            .medRefusjon(BigDecimal.TEN)
            .build();
        var kravDokIm = mapTilKravdok(im).getKey();
        var fraværsperioderIm = mapTilKravdok(im).getValue();

        var jpSøknad = new JournalpostId("2");
        var kravDokSøknad = new KravDokument(jpSøknad, innsendingsSøknad, KravDokumentType.SØKNAD);
        var fraværsperioderSøknad = List.of(
            lagSøknadsperiode(jpSøknad, søknadsperiodeSøknad.getFom(), søknadsperiodeSøknad.getTom(), fraværSøknad, UttakArbeidType.ARBEIDSTAKER, im.getArbeidsgiver(), FraværÅrsak.SMITTEVERNHENSYN, SøknadÅrsak.NYOPPSTARTET_HOS_ARBEIDSGIVER));

        var input = Map.of(
            kravDokIm, fraværsperioderIm,
            kravDokSøknad, fraværsperioderSøknad);

        List<WrappedOppgittFraværPeriode> resultat = new KravDokumentFravær().trekkUtAlleFraværOgValiderOverlapp(input);

        assertThat(resultat).hasSize(3);
        WrappedOppgittFraværPeriode fp1 = resultat.get(0);
        assertThat(fp1.getKravDokumentType()).isEqualTo(KravDokumentType.SØKNAD);
        assertThat(fp1.getSamtidigeKrav()).isEqualTo(SamtidigKravStatus.søknadFinnes());
        assertThat(fp1.getPeriode().getFraværÅrsak()).isEqualTo(FraværÅrsak.SMITTEVERNHENSYN);
        assertThat(fp1.getPeriode().getSøknadÅrsak()).isEqualTo(SøknadÅrsak.NYOPPSTARTET_HOS_ARBEIDSGIVER);
        assertThat(fp1.getPeriode().getFraværPerDag()).isEqualTo(fraværSøknad);
        assertThat(fp1.getPeriode().getFom()).isEqualTo(idag.minusDays(10));
        assertThat(fp1.getPeriode().getTom()).isEqualTo(idag.minusDays(9));

        WrappedOppgittFraværPeriode fp2 = resultat.get(1);
        assertThat(fp2.getKravDokumentType()).isEqualTo(KravDokumentType.INNTEKTSMELDING);
        assertThat(fp2.getSamtidigeKrav()).isEqualTo(SamtidigKravStatus.refusjonskravOgSøknadFinnes());
        assertThat(fp2.getPeriode().getFraværÅrsak()).isEqualTo(FraværÅrsak.SMITTEVERNHENSYN);
        assertThat(fp2.getPeriode().getSøknadÅrsak()).isEqualTo(SøknadÅrsak.NYOPPSTARTET_HOS_ARBEIDSGIVER);
        assertThat(fp2.getPeriode().getFraværPerDag()).isEqualTo(fraværIm);
        assertThat(fp2.getPeriode().getFom()).isEqualTo(idag.minusDays(8));
        assertThat(fp2.getPeriode().getTom()).isEqualTo(idag.minusDays(2));

        WrappedOppgittFraværPeriode fp3 = resultat.get(2);
        assertThat(fp3.getKravDokumentType()).isEqualTo(KravDokumentType.INNTEKTSMELDING);
        assertThat(fp3.getSamtidigeKrav()).isEqualTo(SamtidigKravStatus.refusjonskravFinnes());
        //ikke søknad for perioden, så ingen årsaker å kopiere
        assertThat(fp3.getPeriode().getFraværÅrsak()).isEqualTo(FraværÅrsak.UDEFINERT);
        assertThat(fp3.getPeriode().getSøknadÅrsak()).isEqualTo(SøknadÅrsak.UDEFINERT);
        assertThat(fp3.getPeriode().getFraværPerDag()).isEqualTo(fraværIm);
        assertThat(fp3.getPeriode().getFom()).isEqualTo(idag.minusDays(1));
        assertThat(fp3.getPeriode().getTom()).isEqualTo(idag);
    }

    @Test
    void skal_prioritere_fravær_fra_søknad_dersom_im_har_trekt_krav() {
        Duration fraværImMedRefusjon = Duration.ofHours(4);
        Duration fraværImTrektRefusjon = Duration.ZERO;
        Duration fraværSøknad = null;

        var imMedRefusjon = InntektsmeldingBuilder.builder()
            .medJournalpostId("1")
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(3))
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(10), LocalDate.now(), fraværImMedRefusjon)))
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medArbeidsforholdId(EksternArbeidsforholdRef.nullRef())
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("AR123")
            .medRefusjon(BigDecimal.TEN)
            .build();
        var imTrektRefusjon = InntektsmeldingBuilder.builder()
            .medJournalpostId("2")
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(2))
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(10), LocalDate.now(), fraværImTrektRefusjon)))
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medArbeidsforholdId(EksternArbeidsforholdRef.nullRef())
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("AR123")
            .medRefusjon(BigDecimal.TEN)
            .build();
        var kravdokImMedRefusjon = mapTilKravdok(imMedRefusjon);
        var kravdokImTrektRefusjon = mapTilKravdok(imTrektRefusjon);

        var jpSøknad = new JournalpostId("3");
        var kravdokSøknad = new KravDokument(jpSøknad, LocalDateTime.now().minusDays(1), KravDokumentType.SØKNAD);
        var fraværsperioderSøknad = List.of(
            lagSøknadsperiode(jpSøknad, LocalDate.now().minusDays(10), LocalDate.now(), null, UttakArbeidType.ARBEIDSTAKER, imTrektRefusjon.getArbeidsgiver()));

        var input = Map.of(
            kravdokImMedRefusjon.getKey(), kravdokImMedRefusjon.getValue(),
            kravdokImTrektRefusjon.getKey(), kravdokImTrektRefusjon.getValue(),
            kravdokSøknad, fraværsperioderSøknad);

        List<WrappedOppgittFraværPeriode> resultat = new KravDokumentFravær().trekkUtAlleFraværOgValiderOverlapp(input);

        assertThat(resultat).hasSize(1);
        WrappedOppgittFraværPeriode fp1 = resultat.get(0);
        assertThat(fp1.getPeriode().getFraværPerDag()).isEqualTo(fraværSøknad);
        assertThat(fp1.getPeriode().getFom()).isEqualTo(LocalDate.now().minusDays(10));
        assertThat(fp1.getPeriode().getTom()).isEqualTo(LocalDate.now());
        assertThat(fp1.getPeriode().getArbeidsgiver()).isEqualTo(Arbeidsgiver.virksomhet("000000000"));
        assertThat(fp1.getPeriode().getArbeidsforholdRef()).isEqualTo(InternArbeidsforholdRef.nullRef());
        assertThat(fp1.getSamtidigeKrav()).isEqualTo(SamtidigKravStatus.søknadFinnes().oppdaterInntektsmeldingTrekt());
    }


    @Test
    void skal_nulle_refusjonskrav_med_trekt_krav_selv_med_refusjonsbeløp_0() {
        // Refusjonsbeløp = 0 tolkes vanligvis som IM uten refusjonskrav, men trekt fravær vil overstyre dersom det også er oppgitt
        var imMedRefusjon = InntektsmeldingBuilder.builder()
            .medJournalpostId("1")
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(2))
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(10), LocalDate.now(), Duration.ofHours(4))))
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medArbeidsforholdId(EksternArbeidsforholdRef.nullRef())
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("AR123")
            .medRefusjon(BigDecimal.TEN)
            .build();
        var imTrektRefusjon = InntektsmeldingBuilder.builder()
            .medJournalpostId("2")
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(1))
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(10), LocalDate.now(), Duration.ZERO))) // Trekk av periode
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medArbeidsforholdId(EksternArbeidsforholdRef.nullRef())
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("AR123")
            .medRefusjon(BigDecimal.ZERO) // Gir KravDokumentType.INNTEKTSMELDING_UTEN_REFUSJONSKRAV
            .build();
        var kravdokImMedRefusjon = mapTilKravdok(imMedRefusjon);
        var kravdokImTrektRefusjon = mapTilKravdok(imTrektRefusjon);

        var input = Map.of(
            kravdokImMedRefusjon.getKey(), kravdokImMedRefusjon.getValue(),
            kravdokImTrektRefusjon.getKey(), kravdokImTrektRefusjon.getValue());

        List<WrappedOppgittFraværPeriode> resultat = new KravDokumentFravær().trekkUtAlleFraværOgValiderOverlapp(input);

        assertThat(resultat).hasSize(1);
        WrappedOppgittFraværPeriode fp1 = resultat.get(0);
        assertThat(fp1.getPeriode().getFraværPerDag()).isEqualTo(Duration.ZERO);
        assertThat(fp1.getPeriode().getFom()).isEqualTo(LocalDate.now().minusDays(10));
        assertThat(fp1.getPeriode().getTom()).isEqualTo(LocalDate.now());
        assertThat(fp1.getPeriode().getArbeidsgiver()).isEqualTo(Arbeidsgiver.virksomhet("000000000"));
        assertThat(fp1.getPeriode().getArbeidsforholdRef()).isEqualTo(InternArbeidsforholdRef.nullRef());
        assertThat(fp1.getSamtidigeKrav()).isEqualTo(SamtidigKravStatus.inntektsmeldingUtenRefusjonskravTrekt());
    }


    @Test
    void skal_prioritere_fravær_fra_søknad_dersom_im_har_trekt_krav_også_når_det_er_2_arbeidsforhold() {
        Duration fraværImMedRefusjon = Duration.ofHours(4);
        Duration fraværImTrektRefusjon = Duration.ZERO;
        Duration fraværSøknad = null;

        InternArbeidsforholdRef arbeidsforholdId1 = InternArbeidsforholdRef.nyRef();
        EksternArbeidsforholdRef eksternArbeidsforholdRef1 = EksternArbeidsforholdRef.ref("ref1");
        var im1MedRefusjon = InntektsmeldingBuilder.builder()
            .medJournalpostId("1")
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(3))
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(10), LocalDate.now(), fraværImMedRefusjon)))
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(arbeidsforholdId1)
            .medArbeidsforholdId(eksternArbeidsforholdRef1)
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("AR123")
            .medRefusjon(BigDecimal.TEN)
            .build();
        var im1TrektRefusjon = InntektsmeldingBuilder.builder()
            .medJournalpostId("2")
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(2))
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(10), LocalDate.now(), fraværImTrektRefusjon)))
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(arbeidsforholdId1)
            .medArbeidsforholdId(eksternArbeidsforholdRef1)
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("AR123")
            .medRefusjon(BigDecimal.TEN)
            .build();
        InternArbeidsforholdRef arbeidsforholdId2 = InternArbeidsforholdRef.nyRef();
        EksternArbeidsforholdRef eksternArbeidsforholdRef2 = EksternArbeidsforholdRef.ref("ref1");
        var im2MedRefusjon = InntektsmeldingBuilder.builder()
            .medJournalpostId("3")
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(3))
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(10), LocalDate.now(), fraværImMedRefusjon)))
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(arbeidsforholdId2)
            .medArbeidsforholdId(eksternArbeidsforholdRef2)
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("AR123")
            .medRefusjon(BigDecimal.TEN)
            .build();
        var im2TrektRefusjon = InntektsmeldingBuilder.builder()
            .medJournalpostId("4")
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(2))
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(10), LocalDate.now(), fraværImTrektRefusjon)))
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(arbeidsforholdId2)
            .medArbeidsforholdId(eksternArbeidsforholdRef2)
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("AR123")
            .medRefusjon(BigDecimal.TEN)
            .build();


        var kravdokIm1MedRefusjon = mapTilKravdok(im1MedRefusjon);
        var kravdokIm1TrektRefusjon = mapTilKravdok(im1TrektRefusjon);
        var kravdokIm2MedRefusjon = mapTilKravdok(im2MedRefusjon);
        var kravdokIm2TrektRefusjon = mapTilKravdok(im2TrektRefusjon);

        var jpSøknad = new JournalpostId("3");
        var kravdokSøknad = new KravDokument(jpSøknad, LocalDateTime.now().minusDays(1), KravDokumentType.SØKNAD);
        var fraværsperioderSøknad = List.of(
            lagSøknadsperiode(jpSøknad, LocalDate.now().minusDays(10), LocalDate.now(), null, UttakArbeidType.ARBEIDSTAKER, im1TrektRefusjon.getArbeidsgiver()));

        var input = Map.of(
            kravdokIm1MedRefusjon.getKey(), kravdokIm1MedRefusjon.getValue(),
            kravdokIm1TrektRefusjon.getKey(), kravdokIm1TrektRefusjon.getValue(),
            kravdokIm2MedRefusjon.getKey(), kravdokIm2MedRefusjon.getValue(),
            kravdokIm2TrektRefusjon.getKey(), kravdokIm2TrektRefusjon.getValue(),
            kravdokSøknad, fraværsperioderSøknad);

        List<WrappedOppgittFraværPeriode> resultat = new KravDokumentFravær().trekkUtAlleFraværOgValiderOverlapp(input);

        assertThat(resultat).hasSize(1);
        WrappedOppgittFraværPeriode fp1 = resultat.get(0);
        assertThat(fp1.getPeriode().getFraværPerDag()).isEqualTo(fraværSøknad);
        assertThat(fp1.getPeriode().getFom()).isEqualTo(LocalDate.now().minusDays(10));
        assertThat(fp1.getPeriode().getTom()).isEqualTo(LocalDate.now());
        assertThat(fp1.getPeriode().getArbeidsgiver()).isEqualTo(Arbeidsgiver.virksomhet("000000000"));
        assertThat(fp1.getPeriode().getArbeidsforholdRef()).isEqualTo(InternArbeidsforholdRef.nullRef()); //bare IM som har arbeidsforhold, og kravet fra IM er trekt
        assertThat(fp1.getSamtidigeKrav()).isEqualTo(SamtidigKravStatus.søknadFinnes().oppdaterInntektsmeldingTrekt());
    }

    @Test
    void skal_filtrere_inntektsmelding_uten_refusjonskrav() {
        var im = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medArbeidsforholdId(EksternArbeidsforholdRef.nullRef())
            .medJournalpostId("1")
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(10))
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("AR123")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(30), LocalDate.now().minusDays(25))))
            .medRefusjon(BigDecimal.ZERO) // Gir KravDokumentType.INNTEKTSMELDING_UTEN_REFUSJONSKRAV
            .build();
        var input = Map.ofEntries(mapTilKravdok(im));

        List<WrappedOppgittFraværPeriode> oppgittFraværPeriode = new KravDokumentFravær().trekkUtAlleFraværOgValiderOverlapp(input);

        assertThat(oppgittFraværPeriode).isEmpty();
    }

    @Test
    void skal_støtte_flere_arbeidssteder_i_samme_søknad() {
        //se TSF-2491

        Arbeidsgiver virksomhet1 = Arbeidsgiver.virksomhet("000000000");
        Arbeidsgiver virksomhet2 = Arbeidsgiver.virksomhet("000000001");

        Duration fraværVirksomhet1 = Duration.ofHours(1);
        Duration fraværVirksomhet2 = Duration.ofHours(2);
        // IM mottas først, men prioriteres likevel over søknad
        var innsendingsSøknad = LocalDateTime.now().minusDays(1);

        var jpSøknad = new JournalpostId("1");
        var kravDokSøknad = new KravDokument(jpSøknad, innsendingsSøknad, KravDokumentType.SØKNAD);
        var fraværsperioderSøknad = List.of(
            lagSøknadsperiode(jpSøknad, LocalDate.now().minusDays(10), LocalDate.now(), fraværVirksomhet1, UttakArbeidType.ARBEIDSTAKER, virksomhet1),
            lagSøknadsperiode(jpSøknad, LocalDate.now().minusDays(10), LocalDate.now(), fraværVirksomhet2, UttakArbeidType.ARBEIDSTAKER, virksomhet2));

        var input = Map.of(
            kravDokSøknad, fraværsperioderSøknad);

        List<WrappedOppgittFraværPeriode> resultat = new KravDokumentFravær().trekkUtAlleFraværOgValiderOverlapp(input);

        resultat = resultat.stream().sorted(Comparator.comparing(wofp -> wofp.getPeriode().getArbeidsgiver().getOrgnr())).toList();
        assertThat(resultat).hasSize(2);
        WrappedOppgittFraværPeriode fp1 = resultat.get(0);
        assertThat(fp1.getPeriode().getArbeidsgiver()).isEqualTo(virksomhet1);
        assertThat(fp1.getPeriode().getFraværPerDag()).isEqualTo(fraværVirksomhet1);
        assertThat(fp1.getPeriode().getFom()).isEqualTo(LocalDate.now().minusDays(10));
        assertThat(fp1.getPeriode().getTom()).isEqualTo(LocalDate.now());
        assertThat(fp1.getSamtidigeKrav()).isEqualTo(SamtidigKravStatus.søknadFinnes());
        WrappedOppgittFraværPeriode fp2 = resultat.get(1);
        assertThat(fp2.getPeriode().getArbeidsgiver()).isEqualTo(virksomhet2);
        assertThat(fp2.getPeriode().getFraværPerDag()).isEqualTo(fraværVirksomhet2);
        assertThat(fp2.getPeriode().getFom()).isEqualTo(LocalDate.now().minusDays(10));
        assertThat(fp2.getPeriode().getTom()).isEqualTo(LocalDate.now());
        assertThat(fp2.getSamtidigeKrav()).isEqualTo(SamtidigKravStatus.søknadFinnes());
    }

    private static Map.Entry<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> mapTilKravdok(Inntektsmelding im) {
        if (im.getOppgittFravær().size() != 1) {
            throw new IllegalArgumentException("Testmetode søtter bare IM med én fraværsperiode");
        }
        var fom = im.getOppgittFravær().get(0).getFom();
        var tom = im.getOppgittFravær().get(0).getTom();
        var fraværPerDag = im.getOppgittFravær().get(0).getVarighetPerDag();
        var jpId = im.getJournalpostId();
        var kravDokumentType = im.getRefusjonBeløpPerMnd().getVerdi().compareTo(BigDecimal.ZERO) > 0
            ? KravDokumentType.INNTEKTSMELDING
            : KravDokumentType.INNTEKTSMELDING_UTEN_REFUSJONSKRAV;

        return Map.entry(new KravDokument(jpId, im.getInnsendingstidspunkt(), kravDokumentType),
            List.of(lagFraværsperiodeIm(jpId, fom, tom, fraværPerDag, im.getArbeidsgiver(), im.getArbeidsforholdRef())));
    }

    private static VurdertSøktPeriode<OppgittFraværPeriode> lagFraværsperiodeIm(JournalpostId journalpost, LocalDate fom, LocalDate tom, Duration fraværPerDag, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        OppgittFraværPeriode op1 = new OppgittFraværPeriode(journalpost, fom, tom, UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, arbeidsforholdRef, fraværPerDag, FraværÅrsak.UDEFINERT, SøknadÅrsak.UDEFINERT);
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        return new VurdertSøktPeriode<>(periode, UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, arbeidsforholdRef, Utfall.OPPFYLT, op1);
    }


    private static VurdertSøktPeriode<OppgittFraværPeriode> lagSøknadsperiode(JournalpostId journalpost, LocalDate fom, LocalDate tom, UttakArbeidType uttakArbeidType) {
        return lagSøknadsperiode(journalpost, fom, tom, null, uttakArbeidType, null, FraværÅrsak.UDEFINERT, SøknadÅrsak.UDEFINERT);
    }

    private static VurdertSøktPeriode<OppgittFraværPeriode> lagSøknadsperiode(JournalpostId journalpost, LocalDate fom, LocalDate tom, Duration fraværPerDag, UttakArbeidType uttakArbeidType, Arbeidsgiver arbeidsgiver) {
        return lagSøknadsperiode(journalpost, fom, tom, fraværPerDag, uttakArbeidType, arbeidsgiver, FraværÅrsak.ORDINÆRT_FRAVÆR, SøknadÅrsak.NYOPPSTARTET_HOS_ARBEIDSGIVER);
    }

    private static VurdertSøktPeriode<OppgittFraværPeriode> lagSøknadsperiode(JournalpostId journalpost, LocalDate fom, LocalDate tom, Duration fraværPerDag, UttakArbeidType uttakArbeidType, Arbeidsgiver arbeidsgiver, FraværÅrsak fraværÅrsak, SøknadÅrsak søknadÅrsak) {
        if (uttakArbeidType == UttakArbeidType.ARBEIDSTAKER) {
            if (arbeidsgiver == null) {
                throw new IllegalArgumentException("Må opplyse arbeidsgiver");
            }
        } else {
            if (arbeidsgiver != null) {
                throw new IllegalArgumentException("Kan ikke opplyse om arbeidsgiver");
            }
        }
        InternArbeidsforholdRef arbeidsforholdRef = InternArbeidsforholdRef.nullRef();
        OppgittFraværPeriode op1 = new OppgittFraværPeriode(journalpost, fom, tom, uttakArbeidType, arbeidsgiver, arbeidsforholdRef, fraværPerDag, fraværÅrsak, søknadÅrsak);
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        return new VurdertSøktPeriode<>(periode, uttakArbeidType, arbeidsgiver, arbeidsforholdRef, Utfall.OPPFYLT, op1);
    }


}
