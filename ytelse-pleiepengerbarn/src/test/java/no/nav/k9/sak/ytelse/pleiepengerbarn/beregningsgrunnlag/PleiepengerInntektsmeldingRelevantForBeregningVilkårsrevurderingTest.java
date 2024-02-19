package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningAktiviteter;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

class PleiepengerInntektsmeldingRelevantForBeregningVilkårsrevurderingTest {

    @Test
    void skal_ikke_filtrere_bort_inntektsmelding_dersom_det_finnes_opptjeningsaktivitet_for_samme_arbeidsgiver() {

        var orgnr1 = "123456789";
        var im1 = InntektsmeldingBuilder.builder()
            .medJournalpostId("1")
            .medStartDatoPermisjon(LocalDate.now())
            .medKanalreferanse("kanalreferanser")
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr1))
            .medBeløp(BigDecimal.TEN)
            .build();

        var im2 = InntektsmeldingBuilder.builder()
            .medJournalpostId("2")
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123456788"))
            .medArbeidsforholdId(InternArbeidsforholdRef.ref(UUID.randomUUID()))
            .medStartDatoPermisjon(LocalDate.now())
            .medKanalreferanse("kanalreferanser")
            .medBeløp(BigDecimal.TEN)
            .build();


        var filtrert = PleiepengerInntektsmeldingRelevantForBeregningVilkårsrevurdering.filtrerForAktiviteter(List.of(im1, im2),
            Optional.of(OpptjeningAktiviteter.fraOrgnr(OpptjeningAktivitetType.ARBEID,
                DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(10), LocalDate.now().plusDays(10)),
                orgnr1,
                InternArbeidsforholdRef.nullRef()
            )));


        assertThat(filtrert.size()).isEqualTo(1);
        assertThat(filtrert.get(0).getArbeidsgiver().getArbeidsgiverOrgnr()).isEqualTo(orgnr1);
    }


    @Test
    void skal_filtrere_bort_arbeidsforhold_dersom_det_ikke_finnes_i_opptjeningsaktiviteter() {

        var orgnr1 = "123456789";
        var ref1 = InternArbeidsforholdRef.nyRef();
        var im1 = InntektsmeldingBuilder.builder()
            .medJournalpostId("1")
            .medStartDatoPermisjon(LocalDate.now())
            .medKanalreferanse("kanalreferanser")
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr1))
            .medArbeidsforholdId(ref1)
            .medBeløp(BigDecimal.TEN)
            .build();

        var im2 = InntektsmeldingBuilder.builder()
            .medJournalpostId("2")
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr1))
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
            .medStartDatoPermisjon(LocalDate.now())
            .medKanalreferanse("kanalreferanser")
            .medBeløp(BigDecimal.TEN)
            .build();


        var filtrert = PleiepengerInntektsmeldingRelevantForBeregningVilkårsrevurdering.filtrerForAktiviteter(List.of(im1, im2),
            Optional.of(OpptjeningAktiviteter.fraOrgnr(OpptjeningAktivitetType.ARBEID,
                DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(10), LocalDate.now().plusDays(10)),
                orgnr1,
                ref1
            )));


        assertThat(filtrert.size()).isEqualTo(1);
        assertThat(filtrert.get(0).getArbeidsgiver().getArbeidsgiverOrgnr()).isEqualTo(orgnr1);
        assertThat(filtrert.get(0).getArbeidsforholdRef().getReferanse()).isEqualTo(ref1.getReferanse());

    }

    @Test
    void skal_ikke_filtrere_bort_arbeidsforhold_uten_id() {

        var orgnr1 = "123456789";
        var ref1 = InternArbeidsforholdRef.nyRef();
        var im1 = InntektsmeldingBuilder.builder()
            .medJournalpostId("1")
            .medStartDatoPermisjon(LocalDate.now())
            .medKanalreferanse("kanalreferanser")
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr1))
            .medArbeidsforholdId(ref1)
            .medBeløp(BigDecimal.TEN)
            .build();

        var im2 = InntektsmeldingBuilder.builder()
            .medJournalpostId("2")
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr1))
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medStartDatoPermisjon(LocalDate.now())
            .medKanalreferanse("kanalreferanser")
            .medBeløp(BigDecimal.TEN)
            .build();


        var filtrert = PleiepengerInntektsmeldingRelevantForBeregningVilkårsrevurdering.filtrerForAktiviteter(List.of(im1, im2),
            Optional.of(OpptjeningAktiviteter.fraOrgnr(OpptjeningAktivitetType.ARBEID,
                DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(10), LocalDate.now().plusDays(10)),
                orgnr1,
                ref1
            )));


        assertThat(filtrert.size()).isEqualTo(2);

    }

}
