package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.domene.iay.modell.Refusjon;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Beløp;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.ytelse.beregning.grunnlag.InputAktivitetOverstyring;

class FinnInntektsmeldingForBeregningTest {

    @Test
    void skal_mappe_overstyrt_uten_mottatt_fra_arbeidsgiver() {
        // Arrange
        var stp = LocalDate.now();
        var virksomhet = Arbeidsgiver.virksomhet("123456789");
        var inntektPrÅr = new Beløp(BigDecimal.valueOf(12));
        InputAktivitetOverstyring aktivitet = new InputAktivitetOverstyring(virksomhet, inntektPrÅr,
            new Beløp(BigDecimal.valueOf(12000)), null, AktivitetStatus.ARBEIDSTAKER, null);

        // Act
        var mappetInntektsmelding = FinnInntektsmeldingForBeregning.mapAktivitetTilInntektsmelding(aktivitet, stp, Set.of());

        // Assert
        assertThat(mappetInntektsmelding.getArbeidsgiver()).isEqualTo(virksomhet);
        assertThat(mappetInntektsmelding.getInntektBeløp()).isEqualTo(new Beløp(1));
        assertThat(mappetInntektsmelding.getRefusjonBeløpPerMnd()).isEqualTo(new Beløp(1000));
    }

    @Test
    void skal_mappe_overstyrt_med_mottatt_fra_arbeidsgiver() {
        // Arrange
        var stp = LocalDate.now();
        var virksomhet = Arbeidsgiver.virksomhet("123456789");
        var inntektPrÅr = new Beløp(BigDecimal.valueOf(12));
        var inntektsmelding = lagInntektsmelding(stp, virksomhet, InternArbeidsforholdRef.nullRef(), BigDecimal.ZERO, BigDecimal.valueOf(1), stp.plusDays(10), List.of());
        InputAktivitetOverstyring aktivitet = new InputAktivitetOverstyring(virksomhet, inntektPrÅr,
            new Beløp(BigDecimal.valueOf(12000)), null, AktivitetStatus.ARBEIDSTAKER, null);

        // Act
        var mappetInntektsmelding = FinnInntektsmeldingForBeregning.mapAktivitetTilInntektsmelding(aktivitet, stp, Set.of(inntektsmelding));

        // Assert
        assertThat(mappetInntektsmelding.getArbeidsgiver()).isEqualTo(virksomhet);
        assertThat(mappetInntektsmelding.getInntektBeløp()).isEqualTo(new Beløp(1));
        assertThat(mappetInntektsmelding.getRefusjonBeløpPerMnd()).isEqualTo(new Beløp(1));
        assertThat(mappetInntektsmelding.getRefusjonOpphører()).isEqualTo(stp.plusDays(10));
    }

    @Test
    void skal_mappe_overstyrt_med_mottatt_fra_arbeidsgiver_for_flere_arbeidsforhold() {
        // Arrange
        var stp = LocalDate.now();
        var virksomhet = Arbeidsgiver.virksomhet("123456789");
        var inntektPrÅr = new Beløp(BigDecimal.valueOf(12));
        var inntektsmelding1 = lagInntektsmelding(stp, virksomhet, InternArbeidsforholdRef.nyRef(), BigDecimal.ZERO, BigDecimal.valueOf(1), null, List.of());
        var inntektsmelding2 = lagInntektsmelding(stp, virksomhet, InternArbeidsforholdRef.nyRef(), BigDecimal.ZERO, BigDecimal.valueOf(1), null, List.of());
        InputAktivitetOverstyring aktivitet = new InputAktivitetOverstyring(virksomhet, inntektPrÅr,
            new Beløp(BigDecimal.valueOf(12000)), null, AktivitetStatus.ARBEIDSTAKER, null);

        // Act
        var mappetInntektsmelding = FinnInntektsmeldingForBeregning.mapAktivitetTilInntektsmelding(aktivitet, stp, Set.of(inntektsmelding1, inntektsmelding2));

        // Assert
        assertThat(mappetInntektsmelding.getArbeidsgiver()).isEqualTo(virksomhet);
        assertThat(mappetInntektsmelding.getInntektBeløp()).isEqualTo(new Beløp(1));
        assertThat(mappetInntektsmelding.getRefusjonBeløpPerMnd()).isEqualTo(new Beløp(2));
    }

    @Test
    void skal_mappe_overstyrt_med_mottatt_fra_arbeidsgiver_for_flere_arbeidsforhold_med_endringer() {
        // Arrange
        var stp = LocalDate.now();
        var virksomhet = Arbeidsgiver.virksomhet("123456789");
        var inntektPrÅr = new Beløp(BigDecimal.valueOf(12));
        List<Refusjon> endringer1 = List.of(new Refusjon(BigDecimal.valueOf(2), stp.plusDays(1)), new Refusjon(BigDecimal.valueOf(3), stp.plusDays(2)), new Refusjon(BigDecimal.valueOf(1), stp.plusDays(4)));
        var inntektsmelding1 = lagInntektsmelding(stp, virksomhet, InternArbeidsforholdRef.nyRef(), BigDecimal.ZERO, BigDecimal.valueOf(1), null, endringer1);
        List<Refusjon> endringer2 = List.of(new Refusjon(BigDecimal.valueOf(2), stp.plusDays(3)), new Refusjon(BigDecimal.valueOf(1), stp.plusDays(4)));
        var inntektsmelding2 = lagInntektsmelding(stp, virksomhet, InternArbeidsforholdRef.nyRef(), BigDecimal.ZERO, BigDecimal.valueOf(1), null, endringer2);
        InputAktivitetOverstyring aktivitet = new InputAktivitetOverstyring(virksomhet, inntektPrÅr,
            new Beløp(BigDecimal.valueOf(12000)), null, AktivitetStatus.ARBEIDSTAKER, null);

        // Act
        var mappetInntektsmelding = FinnInntektsmeldingForBeregning.mapAktivitetTilInntektsmelding(aktivitet, stp, Set.of(inntektsmelding1, inntektsmelding2));

        // Assert
        assertThat(mappetInntektsmelding.getArbeidsgiver()).isEqualTo(virksomhet);
        assertThat(mappetInntektsmelding.getInntektBeløp()).isEqualTo(new Beløp(1));
        assertThat(mappetInntektsmelding.getRefusjonBeløpPerMnd()).isEqualTo(new Beløp(2));
        assertThat(mappetInntektsmelding.getEndringerRefusjon().size()).isEqualTo(4);
        var e1 = mappetInntektsmelding.getEndringerRefusjon().get(0);
        assertThat(e1.getRefusjonsbeløp().getVerdi().intValue()).isEqualTo(3);
        assertThat(e1.getFom()).isEqualTo(stp.plusDays(1));

        var e2 = mappetInntektsmelding.getEndringerRefusjon().get(1);
        assertThat(e2.getRefusjonsbeløp().getVerdi().intValue()).isEqualTo(4);
        assertThat(e2.getFom()).isEqualTo(stp.plusDays(2));

        var e3 = mappetInntektsmelding.getEndringerRefusjon().get(2);
        assertThat(e3.getRefusjonsbeløp().getVerdi().intValue()).isEqualTo(5);
        assertThat(e3.getFom()).isEqualTo(stp.plusDays(3));

        var e4 = mappetInntektsmelding.getEndringerRefusjon().get(3);
        assertThat(e4.getRefusjonsbeløp().getVerdi().intValue()).isEqualTo(2);
        assertThat(e4.getFom()).isEqualTo(stp.plusDays(4));
    }

    private static Inntektsmelding lagInntektsmelding(LocalDate stp, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdId, BigDecimal inntekt, BigDecimal refusjonVedStp, LocalDate opphørRefusjon, List<Refusjon> endringer) {
        var inntektsmeldingBuilder = InntektsmeldingBuilder.builder()
            .medInnsendingstidspunkt(stp.atStartOfDay())
            .medArbeidsgiver(arbeidsgiver)
            .medStartDatoPermisjon(stp)
            .medRefusjon(refusjonVedStp, opphørRefusjon)
            .medBeløp(inntekt)
            .medArbeidsforholdId(arbeidsforholdId)
            .medJournalpostId("journalpostid")
            .medKanalreferanse("kanalreferanse");
        endringer.forEach(inntektsmeldingBuilder::leggTil);
        return inntektsmeldingBuilder
            .build();
    }

}
