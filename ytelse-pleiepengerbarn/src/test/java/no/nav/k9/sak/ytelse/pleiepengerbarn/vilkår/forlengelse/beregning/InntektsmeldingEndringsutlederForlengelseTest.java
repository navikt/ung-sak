package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;

class InntektsmeldingEndringsutlederForlengelseTest {

    @Test
    void skal_vurdere_set_som_like_hvis_inneholder_de_samme() {
        var imSetForrige = Set.of(new JournalpostId("1"), new JournalpostId("12"), new JournalpostId("123"), new JournalpostId("1234"));
        var imSetNå = Set.of(new JournalpostId("1"), new JournalpostId("12"), new JournalpostId("123"), new JournalpostId("1234"));

        var resultat = InntektsmeldingEndringsutlederForlengelse.harUlikeJournalposter(imSetForrige, imSetNå);

        assertThat(resultat).isFalse();
    }

    @Test
    void skal_vurdere_set_som_like_hvis_inneholder_de_samme_i_forskjellig_rekkefølge() {
        var imSetForrige = Set.of(new JournalpostId("1"), new JournalpostId("12"), new JournalpostId("123"), new JournalpostId("1234"));
        var imSetNå = Set.of(new JournalpostId("123"), new JournalpostId("1234"), new JournalpostId("1"), new JournalpostId("12"));

        var resultat = InntektsmeldingEndringsutlederForlengelse.harUlikeJournalposter(imSetForrige, imSetNå);

        assertThat(resultat).isFalse();
    }

    @Test
    void skal_vurdere_set_som_ulike_hvis_forskjellig_størrelse() {
        var imSetForrige = Set.of(new JournalpostId("1"));
        var imSetNå = Set.of(new JournalpostId("1"), new JournalpostId("12"));

        var resultat = InntektsmeldingEndringsutlederForlengelse.harUlikeJournalposter(imSetForrige, imSetNå);

        assertThat(resultat).isTrue();
    }

    @Test
    void skal_vurdere_set_som_ulike_hvis_lik_størrelse_forskjellig_innhold() {
        var imSetForrige = Set.of(new JournalpostId("1"));
        var imSetNå = Set.of(new JournalpostId("12"));

        var resultat = InntektsmeldingEndringsutlederForlengelse.harUlikeJournalposter(imSetForrige, imSetNå);

        assertThat(resultat).isTrue();
    }



    @Test
    void skal_gi_ingen_endring_dersom_samme_inntektsmelding_brukes() {

        var im = InntektsmeldingBuilder.builder()
            .medJournalpostId("1")
            .medKanalreferanse("kanalreferanser")
            .medBeløp(BigDecimal.TEN)
            .build();

        var resultat = new InntektsmeldingEndringsutlederForlengelse(true).erEndret(List.of(im), List.of(im));

        assertThat(resultat).isFalse();
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

        var resultat = new InntektsmeldingEndringsutlederForlengelse(true).erEndret(List.of(im), List.of(im2));

        assertThat(resultat).isFalse();
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

        var resultat = new InntektsmeldingEndringsutlederForlengelse(true).erEndret(List.of(im), List.of(im2));

        assertThat(resultat).isTrue();
    }


    @Test
    void skal_gi_endring_dersom_ulike_journalposter_og_like_beløp_men_ulik_arbeidsforholdID() {

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
            .medArbeidsforholdId(InternArbeidsforholdRef.ref(UUID.randomUUID()))
            .medStartDatoPermisjon(LocalDate.now())
            .medKanalreferanse("kanalreferanser")
            .medBeløp(BigDecimal.TEN)
            .build();

        var resultat = new InntektsmeldingEndringsutlederForlengelse(true).erEndret(List.of(im), List.of(im2));

        assertThat(resultat).isTrue();
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

        var resultat = new InntektsmeldingEndringsutlederForlengelse(true).erEndret(List.of(im), List.of(im2));

        assertThat(resultat).isFalse();
    }

}
