package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.arbeidsforhold.NaturalYtelseType;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.domene.iay.modell.NaturalYtelse;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

class HarRelevantInntektsmeldingendringForForlengelseIBeregningTest {


    @Test
    void skal_gi_ingen_endring_dersom_samme_inntektsmelding_brukes() {

        var im = InntektsmeldingBuilder.builder()
            .medJournalpostId("1")
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123456789"))
            .medKanalreferanse("kanalreferanser")
            .medBeløp(BigDecimal.TEN)
            .build();

        var resultat = new HarRelevantInntektsmeldingendringForForlengelseIBeregning().finnInntektsmeldingerMedRelevanteEndringer(List.of(im), List.of(im));

        assertThat(resultat.isEmpty()).isTrue();
    }

    @Test
    void skal_gi_ingen_endring_dersom_ulike_journalposter_men_like_beløp_og_lik_startdato() {

        var im = InntektsmeldingBuilder.builder()
            .medJournalpostId("1")
            .medStartDatoPermisjon(LocalDate.now())
            .medKanalreferanse("kanalreferanser")
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123456789"))
            .medBeløp(BigDecimal.TEN)
            .build();

        var im2 = InntektsmeldingBuilder.builder()
            .medJournalpostId("2")
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123456789"))
            .medStartDatoPermisjon(LocalDate.now())
            .medKanalreferanse("kanalreferanser")
            .medBeløp(BigDecimal.TEN)
            .build();

        var resultat = new HarRelevantInntektsmeldingendringForForlengelseIBeregning().finnInntektsmeldingerMedRelevanteEndringer(List.of(im), List.of(im2));

        assertThat(resultat.isEmpty()).isTrue();
    }



    @Test
    void skal_gi_endring_dersom_ulike_journalposter_og_like_beløp_men_ulik_arbeidsgiver() {

        var im = InntektsmeldingBuilder.builder()
            .medJournalpostId("1")
            .medStartDatoPermisjon(LocalDate.now())
            .medKanalreferanse("kanalreferanser")
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123456789"))
            .medBeløp(BigDecimal.TEN)
            .build();

        var im2 = InntektsmeldingBuilder.builder()
            .medJournalpostId("2")
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123456788"))
            .medStartDatoPermisjon(LocalDate.now())
            .medKanalreferanse("kanalreferanser")
            .medBeløp(BigDecimal.TEN)
            .build();

        var resultat = new HarRelevantInntektsmeldingendringForForlengelseIBeregning().finnInntektsmeldingerMedRelevanteEndringer(List.of(im), List.of(im2));

        assertThat(resultat.isEmpty()).isFalse();
    }


    @Test
    void skal_gi_endring_dersom_ulike_journalposter_og_like_beløp_men_ulik_arbeidsforholdID() {

        var im = InntektsmeldingBuilder.builder()
            .medJournalpostId("1")
            .medStartDatoPermisjon(LocalDate.now())
            .medKanalreferanse("kanalreferanser")
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123456789"))
            .medBeløp(BigDecimal.TEN)
            .build();

        var im2 = InntektsmeldingBuilder.builder()
            .medJournalpostId("2")
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123456789"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
            .medStartDatoPermisjon(LocalDate.now())
            .medKanalreferanse("kanalreferanser")
            .medBeløp(BigDecimal.TEN)
            .build();

        var resultat = new HarRelevantInntektsmeldingendringForForlengelseIBeregning().finnInntektsmeldingerMedRelevanteEndringer(List.of(im), List.of(im2));

        assertThat(resultat.isEmpty()).isFalse();
    }


    @Test
    void skal_gi_ingen_endring_dersom_ulike_journalposter_og_like_beløp_lik_arbeidsgiver_og_ulik_stardato() {
        var ref = InternArbeidsforholdRef.ref(UUID.randomUUID());

        var im = InntektsmeldingBuilder.builder()
            .medJournalpostId("1")
            .medStartDatoPermisjon(LocalDate.now())
            .medKanalreferanse("kanalreferanser")
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123456789"))
            .medArbeidsforholdId(ref)
            .medBeløp(BigDecimal.TEN)
            .build();

        var im2 = InntektsmeldingBuilder.builder()
            .medJournalpostId("2")
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123456789"))
            .medArbeidsforholdId(ref)
            .medStartDatoPermisjon(LocalDate.now().plusDays(1))
            .medKanalreferanse("kanalreferanser")
            .medBeløp(BigDecimal.TEN)
            .build();

        var resultat = new HarRelevantInntektsmeldingendringForForlengelseIBeregning().finnInntektsmeldingerMedRelevanteEndringer(List.of(im), List.of(im2));

        assertThat(resultat.isEmpty()).isTrue();
    }

    @Test
    void skal_gi_endring_dersom_ulike_journalposter_og_like_beløp_lik_arbeidsgiver_og_ulik_stardato_og_ulike_naturalytelser() {
        var ref = InternArbeidsforholdRef.ref(UUID.randomUUID());

        var im = InntektsmeldingBuilder.builder()
            .medJournalpostId("1")
            .medStartDatoPermisjon(LocalDate.now())
            .medKanalreferanse("kanalreferanser")
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123456789"))
            .medArbeidsforholdId(ref)
            .medBeløp(BigDecimal.TEN)
            .build();

        var im2 = InntektsmeldingBuilder.builder()
            .medJournalpostId("2")
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123456789"))
            .medArbeidsforholdId(ref)
            .medStartDatoPermisjon(LocalDate.now().plusDays(1))
            .medKanalreferanse("kanalreferanser")
            .leggTil(new NaturalYtelse(LocalDate.now().plusDays(1), LocalDate.now().plusDays(1), BigDecimal.ONE, NaturalYtelseType.BIL))
            .medBeløp(BigDecimal.TEN)
            .build();

        var resultat = new HarRelevantInntektsmeldingendringForForlengelseIBeregning().finnInntektsmeldingerMedRelevanteEndringer(List.of(im), List.of(im2));

        assertThat(resultat.isEmpty()).isFalse();
    }

    @Test
    void skal_gi_ingen_endring_dersom_ulike_journalposter_og_like_beløp_lik_arbeidsgiver_og_ulik_stardato_og_like_naturalytelser() {
        var ref = InternArbeidsforholdRef.ref(UUID.randomUUID());

        var im = InntektsmeldingBuilder.builder()
            .medJournalpostId("1")
            .medStartDatoPermisjon(LocalDate.now())
            .medKanalreferanse("kanalreferanser")
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123456789"))
            .medArbeidsforholdId(ref)
            .leggTil(new NaturalYtelse(LocalDate.now().plusDays(1), LocalDate.now().plusDays(1), BigDecimal.ONE, NaturalYtelseType.BIL))
            .medBeløp(BigDecimal.TEN)
            .build();

        var im2 = InntektsmeldingBuilder.builder()
            .medJournalpostId("2")
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123456789"))
            .medArbeidsforholdId(ref)
            .medStartDatoPermisjon(LocalDate.now().plusDays(1))
            .medKanalreferanse("kanalreferanser")
            .leggTil(new NaturalYtelse(LocalDate.now().plusDays(1), LocalDate.now().plusDays(1), BigDecimal.ONE, NaturalYtelseType.BIL))
            .medBeløp(BigDecimal.TEN)
            .build();

        var resultat = new HarRelevantInntektsmeldingendringForForlengelseIBeregning().finnInntektsmeldingerMedRelevanteEndringer(List.of(im), List.of(im2));

        assertThat(resultat.isEmpty()).isTrue();
    }

}
