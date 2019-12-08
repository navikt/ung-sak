package no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.ytelse.dagpengerelleraap;


import static no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.BeregningsgrunnlagScenario.settoppGrunnlagMedEnPeriode;
import static no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.RegelmodellOversetter.getRegelResultat;
import static no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.VerifiserBeregningsgrunnlag.verifiserBeregningsgrunnlagBruttoPrPeriodeType;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import no.finn.unleash.FakeUnleash;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.foreslå.RegelForeslåBeregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.BeregningsgrunnlagHjemmel;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.RegelResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.ResultatBeregningType;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Arbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Inntektsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Inntektskilde;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Periodeinntekt;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPrStatus;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.summary.EvaluationSerializer;

public class RegelFastsettBeregningsgrunnlagDPellerAAPTest {

    private LocalDate skjæringstidspunkt = LocalDate.of(2018, Month.JANUARY, 15);
    private FakeUnleash unleash = new FakeUnleash();
    @Test
    public void skalForeslåBeregningsgrunnlagForDagpenger() {
        //Arrange
        BigDecimal dagsats = new BigDecimal("1142");
        Inntektsgrunnlag inntektsgrunnlag = lagInntektsgrunnlag(dagsats, skjæringstidspunkt, 150);
        Beregningsgrunnlag beregningsgrunnlag = settoppGrunnlagMedEnPeriode(skjæringstidspunkt, inntektsgrunnlag,
            Collections.singletonList(AktivitetStatus.DP));
        BeregningsgrunnlagPeriode grunnlag = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);

        //Act
        Evaluation evaluation = new RegelFastsettBeregningsgrunnlagDPellerAAP().evaluer(grunnlag);

        //Assert

        RegelResultat regelResultat = getRegelResultat(evaluation, "input");
        assertThat(regelResultat.getBeregningsresultat()).isEqualTo(ResultatBeregningType.BEREGNET);

        Periode periode = new Periode(skjæringstidspunkt, null);
        assertThat(grunnlag.getBeregningsgrunnlagPeriode()).isEqualTo(periode);

        BigDecimal brutto = BigDecimal.valueOf(296920).stripTrailingZeros();
        verifiserBeregningsgrunnlagBruttoPrPeriodeType(grunnlag, BeregningsgrunnlagHjemmel.F_14_7_8_49, AktivitetStatus.DP, brutto.doubleValue());
        BeregningsgrunnlagPrStatus bgps = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.DP);
        assertThat(bgps.getBeregnetPrÅr()).isEqualByComparingTo(brutto);
        assertThat(bgps.getÅrsbeløpFraTilstøtendeYtelse()).isEqualByComparingTo(brutto);
        assertThat(bgps.getOrginalDagsatsFraTilstøtendeYtelse()).isEqualTo(dagsats.longValue());
    }

    @Test
    public void skalForeslåBeregningsgrunnlagForAAP() {
        //Arrange
        BigDecimal dagsats = new BigDecimal("1611");
        Inntektsgrunnlag inntektsgrunnlag = lagInntektsgrunnlag(dagsats, skjæringstidspunkt, 150);
        Beregningsgrunnlag beregningsgrunnlag = settoppGrunnlagMedEnPeriode(skjæringstidspunkt, inntektsgrunnlag, Collections.singletonList(AktivitetStatus.AAP));
        BeregningsgrunnlagPeriode grunnlag = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);

        //Act
        Evaluation evaluation = new RegelFastsettBeregningsgrunnlagDPellerAAP().evaluer(grunnlag);

        //Assert

        RegelResultat regelResultat = getRegelResultat(evaluation, "input");
        assertThat(regelResultat.getBeregningsresultat()).isEqualTo(ResultatBeregningType.BEREGNET);

        Periode periode = new Periode(skjæringstidspunkt, null);
        assertThat(grunnlag.getBeregningsgrunnlagPeriode()).isEqualTo(periode);

        BigDecimal brutto = BigDecimal.valueOf(418860);
        verifiserBeregningsgrunnlagBruttoPrPeriodeType(grunnlag, BeregningsgrunnlagHjemmel.F_14_7, AktivitetStatus.AAP, brutto.doubleValue());
        BeregningsgrunnlagPrStatus bgps = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.AAP);
        assertThat(bgps.getBeregnetPrÅr()).isEqualByComparingTo(brutto);
        assertThat(bgps.getÅrsbeløpFraTilstøtendeYtelse()).isEqualByComparingTo(brutto);
        assertThat(bgps.getOrginalDagsatsFraTilstøtendeYtelse()).isEqualTo(dagsats.longValue());
    }

    @Test
    public void skalForeslåBeregningsgrunnlagForAAPMedManueltFastsattBeløp() {
        //Arrange
        BigDecimal dagsats = new BigDecimal("1611");
        BigDecimal beregnetPrÅr = new BigDecimal(324423);
        Inntektsgrunnlag inntektsgrunnlag = lagInntektsgrunnlag(dagsats, skjæringstidspunkt, 150);
        Beregningsgrunnlag beregningsgrunnlag = settoppGrunnlagMedEnPeriode(skjæringstidspunkt, inntektsgrunnlag, Collections.singletonList(AktivitetStatus.AAP));
        BeregningsgrunnlagPeriode grunnlag = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        BeregningsgrunnlagPrStatus.builder(grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.AAP))
            .medBeregnetPrÅr(beregnetPrÅr)
            .medFastsattAvSaksbehandler(true)
            .build();

        //Act
        Evaluation evaluation = new RegelFastsettBeregningsgrunnlagDPellerAAP().evaluer(grunnlag);

        //Assert

        RegelResultat regelResultat = getRegelResultat(evaluation, "input");
        assertThat(regelResultat.getBeregningsresultat()).isEqualTo(ResultatBeregningType.BEREGNET);

        Periode periode = new Periode(skjæringstidspunkt, null);
        assertThat(grunnlag.getBeregningsgrunnlagPeriode()).isEqualTo(periode);

        verifiserBeregningsgrunnlagBruttoPrPeriodeType(grunnlag, BeregningsgrunnlagHjemmel.F_14_7, AktivitetStatus.AAP, beregnetPrÅr.doubleValue());
        BeregningsgrunnlagPrStatus bgps = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.AAP);
        assertThat(bgps.getBeregnetPrÅr()).isEqualByComparingTo(beregnetPrÅr);
        assertThat(bgps.getÅrsbeløpFraTilstøtendeYtelse()).isEqualByComparingTo(beregnetPrÅr);
        assertThat(bgps.getOrginalDagsatsFraTilstøtendeYtelse()).isEqualTo(dagsats.longValue());
    }


    @Test
    public void skalForeslåBeregningsgrunnlagForAAPMedKombinasjonsStatus() {
        //Arrange
        BigDecimal dagsats = new BigDecimal("1400");
        Inntektsgrunnlag inntektsgrunnlag = lagInntektsgrunnlag(dagsats, skjæringstidspunkt, 150);
        Beregningsgrunnlag beregningsgrunnlag = settoppGrunnlagMedEnPeriode(skjæringstidspunkt, inntektsgrunnlag, List.of(AktivitetStatus.AAP, AktivitetStatus.SN));
        BeregningsgrunnlagPeriode grunnlag = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);

        //Act
        Evaluation evaluation = new RegelFastsettBeregningsgrunnlagDPellerAAP().evaluer(grunnlag);

        //Assert
        @SuppressWarnings("unused")
        String sporing = EvaluationSerializer.asJson(evaluation);

        RegelResultat regelResultat = getRegelResultat(evaluation, "input");
        assertThat(regelResultat.getBeregningsresultat()).isEqualTo(ResultatBeregningType.BEREGNET);

        Periode periode = new Periode(skjæringstidspunkt, null);
        assertThat(grunnlag.getBeregningsgrunnlagPeriode()).isEqualTo(periode);

        BigDecimal brutto = BigDecimal.valueOf(273000);
        verifiserBeregningsgrunnlagBruttoPrPeriodeType(grunnlag, BeregningsgrunnlagHjemmel.F_14_7, AktivitetStatus.AAP, brutto.doubleValue());
        BeregningsgrunnlagPrStatus bgps = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.AAP);
        assertThat(bgps.getBeregnetPrÅr()).isEqualByComparingTo(brutto);
        assertThat(bgps.getÅrsbeløpFraTilstøtendeYtelse()).isEqualByComparingTo(brutto);
        assertThat(bgps.getOrginalDagsatsFraTilstøtendeYtelse()).isEqualTo(dagsats.longValue());
    }

    @Test
    public void skalForeslåBeregningsgrunnlagForDagpengerMedBesteberegningFødendeKvinne() {
        //Arrange
        BigDecimal beregnetDagsats = BigDecimal.valueOf(600);
        BigDecimal brutto = BigDecimal.valueOf(260000);
        Inntektsgrunnlag inntektsgrunnlag = lagInntektsgrunnlag(beregnetDagsats, skjæringstidspunkt, 150);
        Beregningsgrunnlag beregningsgrunnlag = settoppGrunnlagMedEnPeriode(skjæringstidspunkt, inntektsgrunnlag, Collections.singletonList(AktivitetStatus.DP));
        BeregningsgrunnlagPrStatus bgPrStatus = beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream().map(p -> p.getBeregningsgrunnlagPrStatus(AktivitetStatus.DP)).findFirst().get();//NOSONAR
        BeregningsgrunnlagPrStatus.builder(bgPrStatus).medFastsattAvSaksbehandler(true).medBesteberegningPrÅr(brutto).build();
        BeregningsgrunnlagPeriode grunnlag = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);

        //Act
        Evaluation evaluation = new RegelFastsettBeregningsgrunnlagDPellerAAP().evaluer(grunnlag);

        //Assert
        RegelResultat regelResultat = getRegelResultat(evaluation, "input");
        assertThat(regelResultat.getBeregningsresultat()).isEqualTo(ResultatBeregningType.BEREGNET);
        BeregningsgrunnlagPrStatus bgps = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.DP);
        assertThat(bgps.getBruttoPrÅr()).isEqualByComparingTo(brutto);
        assertThat(bgps.getÅrsbeløpFraTilstøtendeYtelse()).isEqualByComparingTo(brutto);
        assertThat(bgps.getOrginalDagsatsFraTilstøtendeYtelse()).isEqualTo(beregnetDagsats.longValue());
        assertThat(grunnlag.getBeregningsgrunnlag().getAktivitetStatus(AktivitetStatus.DP).getHjemmel()).isEqualTo(BeregningsgrunnlagHjemmel.F_14_7);
    }

    @Test
    public void skalForeslåBeregningsgrunnlagForDagpengerIKombinasjonSNOgMedBesteberegningFødendeKvinne() {
        //Arrange
        BigDecimal beregnetDagsats = BigDecimal.valueOf(720);
        BigDecimal brutto = BigDecimal.valueOf(240000);
        Inntektsgrunnlag inntektsgrunnlag = lagInntektsgrunnlag(beregnetDagsats, skjæringstidspunkt, 100);
        Beregningsgrunnlag beregningsgrunnlag = settoppGrunnlagMedEnPeriode(skjæringstidspunkt, inntektsgrunnlag, List.of(AktivitetStatus.DP, AktivitetStatus.SN));
        BeregningsgrunnlagPrStatus bgPrStatus = beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream().map(p -> p.getBeregningsgrunnlagPrStatus(AktivitetStatus.DP)).findFirst().get();//NOSONAR
        BeregningsgrunnlagPrStatus.builder(bgPrStatus).medFastsattAvSaksbehandler(true).medBesteberegningPrÅr(brutto).build();
        BeregningsgrunnlagPeriode grunnlag = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);

        //Act
        Evaluation evaluation = new RegelFastsettBeregningsgrunnlagDPellerAAP().evaluer(grunnlag);

        //Assert
        RegelResultat regelResultat = getRegelResultat(evaluation, "input");
        assertThat(regelResultat.getBeregningsresultat()).isEqualTo(ResultatBeregningType.BEREGNET);
        BeregningsgrunnlagPrStatus bgps = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.DP);
        assertThat(bgps.getBruttoPrÅr()).isEqualByComparingTo(brutto);
        assertThat(bgps.getÅrsbeløpFraTilstøtendeYtelse()).isEqualByComparingTo(brutto);
        assertThat(bgps.getOrginalDagsatsFraTilstøtendeYtelse()).isEqualTo(beregnetDagsats.longValue());
        assertThat(grunnlag.getBeregningsgrunnlag().getAktivitetStatus(AktivitetStatus.DP).getHjemmel()).isEqualTo(BeregningsgrunnlagHjemmel.F_14_7);
    }

    @Test
    public void skalForeslåBeregningsgrunnlagForDagpengerIKombinasjonATOgMedBesteberegningFødendeKvinne() {
        //Arrange
        BigDecimal fastsattPrÅr = BigDecimal.valueOf(120000);
        BigDecimal beregnetDagsats = BigDecimal.valueOf(720);
        BigDecimal besteberegning = BigDecimal.valueOf(240000);
        Inntektsgrunnlag inntektsgrunnlag = lagInntektsgrunnlag(beregnetDagsats, skjæringstidspunkt, 100);
        Beregningsgrunnlag beregningsgrunnlag = settoppGrunnlagMedEnPeriode(skjæringstidspunkt, inntektsgrunnlag,
            List.of(AktivitetStatus.DP, AktivitetStatus.ATFL), Collections.singletonList(Arbeidsforhold.nyttArbeidsforholdHosVirksomhet("12345")));
        BeregningsgrunnlagPeriode grunnlag = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        BeregningsgrunnlagPrStatus dp = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.DP);
        BeregningsgrunnlagPrStatus.builder(dp).medFastsattAvSaksbehandler(true).medBesteberegningPrÅr(besteberegning).build();
        BeregningsgrunnlagPrStatus atfl = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL);
        atfl.getArbeidsforhold().forEach(af -> BeregningsgrunnlagPrArbeidsforhold.builder(af)
            .medFastsattAvSaksbehandler(true)
            .medBeregnetPrÅr(fastsattPrÅr)
            .build());

        //Act
        Evaluation evaluation = new RegelForeslåBeregningsgrunnlag(grunnlag, unleash).evaluer(grunnlag);

        //Assert
        RegelResultat regelResultat = getRegelResultat(evaluation, "input");
        assertThat(regelResultat.getBeregningsresultat()).isEqualTo(ResultatBeregningType.BEREGNET);
        BeregningsgrunnlagPrStatus bgps = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.DP);
        assertThat(bgps.getBeregnetPrÅr()).isEqualByComparingTo(besteberegning);
        assertThat(bgps.getBruttoPrÅr()).isEqualByComparingTo(besteberegning);
        assertThat(bgps.getÅrsbeløpFraTilstøtendeYtelse()).isEqualByComparingTo(besteberegning);
        assertThat(grunnlag.getBeregningsgrunnlag().getAktivitetStatus(AktivitetStatus.DP).getHjemmel()).isEqualTo(BeregningsgrunnlagHjemmel.F_14_7);
        assertThat(bgps.getOrginalDagsatsFraTilstøtendeYtelse()).isEqualTo(beregnetDagsats.longValue());

        bgps = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL);
        assertThat(bgps.getBeregnetPrÅr()).isEqualByComparingTo(fastsattPrÅr);
        assertThat(bgps.getBruttoPrÅr()).isEqualByComparingTo(fastsattPrÅr);
        assertThat(grunnlag.getBeregningsgrunnlag().getAktivitetStatus(AktivitetStatus.ATFL).getHjemmel()).isEqualTo(BeregningsgrunnlagHjemmel.F_14_7);
    }


    private Inntektsgrunnlag lagInntektsgrunnlag(BigDecimal dagsats, LocalDate skjæringstidspunkt, int utbetalingsgrad) {

        Inntektsgrunnlag inntektsgrunnlag = new Inntektsgrunnlag();
        inntektsgrunnlag.leggTilPeriodeinntekt(Periodeinntekt.builder()
            .medInntektskildeOgPeriodeType(Inntektskilde.TILSTØTENDE_YTELSE_DP_AAP)
            .medMåned(skjæringstidspunkt)
            .medInntekt(dagsats)
            .medUtbetalingsgrad(BigDecimal.valueOf(utbetalingsgrad))
            .build());
        return inntektsgrunnlag;
    }

}
