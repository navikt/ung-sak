package no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
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

    Arbeidsgiver virksomhet1 = Arbeidsgiver.virksomhet("000000000");
    Arbeidsgiver virksomhet2 = Arbeidsgiver.virksomhet("000000001");

    AktivitetTypeArbeidsgiver aktivitetArbeidsgiver1 = new AktivitetTypeArbeidsgiver(UttakArbeidType.ARBEIDSTAKER, virksomhet1);
    AktivitetTypeArbeidsgiver aktivitetArbeidsgiver2 = new AktivitetTypeArbeidsgiver(UttakArbeidType.ARBEIDSTAKER, virksomhet2);

    @Test
    void skal_bygge_tidslinje_av_fravær_fra_inntektsmeldinger() {

        var inntektsmelding1 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(virksomhet1)
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
            .medArbeidsgiver(virksomhet2)
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
            .medArbeidsgiver(virksomhet1)
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
            .medArbeidsgiver(virksomhet1)
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


        var resultat = new KravDokumentFravær().trekkUtFravær(input);
        assertThat(resultat).containsOnlyKeys(aktivitetArbeidsgiver1, aktivitetArbeidsgiver2);
        assertThat(resultat.get(aktivitetArbeidsgiver1).stream().map(segment -> segment.getValue().samtidigKravStatus()).toList()).containsOnly(kravStatusForRefusjonskravFinnes(InternArbeidsforholdRef.nullRef()));
        assertThat(resultat.get(aktivitetArbeidsgiver1).stream().map(LocalDateSegment::getLocalDateInterval).toList()).containsOnly(
            new LocalDateInterval(LocalDate.now().minusDays(30), LocalDate.now().minusDays(25)),
            new LocalDateInterval(LocalDate.now(), LocalDate.now().plusDays(5)));
        assertThat(resultat.get(aktivitetArbeidsgiver2).stream().map(segment -> segment.getValue().samtidigKravStatus()).toList()).containsOnly(kravStatusForRefusjonskravFinnes(InternArbeidsforholdRef.nullRef()));
        assertThat(resultat.get(aktivitetArbeidsgiver2).stream().map(LocalDateSegment::getLocalDateInterval).toList()).containsOnly(
            new LocalDateInterval(LocalDate.now().minusDays(26), LocalDate.now().minusDays(25)));
    }

    @Test
    void skal_rydde_i_berørte_tidslinjer() {
        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        LocalDateTime innsendingstidspunkt1 = LocalDateTime.now().minusDays(10);
        LocalDateTime innsendingstidspunkt2 = LocalDateTime.now().minusDays(9);
        var inntektsmelding1 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(virksomhet1)
            .medArbeidsforholdId(arbeidsforholdId)
            .medArbeidsforholdId(EksternArbeidsforholdRef.ref("asdf"))
            .medJournalpostId("1")
            .medInnsendingstidspunkt(innsendingstidspunkt1)
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("AR123")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(30), LocalDate.now().minusDays(25))))
            .medRefusjon(BigDecimal.TEN)
            .build();
        var inntektsmelding2 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(virksomhet1)
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medArbeidsforholdId(EksternArbeidsforholdRef.nullRef())
            .medInnsendingstidspunkt(innsendingstidspunkt2)
            .medJournalpostId("2")
            .medBeløp(BigDecimal.ONE)
            .medKanalreferanse("AR124")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(28), LocalDate.now().minusDays(25))))
            .medRefusjon(BigDecimal.ONE)
            .build();
        var input = Map.ofEntries(
            mapTilKravdok(inntektsmelding1), mapTilKravdok(inntektsmelding2));

        var resultat = new KravDokumentFravær().trekkUtFravær(input);

        assertThat(resultat).containsOnlyKeys(aktivitetArbeidsgiver1);
        LocalDateTimeline<OppgittFraværHolder> resultatTidslinje = resultat.get(aktivitetArbeidsgiver1);

        LocalDateTimeline<OppgittFraværHolder> fasit = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(LocalDate.now().minusDays(30), LocalDate.now().minusDays(29), OppgittFraværHolder.fraRefusjonskrav(arbeidsforholdId, new OppgittFraværVerdi(innsendingstidspunkt1, null, FraværÅrsak.UDEFINERT, SøknadÅrsak.UDEFINERT, Utfall.OPPFYLT))),
            new LocalDateSegment<>(LocalDate.now().minusDays(28), LocalDate.now().minusDays(25), OppgittFraværHolder.fraRefusjonskrav(InternArbeidsforholdRef.nullRef(), new OppgittFraværVerdi(innsendingstidspunkt2, null, FraværÅrsak.UDEFINERT, SøknadÅrsak.UDEFINERT, Utfall.OPPFYLT)))
        ));
        assertThat(resultatTidslinje).isEqualTo(fasit);
    }

    @Test
    void skal_rydde_i_berørte_tidslinjer_reverse() {
        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        LocalDateTime innsendingstidspunkt1 = LocalDateTime.now().minusDays(9);
        LocalDateTime innsendingstidspunkt2 = LocalDateTime.now().minusDays(10);
        var inntektsmelding1 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(virksomhet1)
            .medArbeidsforholdId(arbeidsforholdId)
            .medArbeidsforholdId(EksternArbeidsforholdRef.ref("asdf"))
            .medJournalpostId("1")
            .medInnsendingstidspunkt(innsendingstidspunkt1)
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("AR124")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(30), LocalDate.now().minusDays(25))))
            .medRefusjon(BigDecimal.TEN)
            .build();
        var inntektsmelding2 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(virksomhet1)
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medArbeidsforholdId(EksternArbeidsforholdRef.nullRef())
            .medInnsendingstidspunkt(innsendingstidspunkt2)
            .medJournalpostId("2")
            .medBeløp(BigDecimal.ONE)
            .medKanalreferanse("AR123")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(28), LocalDate.now().minusDays(25))))
            .medRefusjon(BigDecimal.ONE)
            .build();
        var input = Map.ofEntries(
            mapTilKravdok(inntektsmelding1), mapTilKravdok(inntektsmelding2));

        var resultat = new KravDokumentFravær().trekkUtFravær(input);

        assertThat(resultat).containsOnlyKeys(aktivitetArbeidsgiver1);
        LocalDateTimeline<OppgittFraværHolder> resultatTidslinje = resultat.get(aktivitetArbeidsgiver1);

        LocalDateTimeline<OppgittFraværHolder> fasit = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(LocalDate.now().minusDays(30), LocalDate.now().minusDays(25), OppgittFraværHolder.fraRefusjonskrav(arbeidsforholdId, new OppgittFraværVerdi(innsendingstidspunkt1, null, FraværÅrsak.UDEFINERT, SøknadÅrsak.UDEFINERT, Utfall.OPPFYLT)))
        ));
        assertThat(resultatTidslinje).isEqualTo(fasit);
    }

    @Test
    void skal_rydde_i_berørte_tidslinjer_reverse_2() {
        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        LocalDateTime innsendingstidspunkt1 = LocalDateTime.now().minusDays(9);
        LocalDateTime innsendingstidspunkt2 = LocalDateTime.now().minusDays(10);
        var inntektsmelding1 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(virksomhet1)
            .medArbeidsforholdId(arbeidsforholdId)
            .medArbeidsforholdId(EksternArbeidsforholdRef.ref("asdf"))
            .medJournalpostId("1")
            .medInnsendingstidspunkt(innsendingstidspunkt1)
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("AR124")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(30), LocalDate.now().minusDays(25))))
            .medRefusjon(BigDecimal.TEN)
            .build();
        var inntektsmelding2 = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(virksomhet1)
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medArbeidsforholdId(EksternArbeidsforholdRef.nullRef())
            .medInnsendingstidspunkt(innsendingstidspunkt2)
            .medJournalpostId("2")
            .medBeløp(BigDecimal.ONE)
            .medKanalreferanse("AR123")
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(28), LocalDate.now().minusDays(22))))
            .medRefusjon(BigDecimal.ONE)
            .build();
        var input = Map.ofEntries(
            mapTilKravdok(inntektsmelding1), mapTilKravdok(inntektsmelding2));

        var resultat = new KravDokumentFravær().trekkUtFravær(input);

        assertThat(resultat).containsOnlyKeys(aktivitetArbeidsgiver1);
        LocalDateTimeline<OppgittFraværHolder> resultatTidslinje = resultat.get(aktivitetArbeidsgiver1);

        LocalDateTimeline<OppgittFraværHolder> fasit = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(LocalDate.now().minusDays(30), LocalDate.now().minusDays(25), OppgittFraværHolder.fraRefusjonskrav(arbeidsforholdId, new OppgittFraværVerdi(innsendingstidspunkt1, null, FraværÅrsak.UDEFINERT, SøknadÅrsak.UDEFINERT, Utfall.OPPFYLT))),
            new LocalDateSegment<>(LocalDate.now().minusDays(24), LocalDate.now().minusDays(22), OppgittFraværHolder.fraRefusjonskrav(InternArbeidsforholdRef.nullRef(), new OppgittFraværVerdi(innsendingstidspunkt2, null, FraværÅrsak.UDEFINERT, SøknadÅrsak.UDEFINERT, Utfall.OPPFYLT)))
        ));
        assertThat(resultatTidslinje).isEqualTo(fasit);
    }

    @Test
    void skal_takle_søknader_om_utbetaling_til_frilanser() {
        JournalpostId journalpost1 = new JournalpostId("1");
        JournalpostId journalpost2 = new JournalpostId("2");
        LocalDateTime nå = LocalDateTime.now();
        LocalDate idag = nå.toLocalDate();
        LocalDateTime innsendingstidspunkt1 = nå.minusMinutes(15);
        LocalDateTime innsendingstidspunkt2 = nå.minusMinutes(5);
        var kravDok1 = new KravDokument(journalpost1, innsendingstidspunkt1, KravDokumentType.SØKNAD);
        var kravDok2 = new KravDokument(journalpost2, innsendingstidspunkt2, KravDokumentType.SØKNAD);

        var input = Map.of(
            kravDok1, List.of(lagSøknadsperiode(journalpost1, idag.minusDays(10), idag.minusDays(9), UttakArbeidType.FRILANSER)),
            kravDok2, List.of(lagSøknadsperiode(journalpost2, idag.minusDays(5), idag.minusDays(5), UttakArbeidType.FRILANSER)));

        var resultat = new KravDokumentFravær().trekkUtFravær(input);

        assertThat(resultat).containsOnlyKeys(new AktivitetTypeArbeidsgiver(UttakArbeidType.FRILANSER, null));
        LocalDateTimeline<OppgittFraværHolder> resultatTidslinje = resultat.get(new AktivitetTypeArbeidsgiver(UttakArbeidType.FRILANSER, null));

        LocalDateTimeline<OppgittFraværHolder> fasit = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(LocalDate.now().minusDays(10), LocalDate.now().minusDays(9), OppgittFraværHolder.fraSøknad(new OppgittFraværVerdi(innsendingstidspunkt1, null, FraværÅrsak.UDEFINERT, SøknadÅrsak.UDEFINERT, Utfall.OPPFYLT))),
            new LocalDateSegment<>(LocalDate.now().minusDays(5), LocalDate.now().minusDays(5), OppgittFraværHolder.fraSøknad(new OppgittFraværVerdi(innsendingstidspunkt2, null, FraværÅrsak.UDEFINERT, SøknadÅrsak.UDEFINERT, Utfall.OPPFYLT)))
        ));
        assertThat(resultatTidslinje).isEqualTo(fasit);

    }

    @Test
    void skal_prioritere_fravær_fra_im_over_fravær_fra_søknad() {
        Duration fraværIm = Duration.ofHours(4);
        Duration fraværSøknad = null;
        // IM mottas først, men prioriteres likevel over søknad
        var innsendingIm = LocalDateTime.now().minusDays(2);
        var innsendingsSøknad = LocalDateTime.now().minusDays(1);

        LocalDate tom = LocalDate.now();
        LocalDate fom = tom.minusDays(10);
        var im = InntektsmeldingBuilder.builder()
            .medJournalpostId("1")
            .medInnsendingstidspunkt(innsendingIm)
            .medOppgittFravær(List.of(new PeriodeAndel(fom, tom, fraværIm)))
            .medArbeidsgiver(virksomhet1)
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
            lagSøknadsperiode(jpSøknad, fom, tom, fraværSøknad, UttakArbeidType.ARBEIDSTAKER, im.getArbeidsgiver()));

        var input = Map.of(
            kravDokIm, fraværsperioderIm,
            kravDokSøknad, fraværsperioderSøknad);

        var resultat = new KravDokumentFravær().trekkUtFravær(input);

        assertThat(resultat).containsOnlyKeys(aktivitetArbeidsgiver1);
        LocalDateTimeline<OppgittFraværHolder> resultatTidslinje = resultat.get(aktivitetArbeidsgiver1);

        LocalDateTimeline<OppgittFraværHolder> fasit = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, tom, OppgittFraværHolder.fraRefusjonskrav(InternArbeidsforholdRef.nullRef(), new OppgittFraværVerdi(innsendingIm, fraværIm, FraværÅrsak.UDEFINERT, SøknadÅrsak.UDEFINERT, Utfall.OPPFYLT))
                .oppdaterMed(OppgittFraværHolder.fraSøknad(new OppgittFraværVerdi(innsendingsSøknad, fraværSøknad, FraværÅrsak.ORDINÆRT_FRAVÆR, SøknadÅrsak.NYOPPSTARTET_HOS_ARBEIDSGIVER, Utfall.OPPFYLT)))
            )
        ));
        assertThat(resultatTidslinje).isEqualTo(fasit);
    }

    @Test
    void skal_prioritere_fravær_fra_im_over_fravær_fra_søknad_også_når_arbeidsgiver_opplyser_arbeidsforhold_og_kopiere_over_søknadsårsaker_fra_søknad() {
        Duration fraværIm = Duration.ofHours(3);
        Duration fraværSøknad = Duration.ofHours(6);
        // IM mottas først, men prioriteres likevel over søknad
        var innsendingIm = LocalDateTime.now().minusDays(2);
        var innsendingsSøknad = LocalDateTime.now().minusDays(1);

        Periode søknadsperiode = new Periode(LocalDate.now().minusDays(10), LocalDate.now());

        InternArbeidsforholdRef arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        var im = InntektsmeldingBuilder.builder()
            .medJournalpostId("1")
            .medInnsendingstidspunkt(innsendingIm)
            .medOppgittFravær(List.of(new PeriodeAndel(søknadsperiode.getFom(), søknadsperiode.getTom(), fraværIm)))
            .medArbeidsgiver(virksomhet1)
            .medArbeidsforholdId(arbeidsforholdId)
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

        var resultat = new KravDokumentFravær().trekkUtFravær(input);

        assertThat(resultat).containsOnlyKeys(aktivitetArbeidsgiver1);
        LocalDateTimeline<OppgittFraværHolder> resultatTidslinje = resultat.get(aktivitetArbeidsgiver1);

        LocalDateTimeline<OppgittFraværHolder> fasit = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(søknadsperiode.getFom(), søknadsperiode.getTom(), OppgittFraværHolder.fraRefusjonskrav(arbeidsforholdId, new OppgittFraværVerdi(innsendingIm, fraværIm, FraværÅrsak.UDEFINERT, SøknadÅrsak.UDEFINERT, Utfall.OPPFYLT))
                .oppdaterMed(OppgittFraværHolder.fraSøknad(new OppgittFraværVerdi(innsendingsSøknad, fraværSøknad, FraværÅrsak.SMITTEVERNHENSYN, SøknadÅrsak.NYOPPSTARTET_HOS_ARBEIDSGIVER, Utfall.OPPFYLT)))
            )
        ));
        assertThat(resultatTidslinje).isEqualTo(fasit);

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

        InternArbeidsforholdRef arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        var im = InntektsmeldingBuilder.builder()
            .medJournalpostId("1")
            .medInnsendingstidspunkt(innsendingIm)
            .medOppgittFravær(List.of(new PeriodeAndel(søknadsperiodeIm.getFom(), søknadsperiodeIm.getTom(), fraværIm)))
            .medArbeidsgiver(virksomhet1)
            .medArbeidsforholdId(arbeidsforholdId)
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

        var resultat = new KravDokumentFravær().trekkUtFravær(input);
        assertThat(resultat).containsOnlyKeys(aktivitetArbeidsgiver1);
        LocalDateTimeline<OppgittFraværHolder> resultatTidslinje = resultat.get(aktivitetArbeidsgiver1);

        var segmenter = new ArrayList<>(resultatTidslinje.toSegments());
        var segment1 = segmenter.get(0);
        assertThat(segment1.getFom()).isEqualTo(idag.minusDays(10));
        assertThat(segment1.getTom()).isEqualTo(idag.minusDays(9));
        assertThat(segment1.getValue().søknadGjelder()).isTrue();
        assertThat(segment1.getValue().samtidigKravStatus()).isEqualTo(kravStatusForSøknadFinnes());
        assertThat(segment1.getValue().getSøknad().fraværPerDag()).isEqualTo(fraværSøknad);
        assertThat(segment1.getValue().fraværÅrsak()).isEqualTo(FraværÅrsak.SMITTEVERNHENSYN);
        assertThat(segment1.getValue().søknadÅrsak()).isEqualTo(SøknadÅrsak.NYOPPSTARTET_HOS_ARBEIDSGIVER);
        var segment2 = segmenter.get(1);
        assertThat(segment2.getFom()).isEqualTo(idag.minusDays(8));
        assertThat(segment2.getTom()).isEqualTo(idag.minusDays(2));
        assertThat(segment2.getValue().søknadGjelder()).isFalse();
        assertThat(segment2.getValue().refusjonskravGjelder()).isTrue();
        assertThat(segment2.getValue().samtidigKravStatus()).isEqualTo(kravStatusForRefusjonskravOgSøknadFinnes(arbeidsforholdId));
        assertThat(segment2.getValue().getRefusjonskrav().get(arbeidsforholdId).fraværPerDag()).isEqualTo(fraværIm);
        assertThat(segment2.getValue().fraværÅrsak()).isEqualTo(FraværÅrsak.SMITTEVERNHENSYN);
        assertThat(segment2.getValue().søknadÅrsak()).isEqualTo(SøknadÅrsak.NYOPPSTARTET_HOS_ARBEIDSGIVER);
        var segment3 = segmenter.get(2);
        assertThat(segment3.getFom()).isEqualTo(idag.minusDays(1));
        assertThat(segment3.getTom()).isEqualTo(idag);
        assertThat(segment3.getValue().søknadGjelder()).isFalse();
        assertThat(segment3.getValue().refusjonskravGjelder()).isTrue();
        assertThat(segment3.getValue().samtidigKravStatus()).isEqualTo(kravStatusForRefusjonskravFinnes(arbeidsforholdId));
        assertThat(segment2.getValue().getRefusjonskrav().get(arbeidsforholdId).fraværPerDag()).isEqualTo(fraværIm);
        assertThat(segment3.getValue().fraværÅrsak()).isEqualTo(FraværÅrsak.UDEFINERT);
        assertThat(segment3.getValue().søknadÅrsak()).isEqualTo(SøknadÅrsak.UDEFINERT);
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
            .medArbeidsgiver(virksomhet1)
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
            .medArbeidsgiver(virksomhet1)
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

        var resultat = new KravDokumentFravær().trekkUtFravær(input);

        assertThat(resultat).containsOnlyKeys(aktivitetArbeidsgiver1);
        LocalDateTimeline<OppgittFraværHolder> resultatTidslinje = resultat.get(aktivitetArbeidsgiver1);
        var segmenter = new ArrayList<>(resultatTidslinje.toSegments());
        assertThat(segmenter).hasSize(1);
        var segment1 = segmenter.get(0);
        assertThat(segment1.getFom()).isEqualTo(LocalDate.now().minusDays(10));
        assertThat(segment1.getTom()).isEqualTo(LocalDate.now());
        assertThat(segment1.getValue().søknadGjelder()).isTrue();
        assertThat(segment1.getValue().samtidigKravStatus()).isEqualTo(kravStatusForSøknadOgRefusjonskrav(SamtidigKravStatus.KravStatus.FINNES, SamtidigKravStatus.KravStatus.TREKT, InternArbeidsforholdRef.nullRef()));
        assertThat(segment1.getValue().getSøknad().fraværPerDag()).isEqualTo(fraværSøknad);

    }

    @Test
    void skal_nulle_refusjonskrav_med_trekt_krav_selv_med_refusjonsbeløp_0() {
        // Refusjonsbeløp = 0 tolkes vanligvis som IM uten refusjonskrav, men trekt fravær vil overstyre dersom det også er oppgitt
        var imMedRefusjon = InntektsmeldingBuilder.builder()
            .medJournalpostId("1")
            .medInnsendingstidspunkt(LocalDateTime.now().minusDays(2))
            .medOppgittFravær(List.of(new PeriodeAndel(LocalDate.now().minusDays(10), LocalDate.now(), Duration.ofHours(4))))
            .medArbeidsgiver(virksomhet1)
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
            .medArbeidsgiver(virksomhet1)
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

        var resultat = new KravDokumentFravær().trekkUtFravær(input);

        assertThat(resultat).containsOnlyKeys(aktivitetArbeidsgiver1);
        LocalDateTimeline<OppgittFraværHolder> resultatTidslinje = resultat.get(aktivitetArbeidsgiver1);
        var segmenter = new ArrayList<>(resultatTidslinje.toSegments());
        assertThat(segmenter).hasSize(1);
        var segment1 = segmenter.get(0);
        assertThat(segment1.getFom()).isEqualTo(LocalDate.now().minusDays(10));
        assertThat(segment1.getTom()).isEqualTo(LocalDate.now());
        assertThat(segment1.getValue().refusjonskravGjelder()).isTrue();
        assertThat(segment1.getValue().getRefusjonskrav().get(InternArbeidsforholdRef.nullRef()).fraværPerDag()).isEqualTo(Duration.ZERO);
        assertThat(segment1.getValue().samtidigKravStatus()).isEqualTo(kravStatusForBeggeImTyperTrekt(InternArbeidsforholdRef.nullRef()));
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
            .medArbeidsgiver(virksomhet1)
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
            .medArbeidsgiver(virksomhet1)
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
            .medArbeidsgiver(virksomhet1)
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
            .medArbeidsgiver(virksomhet1)
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

        var resultat = new KravDokumentFravær().trekkUtFravær(input);

        assertThat(resultat).containsOnlyKeys(aktivitetArbeidsgiver1);
        LocalDateTimeline<OppgittFraværHolder> resultatTidslinje = resultat.get(aktivitetArbeidsgiver1);
        var segmenter = new ArrayList<>(resultatTidslinje.toSegments());
        assertThat(segmenter).hasSize(1);
        var segment1 = segmenter.get(0);
        assertThat(segment1.getFom()).isEqualTo(LocalDate.now().minusDays(10));
        assertThat(segment1.getTom()).isEqualTo(LocalDate.now());
        assertThat(segment1.getValue().søknadGjelder()).isTrue();
        assertThat(segment1.getValue().getSøknad().fraværPerDag()).isEqualTo(fraværSøknad);
        assertThat(segment1.getValue().samtidigKravStatus()).isEqualTo(kravStatusForSøknadOgRefusjonskrav(SamtidigKravStatus.KravStatus.FINNES, SamtidigKravStatus.KravStatus.TREKT, arbeidsforholdId1, arbeidsforholdId2));
    }

    @Test
    void skal_filtrere_inntektsmelding_uten_refusjonskrav() {
        var im = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(virksomhet1)
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

        var resultat = new KravDokumentFravær().trekkUtFravær(input);

        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_støtte_flere_arbeidssteder_i_samme_søknad() {
        //se TSF-2491

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

        var resultat = new KravDokumentFravær().trekkUtFravær(input);

        assertThat(resultat).containsOnlyKeys(aktivitetArbeidsgiver1, aktivitetArbeidsgiver2);
        LocalDateTimeline<OppgittFraværHolder> resultatTidslinje1 = resultat.get(aktivitetArbeidsgiver1);
        var segmenter1 = new ArrayList<>(resultatTidslinje1.toSegments());
        assertThat(segmenter1).hasSize(1);
        var segment1_1 = segmenter1.get(0);
        assertThat(segment1_1.getFom()).isEqualTo(LocalDate.now().minusDays(10));
        assertThat(segment1_1.getTom()).isEqualTo(LocalDate.now());
        assertThat(segment1_1.getValue().søknadGjelder()).isTrue();
        assertThat(segment1_1.getValue().getSøknad().fraværPerDag()).isEqualTo(fraværVirksomhet1);
        assertThat(segment1_1.getValue().samtidigKravStatus()).isEqualTo(kravStatusForSøknadFinnes());

        LocalDateTimeline<OppgittFraværHolder> resultatTidslinje2 = resultat.get(aktivitetArbeidsgiver2);
        var segmenter2 = new ArrayList<>(resultatTidslinje2.toSegments());
        assertThat(segmenter2).hasSize(1);
        var segment2_1 = segmenter2.get(0);
        assertThat(segment2_1.getFom()).isEqualTo(LocalDate.now().minusDays(10));
        assertThat(segment2_1.getTom()).isEqualTo(LocalDate.now());
        assertThat(segment2_1.getValue().søknadGjelder()).isTrue();
        assertThat(segment2_1.getValue().getSøknad().fraværPerDag()).isEqualTo(fraværVirksomhet2);
        assertThat(segment2_1.getValue().samtidigKravStatus()).isEqualTo(kravStatusForSøknadFinnes());

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


    private LocalDateTime defaultInnsendingstidspunkt = LocalDateTime.now();

    private Map<AktivitetTypeArbeidsgiver, LocalDateTimeline<OppgittFraværHolder>> fraværRefusjonskrav(Arbeidsgiver virksomhet, Map<LocalDateInterval, Duration> perioderMedFravær) {
        FraværÅrsak fraværÅrsak = FraværÅrsak.UDEFINERT;
        SøknadÅrsak søknadÅrsak = SøknadÅrsak.UDEFINERT;
        Utfall søknadsfristUtfall = Utfall.OPPFYLT;
        return Map.of(
            new AktivitetTypeArbeidsgiver(UttakArbeidType.ARBEIDSTAKER, virksomhet),
            new LocalDateTimeline<>(perioderMedFravær.entrySet().stream()
                .map(e -> new LocalDateSegment<>(e.getKey(), OppgittFraværHolder.fraRefusjonskrav(InternArbeidsforholdRef.nullRef(), new OppgittFraværVerdi(defaultInnsendingstidspunkt, e.getValue(), fraværÅrsak, søknadÅrsak, søknadsfristUtfall)))).toList()));
    }

    private Map<AktivitetTypeArbeidsgiver, LocalDateTimeline<OppgittFraværHolder>> fraværEnSøknad(UttakArbeidType aktivitetType, Arbeidsgiver arbeidsgiver, LocalDate fom, LocalDate tom, Duration fraværPrDag) {
        FraværÅrsak fraværÅrsak = FraværÅrsak.ORDINÆRT_FRAVÆR;
        SøknadÅrsak søknadÅrsak = aktivitetType == UttakArbeidType.ARBEIDSTAKER ? SøknadÅrsak.NYOPPSTARTET_HOS_ARBEIDSGIVER : SøknadÅrsak.UDEFINERT;
        Utfall søknadsfristUtfall = Utfall.OPPFYLT;
        return Map.of(
            new AktivitetTypeArbeidsgiver(aktivitetType, arbeidsgiver),
            new LocalDateTimeline<>(fom, tom, OppgittFraværHolder.fraSøknad(new OppgittFraværVerdi(defaultInnsendingstidspunkt, fraværPrDag, fraværÅrsak, søknadÅrsak, søknadsfristUtfall))));
    }

    private Map<AktivitetTypeArbeidsgiver, LocalDateTimeline<OppgittFraværHolder>> fraværRefusjonskravHeleDager(Arbeidsgiver arbeidsgiver, List<LocalDateInterval> perioder) {
        FraværÅrsak fraværÅrsak = FraværÅrsak.UDEFINERT;
        SøknadÅrsak søknadÅrsak = SøknadÅrsak.UDEFINERT;
        Utfall søknadsfristUtfall = Utfall.OPPFYLT;
        return Map.of(
            new AktivitetTypeArbeidsgiver(UttakArbeidType.ARBEIDSTAKER, arbeidsgiver),
            new LocalDateTimeline<>(perioder.stream().map(p -> new LocalDateSegment<>(p, OppgittFraværHolder.fraRefusjonskrav(InternArbeidsforholdRef.nullRef(), new OppgittFraværVerdi(defaultInnsendingstidspunkt, null, fraværÅrsak, søknadÅrsak, søknadsfristUtfall)))).toList()));
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

    private static SamtidigKravStatus kravStatusForSøknadFinnes() {
        return new SamtidigKravStatus(SamtidigKravStatus.KravStatus.FINNES, SamtidigKravStatus.KravStatus.FINNES_IKKE, SamtidigKravStatus.KravStatus.FINNES_IKKE, Map.of());
    }

    private static SamtidigKravStatus kravStatusForRefusjonskravOgSøknadFinnes(InternArbeidsforholdRef arbeidsforhold1, InternArbeidsforholdRef... evtFlereArbeidsforhold) {
        return kravStatusForSøknadOgRefusjonskrav(SamtidigKravStatus.KravStatus.FINNES, SamtidigKravStatus.KravStatus.FINNES, arbeidsforhold1, evtFlereArbeidsforhold);
    }

    private static SamtidigKravStatus kravStatusForRefusjonskravFinnes(InternArbeidsforholdRef arbeidsforhold1, InternArbeidsforholdRef... evtFlereArbeidsforhold) {
        return kravStatusForSøknadOgRefusjonskrav(SamtidigKravStatus.KravStatus.FINNES_IKKE, SamtidigKravStatus.KravStatus.FINNES, arbeidsforhold1, evtFlereArbeidsforhold);
    }

    private static SamtidigKravStatus kravStatusForSøknadOgRefusjonskrav(SamtidigKravStatus.KravStatus søknadStatus, SamtidigKravStatus.KravStatus refusjonskraStatus, InternArbeidsforholdRef arbeidsforhold1, InternArbeidsforholdRef... evtFlereArbeidsforhold) {
        List<InternArbeidsforholdRef> arbeidsforhold = new ArrayList<>();
        arbeidsforhold.add(arbeidsforhold1);
        arbeidsforhold.addAll(Arrays.asList(evtFlereArbeidsforhold));
        var statusPrArbeidsforhold = arbeidsforhold.stream().collect(Collectors.toMap(a -> a, a -> refusjonskraStatus));
        return new SamtidigKravStatus(søknadStatus, refusjonskraStatus, SamtidigKravStatus.KravStatus.FINNES_IKKE, statusPrArbeidsforhold);
    }

    private static SamtidigKravStatus kravStatusForBeggeImTyperTrekt(InternArbeidsforholdRef arbeidsforhold1, InternArbeidsforholdRef... evtFlereArbeidsforhold) {
        List<InternArbeidsforholdRef> arbeidsforhold = new ArrayList<>();
        arbeidsforhold.add(arbeidsforhold1);
        arbeidsforhold.addAll(Arrays.asList(evtFlereArbeidsforhold));
        var statusPrArbeidsforhold = arbeidsforhold.stream().collect(Collectors.toMap(a -> a, a -> SamtidigKravStatus.KravStatus.TREKT));
        return new SamtidigKravStatus(SamtidigKravStatus.KravStatus.FINNES_IKKE, SamtidigKravStatus.KravStatus.TREKT, SamtidigKravStatus.KravStatus.TREKT, statusPrArbeidsforhold);
    }


}
