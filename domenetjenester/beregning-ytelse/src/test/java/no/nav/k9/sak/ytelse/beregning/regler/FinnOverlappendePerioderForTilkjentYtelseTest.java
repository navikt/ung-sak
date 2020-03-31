package no.nav.k9.sak.ytelse.beregning.regler;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.summary.EvaluationSerializer;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.ytelse.beregning.regelmodell.Beregningsresultat;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatAndel;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatRegelmodell;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatRegelmodellMellomregning;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakAktivitet;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultat;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultatPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Beregningsgrunnlag;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrStatus;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Periode;

@Ignore("Espen Velsvik: Venter på at kalkulus returnerer ikke-redusert dagsats")
public class FinnOverlappendePerioderForTilkjentYtelseTest {

    private final String orgnr = "123";
    private BeregningsgrunnlagPrArbeidsforhold prArbeidsforhold;
    private Arbeidsforhold arbeidsforhold;
    private final FagsakYtelseType ytelseType = FagsakYtelseType.OMSORGSPENGER;
    /*
     * For eksempler brukt i testene under se https://confluence.adeo.no/display/MODNAV/27b.+Beregne+tilkjent+ytelse
     */

    @Test
    public void skal_gradere_deltiddstilling_eksempel_1() {
        // Arrange
        int redBrukersAndelPrÅr = 0;
        int redRefusjonPrÅr = 10000;
        BigDecimal stillingsgrad = BigDecimal.valueOf(50);
        int nyArbeidstidProsent = 40;
        long dagsatsBruker = getDagsats(redBrukersAndelPrÅr);
        long dagsatsArbeidsgiver = getDagsats(redRefusjonPrÅr);
        BeregningsresultatRegelmodellMellomregning mellomregning = settOppGraderingScenario(redBrukersAndelPrÅr, redRefusjonPrÅr, stillingsgrad, nyArbeidstidProsent, dagsatsBruker,
            dagsatsArbeidsgiver, true);

        // Act
        FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        Evaluation evaluation = regel.evaluate(mellomregning);
        String sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert
        long faktiskDagsatBruker = getDagsats(4000.0);
        long faktiskDagsatArbeidsgiver = getDagsats(2000.0);

        List<BeregningsresultatAndel> andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).hasSize(2);
        assertThat(andel.get(0).erBrukerMottaker()).isTrue();
        assertThat(andel.get(0).getDagsats()).isEqualTo(faktiskDagsatBruker);
        assertThat(andel.get(0).getDagsatsFraBg()).isEqualTo(dagsatsBruker);
        assertThat(andel.get(1).erBrukerMottaker()).isFalse();
        assertThat(andel.get(1).getDagsats()).isEqualTo(faktiskDagsatArbeidsgiver);
        assertThat(andel.get(1).getDagsatsFraBg()).isEqualTo(dagsatsArbeidsgiver);
    }

    @Test
    public void skal_gradere_deltiddstilling_eksempel_2() {
        // Arrange
        int redBrukersAndelPrÅr = 1000;
        int redRefusjonPrÅr = 9000;
        BigDecimal stillingsgrad = BigDecimal.valueOf(50);
        int nyArbeidstidProsent = 40;
        long dagsatsBruker = getDagsats(redBrukersAndelPrÅr);
        long dagsatsArbeidsgiver = getDagsats(redRefusjonPrÅr);
        BeregningsresultatRegelmodellMellomregning mellomregning = settOppGraderingScenario(redBrukersAndelPrÅr, redRefusjonPrÅr, stillingsgrad, nyArbeidstidProsent, dagsatsBruker,
            dagsatsArbeidsgiver, true);

        // Act
        FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        Evaluation evaluation = regel.evaluate(mellomregning);
        String sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert
        long faktiskDagsatBruker = getDagsats(4200.0);
        long faktiskDagsatArbeidsgiver = getDagsats(1800.0);

        List<BeregningsresultatAndel> andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).hasSize(2);
        assertThat(andel.get(0).erBrukerMottaker()).isTrue();
        assertThat(andel.get(0).getDagsats()).isEqualTo(faktiskDagsatBruker);
        assertThat(andel.get(0).getDagsatsFraBg()).isEqualTo(dagsatsBruker);
        assertThat(andel.get(1).erBrukerMottaker()).isFalse();
        assertThat(andel.get(1).getDagsats()).isEqualTo(faktiskDagsatArbeidsgiver);
        assertThat(andel.get(1).getDagsatsFraBg()).isEqualTo(dagsatsArbeidsgiver);
    }

    @Test
    public void skal_gradere_deltiddstilling_eksempel_3() {
        // Arrange
        int redBrukersAndelPrÅr = 0;
        int redRefusjonPrÅr = 100000;
        BigDecimal stillingsgrad = BigDecimal.valueOf(50);
        int nyArbeidstidProsent = 50;
        long dagsatsBruker = getDagsats(redBrukersAndelPrÅr);
        long dagsatsArbeidsgiver = getDagsats(redRefusjonPrÅr);
        BeregningsresultatRegelmodellMellomregning mellomregning = settOppGraderingScenario(redBrukersAndelPrÅr, redRefusjonPrÅr, stillingsgrad, nyArbeidstidProsent, dagsatsBruker,
            dagsatsArbeidsgiver, true);

        // Act
        FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        Evaluation evaluation = regel.evaluate(mellomregning);
        String sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert
        long faktiskDagsatBruker = getDagsats(50000.0);

        List<BeregningsresultatAndel> andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).hasSize(2);
        assertThat(andel.get(0).erBrukerMottaker()).isTrue();
        assertThat(andel.get(0).getDagsats()).isEqualTo(faktiskDagsatBruker);
        assertThat(andel.get(0).getDagsatsFraBg()).isEqualTo(dagsatsBruker);
        assertThat(andel.get(1).erBrukerMottaker()).isFalse();
        assertThat(andel.get(1).getDagsats()).isEqualTo(0L);
        assertThat(andel.get(1).getDagsatsFraBg()).isEqualTo(dagsatsArbeidsgiver);
    }

    @Test
    public void skal_gradere_deltiddstilling_eksempel_4() {
        // Arrange
        int redBrukersAndelPrÅr = 10000;
        int redRefusjonPrÅr = 90000;
        BigDecimal stillingsgrad = BigDecimal.valueOf(50);
        int nyArbeidstidProsent = 50;
        long dagsatsBruker = getDagsats(redBrukersAndelPrÅr);
        long dagsatsArbeidsgiver = getDagsats(redRefusjonPrÅr);
        BeregningsresultatRegelmodellMellomregning mellomregning = settOppGraderingScenario(redBrukersAndelPrÅr, redRefusjonPrÅr, stillingsgrad, nyArbeidstidProsent, dagsatsBruker,
            dagsatsArbeidsgiver, true);

        // Act
        FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        Evaluation evaluation = regel.evaluate(mellomregning);
        String sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert
        long faktiskDagsatBruker = getDagsats(50000.0);

        List<BeregningsresultatAndel> andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).hasSize(2);
        assertThat(andel.get(0).erBrukerMottaker()).isTrue();
        assertThat(andel.get(0).getDagsats()).isEqualTo(faktiskDagsatBruker);
        assertThat(andel.get(0).getDagsatsFraBg()).isEqualTo(dagsatsBruker);
        assertThat(andel.get(1).erBrukerMottaker()).isFalse();
        assertThat(andel.get(1).getDagsats()).isEqualTo(0L);
        assertThat(andel.get(1).getDagsatsFraBg()).isEqualTo(dagsatsArbeidsgiver);
    }

    @Test
    public void skal_gradere_deltiddstilling_eksempel_5() {
        // Arrange
        int redBrukersAndelPrÅr = 0;
        int redRefusjonPrÅr = 100000;
        BigDecimal stillingsgrad = BigDecimal.valueOf(50);
        int nyArbeidstidProsent = 60;
        long dagsatsBruker = getDagsats(redBrukersAndelPrÅr);
        long dagsatsArbeidsgiver = getDagsats(redRefusjonPrÅr);
        BeregningsresultatRegelmodellMellomregning mellomregning = settOppGraderingScenario(redBrukersAndelPrÅr, redRefusjonPrÅr, stillingsgrad, nyArbeidstidProsent, dagsatsBruker,
            dagsatsArbeidsgiver, true);

        // Act
        FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        Evaluation evaluation = regel.evaluate(mellomregning);
        String sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert
        long faktiskDagsatBruker = getDagsats(40000.0);

        List<BeregningsresultatAndel> andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).hasSize(2);
        assertThat(andel.get(0).erBrukerMottaker()).isTrue();
        assertThat(andel.get(0).getDagsats()).isEqualTo(faktiskDagsatBruker);
        assertThat(andel.get(0).getDagsatsFraBg()).isEqualTo(dagsatsBruker);
        assertThat(andel.get(1).erBrukerMottaker()).isFalse();
        assertThat(andel.get(1).getDagsats()).isEqualTo(0L);
        assertThat(andel.get(1).getDagsatsFraBg()).isEqualTo(dagsatsArbeidsgiver);
    }

    @Test
    public void skal_gradere_deltiddstilling_eksempel_6() {
        // Arrange
        int redBrukersAndelPrÅr = 10000;
        int redRefusjonPrÅr = 90000;
        BigDecimal stillingsgrad = BigDecimal.valueOf(50);
        int nyArbeidstidProsent = 60;
        long dagsatsBruker = getDagsats(redBrukersAndelPrÅr);
        long dagsatsArbeidsgiver = getDagsats(redRefusjonPrÅr);
        BeregningsresultatRegelmodellMellomregning mellomregning = settOppGraderingScenario(redBrukersAndelPrÅr, redRefusjonPrÅr, stillingsgrad, nyArbeidstidProsent, dagsatsBruker,
            dagsatsArbeidsgiver, true);

        // Act
        FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        Evaluation evaluation = regel.evaluate(mellomregning);
        String sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert
        long faktiskDagsatBruker = getDagsats(40000.0);

        List<BeregningsresultatAndel> andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).hasSize(2);
        assertThat(andel.get(0).erBrukerMottaker()).isTrue();
        assertThat(andel.get(0).getDagsats()).isEqualTo(faktiskDagsatBruker);
        assertThat(andel.get(0).getDagsatsFraBg()).isEqualTo(dagsatsBruker);
        assertThat(andel.get(1).erBrukerMottaker()).isFalse();
        assertThat(andel.get(1).getDagsats()).isEqualTo(0L);
        assertThat(andel.get(1).getDagsatsFraBg()).isEqualTo(dagsatsArbeidsgiver);
    }

    @Test
    public void skal_gradere_heltiddstilling_eksempel_7() {
        // Arrange
        int redBrukersAndelPrÅr = 0;
        int redRefusjonPrÅr = 100000;
        BigDecimal stillingsgrad = BigDecimal.valueOf(100);
        int nyArbeidstidProsent = 50;
        long dagsatsBruker = getDagsats(redBrukersAndelPrÅr);
        long dagsatsArbeidsgiver = getDagsats(redRefusjonPrÅr);
        BeregningsresultatRegelmodellMellomregning mellomregning = settOppGraderingScenario(redBrukersAndelPrÅr, redRefusjonPrÅr, stillingsgrad, nyArbeidstidProsent, dagsatsBruker,
            dagsatsArbeidsgiver, true);

        // Act
        FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        Evaluation evaluation = regel.evaluate(mellomregning);
        String sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert
        long faktiskDagsatArbeidsgiver = getDagsats(50000.0);

        List<BeregningsresultatAndel> andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).hasSize(2);
        assertThat(andel.get(0).erBrukerMottaker()).isTrue();
        assertThat(andel.get(0).getDagsats()).isEqualTo(0L);
        assertThat(andel.get(0).getDagsatsFraBg()).isEqualTo(dagsatsBruker);
        assertThat(andel.get(1).erBrukerMottaker()).isFalse();
        assertThat(andel.get(1).getDagsats()).isEqualTo(faktiskDagsatArbeidsgiver);
        assertThat(andel.get(1).getDagsatsFraBg()).isEqualTo(dagsatsArbeidsgiver);
    }

    @Test
    public void skal_gradere_heltiddstilling_eksempel_8() {
        // Arrange
        int redBrukersAndelPrÅr = 10000;
        int redRefusjonPrÅr = 90000;
        BigDecimal stillingsgrad = BigDecimal.valueOf(100);
        int nyArbeidstidProsent = 50;
        long dagsatsBruker = getDagsats(redBrukersAndelPrÅr);
        long dagsatsArbeidsgiver = getDagsats(redRefusjonPrÅr);
        BeregningsresultatRegelmodellMellomregning mellomregning = settOppGraderingScenario(redBrukersAndelPrÅr, redRefusjonPrÅr, stillingsgrad, nyArbeidstidProsent, dagsatsBruker,
            dagsatsArbeidsgiver, true);

        // Act
        FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        Evaluation evaluation = regel.evaluate(mellomregning);
        String sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert
        long faktiskDagsatBruker = getDagsats(5000.0);
        long faktiskDagsatArbeidsgiver = getDagsats(45000.0);

        List<BeregningsresultatAndel> andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).hasSize(2);
        assertThat(andel.get(0).erBrukerMottaker()).isTrue();
        assertThat(andel.get(0).getDagsats()).isEqualTo(faktiskDagsatBruker);
        assertThat(andel.get(0).getDagsatsFraBg()).isEqualTo(dagsatsBruker);
        assertThat(andel.get(1).erBrukerMottaker()).isFalse();
        assertThat(andel.get(1).getDagsats()).isEqualTo(faktiskDagsatArbeidsgiver);
        assertThat(andel.get(1).getDagsatsFraBg()).isEqualTo(dagsatsArbeidsgiver);
    }

    @Test
    public void skal_gradere_deltiddstilling_eksempel_9() {
        // Arrange
        int redBrukersAndelPrÅr = 100000;
        int redRefusjonPrÅr = 500000;
        BigDecimal stillingsgrad = BigDecimal.valueOf(50);
        int nyArbeidstidProsent = 40;
        long dagsatsBruker = getDagsats(redBrukersAndelPrÅr);
        long dagsatsArbeidsgiver = getDagsats(redRefusjonPrÅr);
        BeregningsresultatRegelmodellMellomregning mellomregning = settOppGraderingScenario(redBrukersAndelPrÅr, redRefusjonPrÅr, stillingsgrad, nyArbeidstidProsent, dagsatsBruker,
            dagsatsArbeidsgiver, true);

        // Act
        FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        Evaluation evaluation = regel.evaluate(mellomregning);
        String sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert
        long faktiskDagsatBruker = getDagsats(260000.0);
        long faktiskDagsatArbeidsgiver = getDagsats(100000.0);

        List<BeregningsresultatAndel> andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).hasSize(2);
        assertThat(andel.get(0).erBrukerMottaker()).isTrue();
        assertThat(andel.get(0).getDagsats()).isEqualTo(faktiskDagsatBruker);
        assertThat(andel.get(0).getDagsatsFraBg()).isEqualTo(dagsatsBruker);
        assertThat(andel.get(1).erBrukerMottaker()).isFalse();
        assertThat(andel.get(1).getDagsats()).isEqualTo(faktiskDagsatArbeidsgiver);
        assertThat(andel.get(1).getDagsatsFraBg()).isEqualTo(dagsatsArbeidsgiver);
    }

    @Test
    public void skal_ikke_gradere_fulltidsstilling_med_full_permisjon() {
        // Arrange
        int redBrukersAndelPrÅr = 100000;
        int redRefusjonPrÅr = 0;
        BigDecimal stillingsgrad = BigDecimal.valueOf(100);
        int nyArbeidstidProsent = 0;
        long dagsatsBruker = getDagsats(redBrukersAndelPrÅr);
        long dagsatsArbeidsgiver = getDagsats(redRefusjonPrÅr);
        BeregningsresultatRegelmodellMellomregning mellomregning = settOppGraderingScenario(redBrukersAndelPrÅr, redRefusjonPrÅr, stillingsgrad, nyArbeidstidProsent, dagsatsBruker,
            dagsatsArbeidsgiver, false);

        // Act
        FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        Evaluation evaluation = regel.evaluate(mellomregning);
        String sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert

        List<BeregningsresultatAndel> andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).hasSize(1);
        assertThat(andel.get(0).erBrukerMottaker()).isTrue();
        assertThat(andel.get(0).getDagsats()).isEqualTo(dagsatsBruker);
        assertThat(andel.get(0).getDagsatsFraBg()).isEqualTo(dagsatsBruker);
    }

    @Test
    public void skal_gradere_status_SN() {
        // Arrange
        BigDecimal redusertBrukersAndel = BigDecimal.valueOf(100000);
        BigDecimal stillingsgrad = BigDecimal.valueOf(100);
        int utbetalingsgrad = 50;
        long dagsatsBruker = redusertBrukersAndel.divide(BigDecimal.valueOf(260), 0, RoundingMode.HALF_UP).longValue();
        long redDagsatsBruker = getDagsats(0.50 * redusertBrukersAndel.doubleValue());
        BeregningsresultatRegelmodellMellomregning mellomregning = settOppGraderingScenarioForAndreStatuser(redusertBrukersAndel, stillingsgrad, utbetalingsgrad, AktivitetStatus.SN,
            Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE, 
            UttakArbeidType.ANNET, 
            true);
        
        // Act
        FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        Evaluation evaluation = regel.evaluate(mellomregning);
        String sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert
        List<BeregningsresultatAndel> andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).hasSize(1);
        assertThat(andel.get(0).erBrukerMottaker()).isTrue();
        assertThat(andel.get(0).getDagsats()).isEqualTo(redDagsatsBruker);
        assertThat(andel.get(0).getDagsatsFraBg()).isEqualTo(dagsatsBruker);
        assertThat(andel.get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(utbetalingsgrad));
        assertThat(andel.get(0).getStillingsprosent()).isEqualByComparingTo(stillingsgrad);
    }

    @Test
    public void skal_teste_SN_med_oppholdsperiode() {
        // Arrange
        BigDecimal redusertBrukersAndel = BigDecimal.valueOf(100000);
        BeregningsresultatRegelmodellMellomregning mellomregning = settOppScenarioMedOppholdsperiodeForSN(redusertBrukersAndel);
        // Act
        FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        Evaluation evaluation = regel.evaluate(mellomregning);
        String sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert
        List<BeregningsresultatAndel> andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).isEmpty();
    }

    @Test
    public void skal_teste_AT_med_oppholdsperiode() {
        // Arrange
        int redBrukersAndelPrÅr = 100000;
        int redRefusjonPrÅr = 0;
        long dagsatsBruker = getDagsats(redBrukersAndelPrÅr);
        long dagsatsArbeidsgiver = getDagsats(redRefusjonPrÅr);
        BeregningsresultatRegelmodellMellomregning mellomregning = settOppScenarioMedOppholdsperiodeForAT(redBrukersAndelPrÅr, redRefusjonPrÅr, dagsatsBruker, dagsatsArbeidsgiver);

        // Act
        FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        Evaluation evaluation = regel.evaluate(mellomregning);
        String sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert
        List<BeregningsresultatAndel> andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).isEmpty();
    }

    @Test
    public void skal_gradere_status_DP() {
        // Arrange
        BigDecimal redusertBrukersAndel = BigDecimal.valueOf(100000);
        BigDecimal stillingsgrad = BigDecimal.valueOf(100);
        int utbetalingsgrad = 66;
        long dagsatsBruker = redusertBrukersAndel.divide(BigDecimal.valueOf(260), 0, RoundingMode.HALF_UP).longValue();
        long redDagsatsBruker = getDagsats(0.66 * redusertBrukersAndel.doubleValue());
        BeregningsresultatRegelmodellMellomregning mellomregning = settOppGraderingScenarioForAndreStatuser(redusertBrukersAndel, stillingsgrad, utbetalingsgrad, AktivitetStatus.DP,
            Inntektskategori.DAGPENGER, UttakArbeidType.ANNET, 
            true);
        // Act
        FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        Evaluation evaluation = regel.evaluate(mellomregning);
        String sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert
        List<BeregningsresultatAndel> andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).hasSize(1);
        assertThat(andel.get(0).erBrukerMottaker()).isTrue();
        assertThat(andel.get(0).getDagsats()).isEqualTo(redDagsatsBruker);
        assertThat(andel.get(0).getDagsatsFraBg()).isEqualTo(dagsatsBruker);
    }

    @Test
    public void gradering_når_gammel_stillingsprosent_er_0() {
        // Arrange
        int redBrukersAndelPrÅr = 260000;
        int redRefusjonPrÅr = 26000;
        BigDecimal stillingsgrad = BigDecimal.ZERO;
        int nyArbeidstidProsent = 0;
        long dagsatsBruker = getDagsats(redBrukersAndelPrÅr);
        long dagsatsArbeidsgiver = getDagsats(redRefusjonPrÅr);
        BeregningsresultatRegelmodellMellomregning mellomregning = settOppGraderingScenario(redBrukersAndelPrÅr, redRefusjonPrÅr,
            stillingsgrad, nyArbeidstidProsent, dagsatsBruker, dagsatsArbeidsgiver, true);

        // Act
        FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        Evaluation evaluation = regel.evaluate(mellomregning);
        String sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert
        List<BeregningsresultatAndel> andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).hasSize(2);
        assertThat(andel.get(0).erBrukerMottaker()).isTrue();
        assertThat(andel.get(0).getDagsats()).isEqualTo(dagsatsBruker + dagsatsArbeidsgiver);
        assertThat(andel.get(0).getDagsatsFraBg()).isEqualTo(dagsatsBruker);
        assertThat(andel.get(1).erBrukerMottaker()).isFalse();
        assertThat(andel.get(1).getDagsats()).isEqualTo(0);
        assertThat(andel.get(1).getDagsatsFraBg()).isEqualTo(dagsatsArbeidsgiver);
    }

    @Test
    public void skal_bruke_utbetalingsgrad_når_ikke_gradering() {
        // Arrange
        int redBrukersAndelPrÅr = 260000;
        int redRefusjonPrÅr = 130000;
        BigDecimal stillingsgrad = BigDecimal.ZERO;
        int nyArbeidstidProsent = 50; // Gir 50% utbetalingsgrad
        long dagsatsBruker = getDagsats(redBrukersAndelPrÅr);
        long dagsatsArbeidsgiver = getDagsats(redRefusjonPrÅr);
        BeregningsresultatRegelmodellMellomregning mellomregning = settOppGraderingScenario(redBrukersAndelPrÅr, redRefusjonPrÅr,
            stillingsgrad, nyArbeidstidProsent, dagsatsBruker, dagsatsArbeidsgiver, false);

        // Act
        FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        Evaluation evaluation = regel.evaluate(mellomregning);
        String sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert
        List<BeregningsresultatAndel> andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).hasSize(2);
        assertThat(andel.get(0).erBrukerMottaker()).isTrue();
        assertThat(andel.get(0).getDagsats()).isEqualTo(dagsatsBruker / 2);
        assertThat(andel.get(0).getDagsatsFraBg()).isEqualTo(dagsatsBruker);
        assertThat(andel.get(1).erBrukerMottaker()).isFalse();
        assertThat(andel.get(1).getDagsats()).isEqualTo(dagsatsArbeidsgiver / 2);
        assertThat(andel.get(1).getDagsatsFraBg()).isEqualTo(dagsatsArbeidsgiver);
    }

    @Test
    public void skal_ikke_regne_overkompensasjon_ved_over_100_prosent_stilling() {
        // Arrange
        int redBrukersAndelPrÅr = 100000;
        int redRefusjonPrÅr = 500000;
        BigDecimal stillingsgrad = BigDecimal.valueOf(140);
        int nyArbeidstidProsent = 40;
        long dagsatsBruker = getDagsats(redBrukersAndelPrÅr);
        long dagsatsArbeidsgiver = getDagsats(redRefusjonPrÅr);
        BeregningsresultatRegelmodellMellomregning mellomregning = settOppGraderingScenario(redBrukersAndelPrÅr, redRefusjonPrÅr, stillingsgrad, nyArbeidstidProsent, dagsatsBruker,
            dagsatsArbeidsgiver, true);

        // Act
        FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        Evaluation evaluation = regel.evaluate(mellomregning);
        String sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert
        BigDecimal redusertForDekningsgradBruker = BigDecimal.valueOf(redBrukersAndelPrÅr).multiply(BigDecimal.valueOf(60).scaleByPowerOfTen(-2));
        BigDecimal redusertForDekningsgradArbeidsgiver = BigDecimal.valueOf(redRefusjonPrÅr).multiply(BigDecimal.valueOf(60).scaleByPowerOfTen(-2));
        long faktiskDagsatBruker = getDagsats(redusertForDekningsgradBruker.intValue());
        long faktiskDagsatArbeidsgiver = getDagsats(redusertForDekningsgradArbeidsgiver.intValue());

        List<BeregningsresultatAndel> andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).hasSize(2);
        assertThat(andel.get(0).erBrukerMottaker()).isTrue();
        assertThat(andel.get(0).getDagsats()).isEqualTo(faktiskDagsatBruker);
        assertThat(andel.get(0).getDagsatsFraBg()).isEqualTo(dagsatsBruker);
        assertThat(andel.get(1).erBrukerMottaker()).isFalse();
        assertThat(andel.get(1).getDagsats()).isEqualTo(faktiskDagsatArbeidsgiver);
        assertThat(andel.get(1).getDagsatsFraBg()).isEqualTo(dagsatsArbeidsgiver);
    }

    private BeregningsresultatRegelmodellMellomregning lagMellomregning(AktivitetStatus aktivitetStatus, Inntektskategori inntektskategori, UttakArbeidType uttakArbeidType, BigDecimal stillingsgrad, BigDecimal utbetalingsgrad,
                                                                        BigDecimal redusertBrukersAndel, boolean erGradering) {
        LocalDate fom = LocalDate.now();
        LocalDate tom = LocalDate.now().plusDays(14);

        Beregningsgrunnlag grunnlag = lagBeregningsgrunnlag(fom, tom, aktivitetStatus, inntektskategori, redusertBrukersAndel);
        UttakResultat uttakResultat = new UttakResultat(ytelseType, lagUttakResultatPeriode(fom, tom, stillingsgrad, utbetalingsgrad, uttakArbeidType, erGradering));
        BeregningsresultatRegelmodell input = new BeregningsresultatRegelmodell(grunnlag, uttakResultat);
        Beregningsresultat output = new Beregningsresultat();
        return new BeregningsresultatRegelmodellMellomregning(input, output);
    }

    private BeregningsresultatRegelmodellMellomregning lagMellomregningForOppholdsPeriode(AktivitetStatus aktivitetStatus, Inntektskategori inntektskategori, BigDecimal redusertBrukersAndel) {
        LocalDate fom = LocalDate.now();
        LocalDate tom = LocalDate.now().plusDays(14);

        Beregningsgrunnlag grunnlag = lagBeregningsgrunnlag(fom, tom, aktivitetStatus, inntektskategori, redusertBrukersAndel);
        UttakResultat uttakResultat = new UttakResultat(ytelseType, lagUttakResultatForOppholdsPeriode(fom, tom));
        BeregningsresultatRegelmodell input = new BeregningsresultatRegelmodell(grunnlag, uttakResultat);
        Beregningsresultat output = new Beregningsresultat();
        return new BeregningsresultatRegelmodellMellomregning(input, output);
    }

    private List<UttakResultatPeriode> lagUttakResultatPeriode(LocalDate fom, LocalDate tom, BigDecimal stillingsgrad, BigDecimal utbetalingsgrad, UttakArbeidType uttakArbeidType,
                                                               boolean erGradering) {

        List<UttakAktivitet> uttakAktiviter = Collections.singletonList( new UttakAktivitet(stillingsgrad, utbetalingsgrad, arbeidsforhold, uttakArbeidType, erGradering));
        UttakResultatPeriode periode = new UttakResultatPeriode(fom, tom, uttakAktiviter, false);
        return List.of(periode);
    }

    private List<UttakResultatPeriode> lagUttakResultatForOppholdsPeriode(LocalDate fom, LocalDate tom) {

        UttakResultatPeriode periode = new UttakResultatPeriode(fom, tom, Collections.emptyList(), true);
        return List.of(periode);
    }

    private Beregningsgrunnlag lagBeregningsgrunnlag(LocalDate fom, LocalDate tom, AktivitetStatus aktivitetStatus, Inntektskategori inntektskategori, BigDecimal redusertBrukersAndel) {

        BeregningsgrunnlagPeriode periode1 = lagPeriode(fom, tom, aktivitetStatus, inntektskategori, redusertBrukersAndel);

        return Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(LocalDate.now())
            .medAktivitetStatuser(Collections.singletonList(aktivitetStatus))
            .medBeregningsgrunnlagPeriode(periode1)
            .build();

    }

    private BeregningsgrunnlagPeriode lagPeriode(LocalDate fom, LocalDate tom, AktivitetStatus aktivitetStatus, Inntektskategori inntektskategori, BigDecimal redusertBrukersAndel) {
        Periode periode = new Periode(fom, tom);
        BeregningsgrunnlagPrStatus.Builder builder = BeregningsgrunnlagPrStatus.builder()
            .medAktivitetStatus(aktivitetStatus)
            .medRedusertBrukersAndelPrÅr(redusertBrukersAndel);

        if (AktivitetStatus.ATFL.equals(aktivitetStatus)) {
            builder.medArbeidsforhold(prArbeidsforhold);
        } else {
            builder.medInntektskategori(inntektskategori);
        }

        BeregningsgrunnlagPrStatus bgPrStatus = builder.build();
        return BeregningsgrunnlagPeriode.builder().medPeriode(periode).medBeregningsgrunnlagPrStatus(bgPrStatus).build();
    }

    private BeregningsresultatRegelmodellMellomregning settOppGraderingScenario(int redBrukersAndelPrÅr, int redRefusjonPrÅr, BigDecimal stillingsgrad,
                                                                                int nyArbeidstidProsent, long dagsatsBruker, Long dagsatsArbeidsgiver, boolean erGradering) {
        arbeidsforhold = Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(orgnr);
        Inntektskategori inntektskategori = Inntektskategori.ARBEIDSTAKER;
        prArbeidsforhold = BeregningsgrunnlagPrArbeidsforhold.builder().medArbeidsforhold(arbeidsforhold)
            .medRedusertBrukersAndelPrÅr(BigDecimal.valueOf(redBrukersAndelPrÅr))
            .medRedusertRefusjonPrÅr(BigDecimal.valueOf(redRefusjonPrÅr))
            .medDagsatsBruker(dagsatsBruker)
            .medDagsatsArbeidsgiver(dagsatsArbeidsgiver)
            .medInntektskategori(inntektskategori)
            .build();
        BigDecimal utbetalingsgrad = BigDecimal.valueOf(100).subtract(BigDecimal.valueOf(nyArbeidstidProsent));
        return lagMellomregning(AktivitetStatus.ATFL, inntektskategori, UttakArbeidType.ARBEIDSTAKER,
            stillingsgrad, utbetalingsgrad,
            BigDecimal.valueOf(redBrukersAndelPrÅr), erGradering);
    }

    private static long getDagsats(int årsbeløp) {
        return Math.round(årsbeløp / 260.0);
    }

    private static long getDagsats(double årsbeløp) {
        return Math.round(årsbeløp / 260.0);
    }

    private BeregningsresultatRegelmodellMellomregning settOppGraderingScenarioForAndreStatuser(BigDecimal redusertBrukersAndel, BigDecimal stillingsgrad, int utbetalingsgrad,
                                                                                                AktivitetStatus aktivitetStatus, Inntektskategori inntektskategori, UttakArbeidType uttakArbeidType,
                                                                                                boolean erGradering) {
        return lagMellomregning(aktivitetStatus, inntektskategori, uttakArbeidType, stillingsgrad, BigDecimal.valueOf(utbetalingsgrad), redusertBrukersAndel, erGradering);
    }

    private BeregningsresultatRegelmodellMellomregning settOppScenarioMedOppholdsperiodeForSN(BigDecimal redusertBrukersAndel) {
        return lagMellomregningForOppholdsPeriode(AktivitetStatus.SN, Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE, redusertBrukersAndel);
    }

    private BeregningsresultatRegelmodellMellomregning settOppScenarioMedOppholdsperiodeForAT(int redBrukersAndelPrÅr, int redRefusjonPrÅr, long dagsatsBruker, Long dagsatsArbeidsgiver) {
        arbeidsforhold = Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(orgnr);
        Inntektskategori inntektskategori = Inntektskategori.ARBEIDSTAKER;
        prArbeidsforhold = BeregningsgrunnlagPrArbeidsforhold.builder().medArbeidsforhold(arbeidsforhold)
            .medRedusertBrukersAndelPrÅr(BigDecimal.valueOf(redBrukersAndelPrÅr))
            .medRedusertRefusjonPrÅr(BigDecimal.valueOf(redRefusjonPrÅr))
            .medDagsatsBruker(dagsatsBruker)
            .medDagsatsArbeidsgiver(dagsatsArbeidsgiver)
            .medInntektskategori(inntektskategori)
            .build();
        return lagMellomregningForOppholdsPeriode(AktivitetStatus.ATFL, inntektskategori, BigDecimal.valueOf(redBrukersAndelPrÅr));
    }

}
