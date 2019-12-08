package no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.fordel;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.fordel.OmfordelFraFrilans;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Arbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPrStatus;

public class OmfordelFraFrilansTest {

    private static final LocalDate STP = LocalDate.now();
    private static final String ORGNR = "995428563";

    @Test
    public void skal_ikkje_flytte_om_det_ikkje_finnes_frilans() {
        // Arrange
        BigDecimal refusjonskravPrÅr = BigDecimal.valueOf(200_000);
        BigDecimal beregnetPrÅr = BigDecimal.valueOf(100_000);
        BeregningsgrunnlagPrArbeidsforhold arbeidsforhold = lagArbeidsforhold(refusjonskravPrÅr, beregnetPrÅr);
        BeregningsgrunnlagPrStatus atfl = lagATFL(arbeidsforhold);

        BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPrStatus(atfl)
            .medPeriode(Periode.of(STP, null))
            .build();

        // Act
        kjørRegel(arbeidsforhold, periode);

        // Assert
        assertThat(arbeidsforhold.getFordeltPrÅr()).isNull();
        assertThat(arbeidsforhold.getBruttoInkludertNaturalytelsePrÅr().get()).isEqualByComparingTo(beregnetPrÅr);
    }

    @Test
    public void skal_flytte_fra_frilans_til_arbeid_frilans_avkortet_til_0() {
        // Arrange
        BigDecimal refusjonskravPrÅr = BigDecimal.valueOf(200_000);
        BigDecimal beregnetPrÅr = BigDecimal.valueOf(100_000);
        BeregningsgrunnlagPrArbeidsforhold arbeidsforhold = lagArbeidsforhold(refusjonskravPrÅr, beregnetPrÅr);
        BigDecimal beregnetPrÅrFL = BigDecimal.valueOf(100_000);
        BeregningsgrunnlagPrArbeidsforhold frilans = lagFLArbeidsforhold(beregnetPrÅrFL);
        BeregningsgrunnlagPrStatus atfl = lagATFLMedFrilans(arbeidsforhold, frilans);

        BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPrStatus(atfl)
            .medPeriode(Periode.of(STP, null))
            .build();

        // Act
        kjørRegel(arbeidsforhold, periode);

        // Assert
        assertThat(arbeidsforhold.getFordeltPrÅr()).isEqualByComparingTo(refusjonskravPrÅr);
        assertThat(frilans.getFordeltPrÅr()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    public void skal_flytte_fra_frilans_til_arbeid_med_restbeløp_å_flytte() {
        // Arrange
        BigDecimal refusjonskravPrÅr = BigDecimal.valueOf(200_000);
        BigDecimal beregnetPrÅr = BigDecimal.valueOf(100_000);
        BeregningsgrunnlagPrArbeidsforhold arbeidsforhold = lagArbeidsforhold(refusjonskravPrÅr, beregnetPrÅr);
        BigDecimal beregnetPrÅrFL = BigDecimal.valueOf(50_000);
        BeregningsgrunnlagPrArbeidsforhold frilans = lagFLArbeidsforhold(beregnetPrÅrFL);
        BeregningsgrunnlagPrStatus atfl = lagATFLMedFrilans(arbeidsforhold, frilans);

        BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPrStatus(atfl)
            .medPeriode(Periode.of(STP, null))
            .build();

        // Act
        kjørRegel(arbeidsforhold, periode);

        // Assert
        assertThat(arbeidsforhold.getFordeltPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(150_000));
        assertThat(frilans.getFordeltPrÅr()).isEqualByComparingTo(BigDecimal.ZERO);
    }


    @Test
    public void skal_flytte_fra_frilans_til_arbeid_med_restbeløp_på_frilans() {
        // Arrange
        BigDecimal refusjonskravPrÅr = BigDecimal.valueOf(200_000);
        BigDecimal beregnetPrÅr = BigDecimal.valueOf(100_000);
        BeregningsgrunnlagPrArbeidsforhold arbeidsforhold = lagArbeidsforhold(refusjonskravPrÅr, beregnetPrÅr);
        BigDecimal beregnetPrÅrFL = BigDecimal.valueOf(150_000);
        BeregningsgrunnlagPrArbeidsforhold frilans = lagFLArbeidsforhold(beregnetPrÅrFL);
        BeregningsgrunnlagPrStatus atfl = lagATFLMedFrilans(arbeidsforhold, frilans);

        BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPrStatus(atfl)
            .medPeriode(Periode.of(STP, null))
            .build();

        // Act
        kjørRegel(arbeidsforhold, periode);

        // Assert
        assertThat(arbeidsforhold.getFordeltPrÅr()).isEqualByComparingTo(refusjonskravPrÅr);
        assertThat(frilans.getFordeltPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(50_000));
    }

    private BeregningsgrunnlagPrArbeidsforhold lagArbeidsforhold(BigDecimal refusjonskravPrÅr, BigDecimal beregnetPrÅr) {
        return BeregningsgrunnlagPrArbeidsforhold.builder()
            .medAndelNr(1L)
            .medArbeidsforhold(Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(ORGNR, null))
            .medRefusjonskravPrÅr(refusjonskravPrÅr)
            .medBeregnetPrÅr(beregnetPrÅr)
            .build();
    }

    private BeregningsgrunnlagPrArbeidsforhold lagFLArbeidsforhold(BigDecimal beregnetPrÅr) {
        return BeregningsgrunnlagPrArbeidsforhold.builder()
            .medAndelNr(2L)
            .medArbeidsforhold(Arbeidsforhold.frilansArbeidsforhold())
            .medBeregnetPrÅr(beregnetPrÅr)
            .build();
    }

    private BeregningsgrunnlagPrStatus lagATFL(BeregningsgrunnlagPrArbeidsforhold arbeidsforhold) {
        return BeregningsgrunnlagPrStatus.builder()
            .medAktivitetStatus(AktivitetStatus.ATFL)
            .medArbeidsforhold(arbeidsforhold).build();
    }

    private BeregningsgrunnlagPrStatus lagATFLMedFrilans(BeregningsgrunnlagPrArbeidsforhold arbeidsforhold, BeregningsgrunnlagPrArbeidsforhold frilans) {
        return BeregningsgrunnlagPrStatus.builder()
            .medAktivitetStatus(AktivitetStatus.ATFL)
            .medArbeidsforhold(arbeidsforhold)
            .medArbeidsforhold(frilans)
            .build();
    }

    private void kjørRegel(BeregningsgrunnlagPrArbeidsforhold arbeidsforhold, BeregningsgrunnlagPeriode periode) {
        OmfordelFraFrilans regel = new OmfordelFraFrilans(arbeidsforhold);
        regel.evaluate(periode);
    }

}
