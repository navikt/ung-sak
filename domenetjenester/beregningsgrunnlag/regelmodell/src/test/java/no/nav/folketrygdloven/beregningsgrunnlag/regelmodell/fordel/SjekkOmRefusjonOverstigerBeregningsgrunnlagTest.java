package no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.fordel;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.fordel.SjekkOmRefusjonOverstigerBeregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Arbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPrStatus;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.Resultat;

public class SjekkOmRefusjonOverstigerBeregningsgrunnlagTest {

    private static final LocalDate STP = LocalDate.now();
    private static final String ORGNR = "995428563";

    @Test
    public void skal_gi_at_refusjon_overstiger_bg() {
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
        Evaluation evaluering = kjørRegel(arbeidsforhold, periode);

        // Assert
        assertThat(evaluering.result()).isEqualTo(Resultat.JA);
    }

    @Test
    public void skal_gi_at_refusjon_ikkje_overstiger_bg() {
        // Arrange
        BigDecimal refusjonskravPrÅr = BigDecimal.valueOf(200_000);
        BigDecimal beregnetPrÅr = BigDecimal.valueOf(250_000);
        BeregningsgrunnlagPrArbeidsforhold arbeidsforhold = lagArbeidsforhold(refusjonskravPrÅr, beregnetPrÅr);
        BeregningsgrunnlagPrStatus atfl = lagATFL(arbeidsforhold);

        BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPrStatus(atfl)
            .medPeriode(Periode.of(STP, null))
            .build();

        // Act
        Evaluation evaluering = kjørRegel(arbeidsforhold, periode);

        // Assert
        assertThat(evaluering.result()).isEqualTo(Resultat.NEI);
    }

    private BeregningsgrunnlagPrArbeidsforhold lagArbeidsforhold(BigDecimal refusjonskravPrÅr, BigDecimal beregnetPrÅr) {
        return BeregningsgrunnlagPrArbeidsforhold.builder()
            .medAndelNr(1L)
            .medArbeidsforhold(Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(ORGNR, null))
            .medRefusjonskravPrÅr(refusjonskravPrÅr)
            .medBeregnetPrÅr(beregnetPrÅr)
            .build();
    }

    private BeregningsgrunnlagPrStatus lagATFL(BeregningsgrunnlagPrArbeidsforhold arbeidsforhold) {
        return BeregningsgrunnlagPrStatus.builder()
            .medAktivitetStatus(AktivitetStatus.ATFL)
            .medArbeidsforhold(arbeidsforhold).build();
    }

    private Evaluation kjørRegel(BeregningsgrunnlagPrArbeidsforhold arbeidsforhold, BeregningsgrunnlagPeriode periode) {
        SjekkOmRefusjonOverstigerBeregningsgrunnlag regel = new SjekkOmRefusjonOverstigerBeregningsgrunnlag(arbeidsforhold);
        return regel.evaluate(periode);
    }

}
