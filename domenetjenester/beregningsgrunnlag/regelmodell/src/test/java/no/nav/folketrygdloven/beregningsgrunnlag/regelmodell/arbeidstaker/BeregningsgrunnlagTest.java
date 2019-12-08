package no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.arbeidstaker;

import static no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.BeregningsgrunnlagScenario.GRUNNBELØP_2017;
import static no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.BeregningsgrunnlagScenario.kopierOgLeggTilMånedsinntekter;
import static no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.BeregningsgrunnlagScenario.leggTilArbeidsforholdMedInntektsmelding;
import static no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.BeregningsgrunnlagScenario.leggTilArbeidsforholdUtenInntektsmelding;
import static no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.BeregningsgrunnlagScenario.leggTilMånedsinntekter;
import static no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.BeregningsgrunnlagScenario.opprettBeregningsgrunnlagFraInntektskomponenten;
import static no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.BeregningsgrunnlagScenario.opprettBeregningsgrunnlagFraInntektsmelding;
import static no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.BeregningsgrunnlagScenario.opprettSammenligningsgrunnlag;
import static no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.BeregningsgrunnlagScenario.settoppGrunnlagMedEnPeriode;
import static no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.RegelmodellOversetter.getRegelResultat;
import static no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.VerifiserBeregningsgrunnlag.verifiserBeregningsgrunnlagBruttoPrPeriodeType;
import static no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.VerifiserBeregningsgrunnlag.verifiserBeregningsperiode;
import static no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.VerifiserBeregningsgrunnlag.verifiserRegelmerknad;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import no.finn.unleash.FakeUnleash;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.foreslå.RegelForeslåBeregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Aktivitet;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.BeregningsgrunnlagHjemmel;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.RegelResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.ResultatBeregningType;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Arbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Inntektsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Inntektskilde;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.fpsak.nare.evaluation.Evaluation;

public class BeregningsgrunnlagTest {

    private static final String ORGNR2 = "654321987";
    private final LocalDate skjæringstidspunkt = LocalDate.of(2018, Month.JANUARY, 15);
    private FakeUnleash unleash = new FakeUnleash();

    @Test
    public void skalGiRegelmerknadVedNullFrilansInntektSisteTreMåneder() {
        // Arrange
        BigDecimal månedsinntekt = BigDecimal.ZERO;
        BigDecimal refusjonskrav = BigDecimal.valueOf(GRUNNBELØP_2017 / 12 / 2);
        BigDecimal månedsinntektSammenligning = BigDecimal.valueOf(5000);
        Beregningsgrunnlag beregningsgrunnlag = opprettBeregningsgrunnlagFraInntektskomponenten(skjæringstidspunkt, månedsinntekt, refusjonskrav, true);
        BeregningsgrunnlagPeriode grunnlag = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        opprettSammenligningsgrunnlag(grunnlag.getInntektsgrunnlag(), skjæringstidspunkt, månedsinntektSammenligning);
        // Act
        Evaluation evaluation = new RegelForeslåBeregningsgrunnlag(grunnlag, unleash).evaluer(grunnlag);
        // Assert

        RegelResultat regelResultat = getRegelResultat(evaluation, "input");
        assertThat(regelResultat.getBeregningsresultat()).isEqualTo(ResultatBeregningType.IKKE_BEREGNET);
        verifiserRegelmerknad(regelResultat, "5038");
        verifiserBeregningsgrunnlagBruttoPrPeriodeType(grunnlag, BeregningsgrunnlagHjemmel.HJEMMEL_BARE_FRILANSER, AktivitetStatus.ATFL, 12 * månedsinntekt.doubleValue());
        Periode beregningsperiode = Periode.månederFør(beregningsgrunnlag.getSkjæringstidspunkt(), 3);
        BeregningsgrunnlagPrArbeidsforhold af = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL).getArbeidsforhold().get(0);
        verifiserBeregningsperiode(af, beregningsperiode);
    }

    @Test
    public void skalBeregneGrunnlagAGVedSammeFrilansInntektSisteTreMåneder() {
        // Arrange
        BigDecimal månedsinntekt = BigDecimal.valueOf(GRUNNBELØP_2017 / 12 / 2);
        BigDecimal refusjonskrav = månedsinntekt;
        Beregningsgrunnlag beregningsgrunnlag = opprettBeregningsgrunnlagFraInntektskomponenten(skjæringstidspunkt, månedsinntekt, refusjonskrav, true);
        BeregningsgrunnlagPeriode grunnlag = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        opprettSammenligningsgrunnlag(grunnlag.getInntektsgrunnlag(), skjæringstidspunkt, månedsinntekt);
        // Act
        Evaluation evaluation = new RegelForeslåBeregningsgrunnlag(grunnlag, unleash).evaluer(grunnlag);
        // Assert

        RegelResultat regelResultat = getRegelResultat(evaluation, "input");
        assertThat(regelResultat.getBeregningsresultat()).isEqualTo(ResultatBeregningType.BEREGNET);
        verifiserBeregningsgrunnlagBruttoPrPeriodeType(grunnlag, BeregningsgrunnlagHjemmel.HJEMMEL_BARE_FRILANSER, AktivitetStatus.ATFL, 12 * månedsinntekt.doubleValue());
        Periode beregningsperiode = Periode.månederFør(beregningsgrunnlag.getSkjæringstidspunkt(), 3);
        BeregningsgrunnlagPrArbeidsforhold af = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL).getArbeidsforhold().get(0);
        verifiserBeregningsperiode(af, beregningsperiode);
    }

    @Test
    public void skalBeregneGrunnlagUtenInntektsmeldingN1N2N3() {
        // Arrange
        BigDecimal månedsinntekt = BigDecimal.valueOf(GRUNNBELØP_2017 / 12 / 2);
        BigDecimal refusjonskrav = månedsinntekt;
        Beregningsgrunnlag beregningsgrunnlag = opprettBeregningsgrunnlagFraInntektskomponenten(skjæringstidspunkt, månedsinntekt, refusjonskrav, false, 3);
        BeregningsgrunnlagPeriode grunnlag = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        opprettSammenligningsgrunnlag(grunnlag.getInntektsgrunnlag(), skjæringstidspunkt, månedsinntekt);
        // Act
        Evaluation evaluation = new RegelForeslåBeregningsgrunnlag(grunnlag, unleash).evaluer(grunnlag);

        // Assert
        RegelResultat regelResultat = getRegelResultat(evaluation, "input");
        assertThat(regelResultat.getBeregningsresultat()).isEqualTo(ResultatBeregningType.BEREGNET);
        verifiserBeregningsgrunnlagBruttoPrPeriodeType(grunnlag, BeregningsgrunnlagHjemmel.HJEMMEL_BARE_ARBEIDSTAKER, AktivitetStatus.ATFL, 12 * månedsinntekt.doubleValue());
        Periode beregningsperiode = Periode.månederFør(beregningsgrunnlag.getSkjæringstidspunkt(), 3);
        BeregningsgrunnlagPrArbeidsforhold af = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL).getArbeidsforhold().get(0);
        verifiserBeregningsperiode(af, beregningsperiode);
    }

    @Test
    public void skalBeregneGrunnlagUtenInntektsmeldingN1N3() {
        // Arrange
        BigDecimal månedsinntekt = BigDecimal.valueOf(25000);
        LocalDate skjæringstidspunkt2 = LocalDate.of(2018, Month.APRIL, 26);
        Beregningsgrunnlag beregningsgrunnlag = opprettBeregningsgrunnlagFraInntektskomponenten(skjæringstidspunkt2, månedsinntekt, null, true, 1);

        Inntektsgrunnlag inntektsgrunnlag = beregningsgrunnlag.getInntektsgrunnlag();
        BeregningsgrunnlagPeriode grunnlag = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        Arbeidsforhold arbeidsforhold = Arbeidsforhold.frilansArbeidsforhold();
        leggTilMånedsinntekter(inntektsgrunnlag, skjæringstidspunkt2.minusMonths(2), List.of(månedsinntekt), Inntektskilde.INNTEKTSKOMPONENTEN_BEREGNING, arbeidsforhold);
        opprettSammenligningsgrunnlag(grunnlag.getInntektsgrunnlag(), skjæringstidspunkt2, månedsinntekt);

        // Act
        Evaluation evaluation = new RegelForeslåBeregningsgrunnlag(grunnlag, unleash).evaluer(grunnlag);

        // Assert
        RegelResultat regelResultat = getRegelResultat(evaluation, "input");
        assertThat(regelResultat.getBeregningsresultat()).isEqualTo(ResultatBeregningType.IKKE_BEREGNET);
        verifiserRegelmerknad(regelResultat, "5038");
        verifiserBeregningsgrunnlagBruttoPrPeriodeType(grunnlag, BeregningsgrunnlagHjemmel.HJEMMEL_BARE_FRILANSER, AktivitetStatus.ATFL, 12 * månedsinntekt.doubleValue() * 2 / 3);
        Periode beregningsperiode = Periode.månederFør(beregningsgrunnlag.getSkjæringstidspunkt(), 3);
        BeregningsgrunnlagPrArbeidsforhold af = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL).getArbeidsforhold().get(0);
        verifiserBeregningsperiode(af, beregningsperiode);
    }

    @Test
    public void skalBeregneGrunnlagUtenInntektsmeldingN1N2() {
        // Arrange
        BigDecimal månedsinntektForSG = BigDecimal.valueOf(GRUNNBELØP_2017 / 12 / 3);
        BigDecimal månedsinntektForBG = BigDecimal.valueOf(GRUNNBELØP_2017 / 12 / 2);
        BigDecimal refusjonskrav = månedsinntektForBG;
        Beregningsgrunnlag beregningsgrunnlag = opprettBeregningsgrunnlagFraInntektskomponenten(skjæringstidspunkt, månedsinntektForBG, refusjonskrav, false, 2);
        BeregningsgrunnlagPeriode grunnlag = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        opprettSammenligningsgrunnlag(grunnlag.getInntektsgrunnlag(), skjæringstidspunkt, månedsinntektForSG);

        // Act
        Evaluation evaluation = new RegelForeslåBeregningsgrunnlag(grunnlag, unleash).evaluer(grunnlag);

        // Assert
        RegelResultat regelResultat = getRegelResultat(evaluation, "input");
        assertThat(regelResultat.getBeregningsresultat()).isEqualTo(ResultatBeregningType.BEREGNET);
        //Beløp er satt presis slik at det blir (beregnet verdi)-0.01<beløp<(beregnet verdi)+0.01
        verifiserBeregningsgrunnlagBruttoPrPeriodeType(grunnlag, BeregningsgrunnlagHjemmel.HJEMMEL_BARE_ARBEIDSTAKER, AktivitetStatus.ATFL, 12 * 2600.66666);
        Periode beregningsperiode = Periode.månederFør(beregningsgrunnlag.getSkjæringstidspunkt(), 3);
        BeregningsgrunnlagPrArbeidsforhold af = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL).getArbeidsforhold().get(0);
        verifiserBeregningsperiode(af, beregningsperiode);
    }

    @Test
    public void skalBeregneGrunnlagUtenInntektsmeldingN1() {
        // Arrange
        BigDecimal månedsinntektForBG = BigDecimal.valueOf(GRUNNBELØP_2017 / 12 / 2);
        BigDecimal månedsinntektForSG = BigDecimal.valueOf(GRUNNBELØP_2017 / 12 / 6);
        BigDecimal refusjonskrav = månedsinntektForSG;
        Beregningsgrunnlag beregningsgrunnlag = opprettBeregningsgrunnlagFraInntektskomponenten(skjæringstidspunkt, månedsinntektForBG, refusjonskrav, false, 1);
        BeregningsgrunnlagPeriode grunnlag = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        opprettSammenligningsgrunnlag(grunnlag.getInntektsgrunnlag(), skjæringstidspunkt, månedsinntektForSG);

        // Act
        Evaluation evaluation = new RegelForeslåBeregningsgrunnlag(grunnlag, unleash).evaluer(grunnlag);

        // Assert
        RegelResultat regelResultat = getRegelResultat(evaluation, "input");
        assertThat(regelResultat.getBeregningsresultat()).isEqualTo(ResultatBeregningType.BEREGNET);
        //Beløp er satt presis slik at det blir (beregnet verdi)-0.01<beløp<(beregnet verdi)+0.01
        verifiserBeregningsgrunnlagBruttoPrPeriodeType(grunnlag, BeregningsgrunnlagHjemmel.HJEMMEL_BARE_ARBEIDSTAKER, AktivitetStatus.ATFL, 12 * 1300.3333);
        Periode beregningsperiode = Periode.månederFør(beregningsgrunnlag.getSkjæringstidspunkt(), 3);
        BeregningsgrunnlagPrArbeidsforhold af = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL).getArbeidsforhold().get(0);
        verifiserBeregningsperiode(af, beregningsperiode);
    }

    @Test
    public void skalBeregneGrunnlagAGUtenInntektsmeldingMedLønnsendring1Måned() {
        // Arrange
        BigDecimal månedsinntekt = BigDecimal.valueOf(25000);
        BigDecimal refusjonskrav = månedsinntekt;
        Beregningsgrunnlag beregningsgrunnlag = opprettBeregningsgrunnlagFraInntektskomponenten(skjæringstidspunkt, månedsinntekt, refusjonskrav, false);
        BeregningsgrunnlagPeriode grunnlag = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);

        BigDecimal månedsinntektFraSaksbehandler = BigDecimal.valueOf(28000);
        opprettSammenligningsgrunnlag(grunnlag.getInntektsgrunnlag(), skjæringstidspunkt, månedsinntekt);
        BeregningsgrunnlagPrArbeidsforhold beregningsgrunnlagPrArbeidsforhold = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL).getArbeidsforhold().get(0);
        BeregningsgrunnlagPrArbeidsforhold.builder(beregningsgrunnlagPrArbeidsforhold)
            .medFastsattAvSaksbehandler(true)
            .medBeregnetPrÅr(månedsinntektFraSaksbehandler.multiply(BigDecimal.valueOf(12)))
            .build();

        // Act
        Evaluation evaluation = new RegelForeslåBeregningsgrunnlag(grunnlag, unleash).evaluer(grunnlag);

        // Assert
        RegelResultat regelResultat = getRegelResultat(evaluation, "input");
        assertThat(regelResultat.getBeregningsresultat()).isEqualTo(ResultatBeregningType.BEREGNET);
        verifiserBeregningsgrunnlagBruttoPrPeriodeType(grunnlag, BeregningsgrunnlagHjemmel.HJEMMEL_BARE_ARBEIDSTAKER, AktivitetStatus.ATFL, 12 * månedsinntektFraSaksbehandler.doubleValue());
        Periode beregningsperiode = Periode.månederFør(beregningsgrunnlag.getSkjæringstidspunkt(), 3);
        BeregningsgrunnlagPrArbeidsforhold af = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL).getArbeidsforhold().get(0);
        verifiserBeregningsperiode(af, beregningsperiode);
    }

    @Test
    public void skalBeregneGrunnlagAGUtenInntektsmeldingMedLønnsendring2Måneder() {
        // Arrange
        BigDecimal månedsinntekt = BigDecimal.valueOf(25000);
        BigDecimal refusjonskrav = månedsinntekt;
        Beregningsgrunnlag beregningsgrunnlag = opprettBeregningsgrunnlagFraInntektskomponenten(skjæringstidspunkt, månedsinntekt, refusjonskrav, false);
        Inntektsgrunnlag inntektsgrunnlag = beregningsgrunnlag.getInntektsgrunnlag();
        opprettSammenligningsgrunnlag(inntektsgrunnlag, skjæringstidspunkt, månedsinntekt);
        BeregningsgrunnlagPeriode grunnlag = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);

        BigDecimal månedsinntektFraSaksbehandler = BigDecimal.valueOf(28000);
        BeregningsgrunnlagPrArbeidsforhold beregningsgrunnlagPrArbeidsforhold = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL).getArbeidsforhold().get(0);
        BeregningsgrunnlagPrArbeidsforhold.builder(beregningsgrunnlagPrArbeidsforhold)
            .medFastsattAvSaksbehandler(true)
            .medBeregnetPrÅr(månedsinntektFraSaksbehandler.multiply(BigDecimal.valueOf(12)))
            .build();

        // Act
        Evaluation evaluation = new RegelForeslåBeregningsgrunnlag(grunnlag, unleash).evaluer(grunnlag);

        // Assert
        RegelResultat regelResultat = getRegelResultat(evaluation, "input");
        assertThat(regelResultat.getBeregningsresultat()).isEqualTo(ResultatBeregningType.BEREGNET);
        verifiserBeregningsgrunnlagBruttoPrPeriodeType(grunnlag, BeregningsgrunnlagHjemmel.HJEMMEL_BARE_ARBEIDSTAKER, AktivitetStatus.ATFL, 12 * månedsinntektFraSaksbehandler.doubleValue());
        Periode beregningsperiode = Periode.månederFør(beregningsgrunnlag.getSkjæringstidspunkt(), 3);
        BeregningsgrunnlagPrArbeidsforhold af = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL).getArbeidsforhold().get(0);
        verifiserBeregningsperiode(af, beregningsperiode);
    }

    @Test
    public void skalBeregneGrunnlagAGUtenInntektsmeldingMedLønnsendring3Måneder() {
        // Arrange
        BigDecimal månedsinntekt = BigDecimal.valueOf(25000);
        BigDecimal refusjonskrav = månedsinntekt;
        Beregningsgrunnlag beregningsgrunnlag = opprettBeregningsgrunnlagFraInntektskomponenten(skjæringstidspunkt, månedsinntekt, refusjonskrav, false);
        BeregningsgrunnlagPeriode grunnlag = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);

        BigDecimal månedsinntektFraSaksbehandler = BigDecimal.valueOf(28000);
        opprettSammenligningsgrunnlag(grunnlag.getInntektsgrunnlag(), skjæringstidspunkt, månedsinntekt);
        BeregningsgrunnlagPrArbeidsforhold beregningsgrunnlagPrArbeidsforhold = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL).getArbeidsforhold().get(0);
        BeregningsgrunnlagPrArbeidsforhold.builder(beregningsgrunnlagPrArbeidsforhold)
            .medFastsattAvSaksbehandler(true)
            .medBeregnetPrÅr(månedsinntektFraSaksbehandler.multiply(BigDecimal.valueOf(12)))
            .build();

        // Act
        Evaluation evaluation = new RegelForeslåBeregningsgrunnlag(grunnlag, unleash).evaluer(grunnlag);

        // Assert
        RegelResultat regelResultat = getRegelResultat(evaluation, "input");
        assertThat(regelResultat.getBeregningsresultat()).isEqualTo(ResultatBeregningType.BEREGNET);
        verifiserBeregningsgrunnlagBruttoPrPeriodeType(grunnlag, BeregningsgrunnlagHjemmel.HJEMMEL_BARE_ARBEIDSTAKER, AktivitetStatus.ATFL, 12 * månedsinntektFraSaksbehandler.doubleValue());
        Periode beregningsperiode = Periode.månederFør(beregningsgrunnlag.getSkjæringstidspunkt(), 3);
        BeregningsgrunnlagPrArbeidsforhold af = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL).getArbeidsforhold().get(0);
        verifiserBeregningsperiode(af, beregningsperiode);
    }

    @Test
    public void skalGiRegelmerknadVedAvvikVedLønnsøkning() {
        // Arrange
        BigDecimal månedsinntekt = BigDecimal.valueOf(25000);
        BigDecimal refusjonskrav = månedsinntekt;
        Beregningsgrunnlag beregningsgrunnlag = opprettBeregningsgrunnlagFraInntektskomponenten(skjæringstidspunkt, månedsinntekt, refusjonskrav, false);
        BeregningsgrunnlagPeriode grunnlag = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);

        BigDecimal månedsinntektFraSaksbehandler = BigDecimal.valueOf(35000);  // 40% avvik, dvs > 25% avvik
        opprettSammenligningsgrunnlag(grunnlag.getInntektsgrunnlag(), skjæringstidspunkt, månedsinntekt);
        BeregningsgrunnlagPrArbeidsforhold beregningsgrunnlagPrArbeidsforhold = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL).getArbeidsforhold().get(0);
        BeregningsgrunnlagPrArbeidsforhold.builder(beregningsgrunnlagPrArbeidsforhold)
            .medFastsattAvSaksbehandler(true)
            .medBeregnetPrÅr(månedsinntektFraSaksbehandler.multiply(BigDecimal.valueOf(12)))
            .build();

        // Act
        Evaluation evaluation = new RegelForeslåBeregningsgrunnlag(grunnlag, unleash).evaluer(grunnlag);

        // Assert
        RegelResultat regelResultat = getRegelResultat(evaluation, "input");
        verifiserRegelmerknad(regelResultat, "5038");

        verifiserBeregningsgrunnlagBruttoPrPeriodeType(grunnlag, BeregningsgrunnlagHjemmel.HJEMMEL_BARE_ARBEIDSTAKER, AktivitetStatus.ATFL, 12 * månedsinntektFraSaksbehandler.doubleValue());
        Periode beregningsperiode = Periode.månederFør(beregningsgrunnlag.getSkjæringstidspunkt(), 3);
        BeregningsgrunnlagPrArbeidsforhold af = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL).getArbeidsforhold().get(0);
        verifiserBeregningsperiode(af, beregningsperiode);
    }

    @Test
    public void skalBeregneGrunnlagAGMedInntektsmelding() {
        // Arrange
        BigDecimal månedsinntekt = BigDecimal.valueOf(GRUNNBELØP_2017 / 12 / 2);
        BigDecimal refusjonskrav = månedsinntekt;
        Beregningsgrunnlag beregningsgrunnlag = opprettBeregningsgrunnlagFraInntektskomponenten(skjæringstidspunkt, månedsinntekt, refusjonskrav, false);
        Inntektsgrunnlag inntektsgrunnlag = beregningsgrunnlag.getInntektsgrunnlag();
        BeregningsgrunnlagPeriode grunnlag = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        Arbeidsforhold arbeidsforhold = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL).getArbeidsforhold().get(0).getArbeidsforhold();

        BigDecimal månedsinntektFraInntektsmelding = månedsinntekt.multiply(BigDecimal.valueOf(1.1));
        leggTilMånedsinntekter(inntektsgrunnlag, beregningsgrunnlag.getSkjæringstidspunkt(),
            List.of(månedsinntektFraInntektsmelding), Inntektskilde.INNTEKTSMELDING, arbeidsforhold);
        opprettSammenligningsgrunnlag(grunnlag.getInntektsgrunnlag(), skjæringstidspunkt, månedsinntekt);

        // Act
        Evaluation evaluation = new RegelForeslåBeregningsgrunnlag(grunnlag, unleash).evaluer(grunnlag);

        // Assert
        RegelResultat regelResultat = getRegelResultat(evaluation, "input");
        assertThat(regelResultat.getBeregningsresultat()).isEqualTo(ResultatBeregningType.BEREGNET);
        verifiserBeregningsgrunnlagBruttoPrPeriodeType(grunnlag, BeregningsgrunnlagHjemmel.HJEMMEL_BARE_ARBEIDSTAKER, AktivitetStatus.ATFL, 12 * månedsinntektFraInntektsmelding.doubleValue());
        BeregningsgrunnlagPrArbeidsforhold af = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL).getArbeidsforhold().get(0);
        Periode beregningsperiode = Periode.månederFør(beregningsgrunnlag.getSkjæringstidspunkt(), 3);
        verifiserBeregningsperiode(af, beregningsperiode);
    }

    @Test
    public void toArbeidsforholdMedInntektsmelding() {
        // Arrange
        BigDecimal månedsinntekt1 = BigDecimal.valueOf(15000);
        BigDecimal månedsinntekt2 = BigDecimal.valueOf(25000);
        BigDecimal refusjonskrav = BigDecimal.valueOf(25000);
        Beregningsgrunnlag beregningsgrunnlag = opprettBeregningsgrunnlagFraInntektsmelding(skjæringstidspunkt, månedsinntekt1, refusjonskrav);

        kopierOgLeggTilMånedsinntekter(beregningsgrunnlag.getInntektsgrunnlag(), skjæringstidspunkt, månedsinntekt1.add(månedsinntekt2), Inntektskilde.INNTEKTSKOMPONENTEN_SAMMENLIGNING, null, 12);

        Arbeidsforhold arbeidsforhold2 = Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(ORGNR2);
        BeregningsgrunnlagPeriode grunnlag = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        leggTilArbeidsforholdMedInntektsmelding(grunnlag, skjæringstidspunkt, månedsinntekt2, refusjonskrav, arbeidsforhold2, BigDecimal.ZERO, null);

        // Act
        Evaluation evaluation = new RegelForeslåBeregningsgrunnlag(grunnlag, unleash).evaluer(grunnlag);

        // Assert
        RegelResultat regelResultat = getRegelResultat(evaluation, "input");
        assertThat(regelResultat.getBeregningsresultat()).isEqualTo(ResultatBeregningType.BEREGNET);
        verifiserBeregningsgrunnlagBruttoPrPeriodeType(grunnlag, BeregningsgrunnlagHjemmel.HJEMMEL_BARE_ARBEIDSTAKER, AktivitetStatus.ATFL, 12 * månedsinntekt1.add(månedsinntekt2).doubleValue());
        BeregningsgrunnlagPrArbeidsforhold af = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL).getArbeidsforhold().get(0);
        Periode beregningsperiode = Periode.månederFør(beregningsgrunnlag.getSkjæringstidspunkt(), 3);
        verifiserBeregningsperiode(af, beregningsperiode);
    }

    @Test
    public void toFrilansArbeidsforhold() {
        // Arrange
        BigDecimal månedsinntekt1 = BigDecimal.valueOf(15000);
        BigDecimal månedsinntekt2 = BigDecimal.valueOf(25000);
        BigDecimal refusjonskrav = BigDecimal.valueOf(25000);
        Beregningsgrunnlag beregningsgrunnlag = opprettBeregningsgrunnlagFraInntektskomponenten(skjæringstidspunkt, månedsinntekt1, refusjonskrav, true);

        kopierOgLeggTilMånedsinntekter(beregningsgrunnlag.getInntektsgrunnlag(), skjæringstidspunkt, månedsinntekt1.add(månedsinntekt2), Inntektskilde.INNTEKTSKOMPONENTEN_SAMMENLIGNING, null, 12);

        Arbeidsforhold arbeidsforhold2 = Arbeidsforhold.frilansArbeidsforhold();
        kopierOgLeggTilMånedsinntekter(beregningsgrunnlag.getInntektsgrunnlag(), skjæringstidspunkt, månedsinntekt2, Inntektskilde.INNTEKTSKOMPONENTEN_BEREGNING, arbeidsforhold2, 12);
        BeregningsgrunnlagPeriode grunnlag = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
//        leggTilArbeidsforholdUtenInntektsmelding(grunnlag, skjæringstidspunkt, månedsinntekt2, refusjonskrav, arbeidsforhold2);

        // Act
        Evaluation evaluation = new RegelForeslåBeregningsgrunnlag(grunnlag, unleash).evaluer(grunnlag);

        // Assert
        RegelResultat regelResultat = getRegelResultat(evaluation, "input");
        assertThat(regelResultat.getBeregningsresultat()).isEqualTo(ResultatBeregningType.BEREGNET);
        verifiserBeregningsgrunnlagBruttoPrPeriodeType(grunnlag, BeregningsgrunnlagHjemmel.HJEMMEL_BARE_FRILANSER, AktivitetStatus.ATFL, 12 * (månedsinntekt1.add(månedsinntekt2)).doubleValue());
        BeregningsgrunnlagPrArbeidsforhold af = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL).getArbeidsforhold().get(0);
        assertThat(af.getBeregningsperiode()).isNotNull();
    }

    @Test
    public void kombinasjonArbeidstakerOgFrilans() {
        // Arrange
        BigDecimal månedsinntektFrilans = BigDecimal.valueOf(15000);
        BigDecimal månedsinntektArbeidstaker = BigDecimal.valueOf(25000);
        BigDecimal refusjonskravArbeidstaker = BigDecimal.valueOf(25000);
        Beregningsgrunnlag beregningsgrunnlag = opprettBeregningsgrunnlagFraInntektsmelding(skjæringstidspunkt, månedsinntektArbeidstaker, refusjonskravArbeidstaker);

        kopierOgLeggTilMånedsinntekter(beregningsgrunnlag.getInntektsgrunnlag(), skjæringstidspunkt, månedsinntektFrilans.add(månedsinntektArbeidstaker), Inntektskilde.INNTEKTSKOMPONENTEN_SAMMENLIGNING, null, 12);

        Arbeidsforhold frilans = Arbeidsforhold.frilansArbeidsforhold();
        BeregningsgrunnlagPeriode grunnlag = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        leggTilArbeidsforholdUtenInntektsmelding(grunnlag, skjæringstidspunkt, månedsinntektFrilans, null, frilans);

        // Act
        Evaluation evaluation = new RegelForeslåBeregningsgrunnlag(grunnlag, unleash).evaluer(grunnlag);

        // Assert
        RegelResultat regelResultat = getRegelResultat(evaluation, "input");
        assertThat(regelResultat.getBeregningsresultat()).isEqualTo(ResultatBeregningType.BEREGNET);
        verifiserBeregningsgrunnlagBruttoPrPeriodeType(grunnlag, BeregningsgrunnlagHjemmel.HJEMMEL_ARBEIDSTAKER_OG_FRILANSER, AktivitetStatus.ATFL, 12 * (månedsinntektFrilans.add(månedsinntektArbeidstaker)).doubleValue());
        BeregningsgrunnlagPrArbeidsforhold af = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL).getArbeidsforhold().get(0);
        Periode beregningsperiode = Periode.månederFør(beregningsgrunnlag.getSkjæringstidspunkt(), 3);
        verifiserBeregningsperiode(af, beregningsperiode);
    }

    @Test
    public void kombinasjonArbeidstakerOgFrilansDerFrilansinntektErNull() {
        // Arrange
        BigDecimal månedsinntektFrilans = BigDecimal.ZERO;
        BigDecimal månedsinntektArbeidstaker = BigDecimal.valueOf(25000);
        BigDecimal refusjonskravFrilans = BigDecimal.ZERO;
        BigDecimal refusjonskravArbeidstaker = BigDecimal.valueOf(25000);
        Beregningsgrunnlag beregningsgrunnlag = opprettBeregningsgrunnlagFraInntektsmelding(skjæringstidspunkt, månedsinntektArbeidstaker, refusjonskravArbeidstaker);

        kopierOgLeggTilMånedsinntekter(beregningsgrunnlag.getInntektsgrunnlag(), skjæringstidspunkt, månedsinntektFrilans.add(månedsinntektArbeidstaker), Inntektskilde.INNTEKTSKOMPONENTEN_SAMMENLIGNING, null, 12);

        Arbeidsforhold frilans = Arbeidsforhold.frilansArbeidsforhold();
        BeregningsgrunnlagPeriode grunnlag = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        leggTilArbeidsforholdUtenInntektsmelding(grunnlag, skjæringstidspunkt, månedsinntektFrilans, refusjonskravFrilans, frilans);

        // Act
        Evaluation evaluation = new RegelForeslåBeregningsgrunnlag(grunnlag, unleash).evaluer(grunnlag);

        // Assert
        RegelResultat regelResultat = getRegelResultat(evaluation, "input");
        assertThat(regelResultat.getBeregningsresultat()).isEqualTo(ResultatBeregningType.BEREGNET);
        verifiserBeregningsgrunnlagBruttoPrPeriodeType(grunnlag, BeregningsgrunnlagHjemmel.HJEMMEL_ARBEIDSTAKER_OG_FRILANSER, AktivitetStatus.ATFL, 12 * (månedsinntektFrilans.add(månedsinntektArbeidstaker)).doubleValue());
        BeregningsgrunnlagPrArbeidsforhold af = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL).getArbeidsforhold().get(0);
        Periode beregningsperiode = Periode.månederFør(beregningsgrunnlag.getSkjæringstidspunkt(), 3);
        verifiserBeregningsperiode(af, beregningsperiode);
    }

    @Test
    public void skalTesteManueltFastsattMånedsinntekt() {
        int beregnetPrÅr = 250000;
        List<Arbeidsforhold> arbeidsforholdList = Collections.singletonList(Arbeidsforhold.builder().medOrgnr("123").medAktivitet(Aktivitet.ARBEIDSTAKERINNTEKT).build());
        Beregningsgrunnlag beregningsgrunnlag = settoppGrunnlagMedEnPeriode(skjæringstidspunkt, new Inntektsgrunnlag(),
            Collections.singletonList(AktivitetStatus.ATFL), arbeidsforholdList);
        opprettSammenligningsgrunnlag(beregningsgrunnlag.getInntektsgrunnlag(), skjæringstidspunkt, BigDecimal.valueOf(22500));
        BeregningsgrunnlagPeriode grunnlag = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        BeregningsgrunnlagPrArbeidsforhold.builder(grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL).getArbeidsforhold().get(0))
            .medFastsattAvSaksbehandler(true)
            .medBeregnetPrÅr(BigDecimal.valueOf(beregnetPrÅr))
            .build();

        // Act
        Evaluation evaluation = new RegelForeslåBeregningsgrunnlag(grunnlag, unleash).evaluer(grunnlag);

        // Assert
        RegelResultat regelResultat = getRegelResultat(evaluation, "input");
        assertThat(regelResultat.getRegelSporing().getSporing()).contains(BeregnRapportertInntektVedManuellFastsettelse.ID);

        assertThat(regelResultat.getBeregningsresultat()).isEqualTo(ResultatBeregningType.BEREGNET);
        BeregningsgrunnlagPrArbeidsforhold af = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL).getArbeidsforhold().get(0);
        assertThat(af.getBeregnetPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(beregnetPrÅr));
    }
}
