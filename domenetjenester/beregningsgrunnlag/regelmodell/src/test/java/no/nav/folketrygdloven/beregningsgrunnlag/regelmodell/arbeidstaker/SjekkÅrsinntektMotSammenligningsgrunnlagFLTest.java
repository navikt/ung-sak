package no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.arbeidstaker;

import static no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.BeregningsgrunnlagScenario.settoppGrunnlagMedEnPeriode;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Arbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Inntektsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.SammenligningsGrunnlag;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.Resultat;

public class SjekkÅrsinntektMotSammenligningsgrunnlagFLTest {
    private Arbeidsforhold arbeidsforhold = Arbeidsforhold.frilansArbeidsforhold();

    @Test
    public void skalReturnereJaNårSammenligningsGrunnlagEr0() {
        //Arrange
        Beregningsgrunnlag grunnlag = settoppGrunnlagMedEnPeriode(LocalDate.now(), new Inntektsgrunnlag(),
            Collections.singletonList(AktivitetStatus.ATFL), Collections.singletonList(arbeidsforhold));
        SammenligningsGrunnlag sg = SammenligningsGrunnlag.builder()
            .medSammenligningsperiode(null)
            .medRapportertPrÅr(BigDecimal.ZERO).build();
        Beregningsgrunnlag.builder(grunnlag).medSammenligningsgrunnlagPrStatus(AktivitetStatus.FL, sg);
        BeregningsgrunnlagPeriode periode = grunnlag.getBeregningsgrunnlagPerioder().get(0);
        //Act
        Evaluation resultat = new SjekkÅrsinntektMotSammenligningsgrunnlagFL().evaluate(periode);
        //Assert
        assertThat(resultat.result()).isEqualTo(Resultat.JA);
        assertThat(grunnlag.getSammenligningsGrunnlagPrAktivitetstatus().get(AktivitetStatus.FL).getAvvikPromille()).isEqualTo(1000L);
    }

    @Test(expected = IllegalStateException.class)
    public void skalKasteExceptionNårSammenligningsgrunnlagErNull() {
        //Arrange
        Beregningsgrunnlag grunnlag = settoppGrunnlagMedEnPeriode(LocalDate.now(), new Inntektsgrunnlag(),
            Collections.singletonList(AktivitetStatus.ATFL), Collections.singletonList(arbeidsforhold));
        BeregningsgrunnlagPeriode periode = grunnlag.getBeregningsgrunnlagPerioder().get(0);
        //Act
        new SjekkÅrsinntektMotSammenligningsgrunnlagFL().evaluate(periode);
    }

    @Test
    public void skalReturnereNeiNårAvvikErAkkurat25Prosent() {
        //Arrange
        Beregningsgrunnlag grunnlag = settoppGrunnlagMedEnPeriode(LocalDate.now(), new Inntektsgrunnlag(),
            Collections.singletonList(AktivitetStatus.ATFL), Collections.singletonList(arbeidsforhold));
        BeregningsgrunnlagPeriode periode = grunnlag.getBeregningsgrunnlagPerioder().get(0);
        SammenligningsGrunnlag sg = SammenligningsGrunnlag.builder()
            .medSammenligningsperiode(null)
            .medRapportertPrÅr(BigDecimal.valueOf(100000)).build();
        Beregningsgrunnlag.builder(grunnlag).medSammenligningsgrunnlagPrStatus(AktivitetStatus.FL, sg);
        BeregningsgrunnlagPrArbeidsforhold bgAT = periode.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL).getArbeidsforhold().get(0);
        BeregningsgrunnlagPrArbeidsforhold.builder(bgAT).medBeregnetPrÅr(BigDecimal.valueOf(125000));

        //Act
        Evaluation resultat = new SjekkÅrsinntektMotSammenligningsgrunnlagFL().evaluate(periode);
        //Assert
        assertThat(resultat.result()).isEqualTo(Resultat.NEI);
        assertThat(grunnlag.getSammenligningsGrunnlagPrAktivitetstatus().get(AktivitetStatus.FL).getAvvikPromille()).isEqualTo(250);
        assertThat(grunnlag.getSammenligningsGrunnlagPrAktivitetstatus().get(AktivitetStatus.FL).getAvvikProsent()).isEqualByComparingTo(BigDecimal.valueOf(25));
    }

    @Test
    public void skalReturnereJaNårAvvikErAkkuratOver25Prosent() {
        //Arrange
        Beregningsgrunnlag grunnlag = settoppGrunnlagMedEnPeriode(LocalDate.now(), new Inntektsgrunnlag(),
            Collections.singletonList(AktivitetStatus.ATFL), Collections.singletonList(arbeidsforhold));
        BeregningsgrunnlagPeriode periode = grunnlag.getBeregningsgrunnlagPerioder().get(0);
        SammenligningsGrunnlag sg = SammenligningsGrunnlag.builder()
            .medSammenligningsperiode(null)
            .medRapportertPrÅr(BigDecimal.valueOf(100000)).build();
        Beregningsgrunnlag.builder(grunnlag).medSammenligningsgrunnlagPrStatus(AktivitetStatus.FL, sg);
        BeregningsgrunnlagPrArbeidsforhold bgAT = periode.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL).getArbeidsforhold().get(0);
        BeregningsgrunnlagPrArbeidsforhold.builder(bgAT).medBeregnetPrÅr(BigDecimal.valueOf(125001));

        //Act
        Evaluation resultat = new SjekkÅrsinntektMotSammenligningsgrunnlagFL().evaluate(periode);
        //Assert
        assertThat(resultat.result()).isEqualTo(Resultat.JA);
        assertThat(grunnlag.getSammenligningsGrunnlagPrAktivitetstatus().get(AktivitetStatus.FL).getAvvikPromille()).isEqualTo(250);
        assertThat(grunnlag.getSammenligningsGrunnlagPrAktivitetstatus().get(AktivitetStatus.FL).getAvvikProsent()).isEqualByComparingTo(BigDecimal.valueOf(25.001));
    }



}
