package no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.ytelse.sykepenger;

import static no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.BeregningsgrunnlagScenario.opprettBeregningsgrunnlagFraInntektsmelding;
import static no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.BeregningsgrunnlagScenario.opprettSammenligningsgrunnlag;
import static no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.VerifiserBeregningsgrunnlag.verifiserBeregningsgrunnlagBeregnet;
import static no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.VerifiserBeregningsgrunnlag.verifiserBeregningsgrunnlagBruttoPrPeriodeType;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import no.finn.unleash.FakeUnleash;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.RegelmodellOversetter;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.foreslå.RegelForeslåBeregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.BeregningsgrunnlagHjemmel;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.RegelMerknad;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.RegelResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.summary.EvaluationSerializer;

public class ForeslåBeregningsgrunnlagSykepengerTest {

    private LocalDate skjæringstidspunkt;
    private FakeUnleash unleash = new FakeUnleash();

    @Before
    public void setup() {
        skjæringstidspunkt = LocalDate.of(2018, Month.JANUARY, 15);
    }

    @Test
    public void skalBeregneSykepengerGrunnlagMedInntektsmeldingMedNaturalYtelserIArbeidsgiverperioden() {
        // Arrange
        BigDecimal månedsinntekt = BigDecimal.valueOf(40000);
        BigDecimal refusjonskrav = BigDecimal.valueOf(10000);
        BigDecimal naturalytelse = BigDecimal.valueOf(2000);
        LocalDate naturalytelseOpphørFom = skjæringstidspunkt.plusDays(7);
        List<Periode> arbeidsgiversPeriode = List.of(Periode.of(skjæringstidspunkt, skjæringstidspunkt.plusDays(14)));
        Beregningsgrunnlag beregningsgrunnlag = opprettBeregningsgrunnlagFraInntektsmelding(skjæringstidspunkt, månedsinntekt,
            refusjonskrav, naturalytelse, naturalytelseOpphørFom);
        Beregningsgrunnlag.builder(beregningsgrunnlag).medBeregningForSykepenger(true);
        opprettSammenligningsgrunnlag(beregningsgrunnlag.getInntektsgrunnlag(), skjæringstidspunkt, BigDecimal.valueOf(42000));
        BeregningsgrunnlagPeriode grunnlag = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        BeregningsgrunnlagPrArbeidsforhold bgArbeidsforhold = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL).getArbeidsforhold().get(0);
        BeregningsgrunnlagPrArbeidsforhold.builder(bgArbeidsforhold).medArbeidsgiverperioder(arbeidsgiversPeriode).build();
        // Act
        Evaluation evaluation = new RegelForeslåBeregningsgrunnlag(grunnlag, unleash).evaluer(grunnlag);
        // Assert
        @SuppressWarnings("unused")
        String sporing = EvaluationSerializer.asJson(evaluation);
        RegelResultat resultat = RegelmodellOversetter.getRegelResultat(evaluation, "input");

        assertThat(resultat.getMerknader().stream().map(RegelMerknad::getMerknadKode).collect(Collectors.toList())).isEmpty();
        assertThat(bgArbeidsforhold.getNaturalytelseBortfaltPrÅr().get()).isEqualByComparingTo(BigDecimal.valueOf(24000)); //NOSONAR
        assertBeregningsgrunnlag(grunnlag, månedsinntekt, 24000);
    }

    @Test
    public void skalBeregneSykepengerGrunnlagMedInntektsmeldingMedNaturalYtelserUtenforArbeidsgiverperioden() {
        // Arrange
        BigDecimal månedsinntekt = BigDecimal.valueOf(40000);
        BigDecimal refusjonskrav = BigDecimal.valueOf(10000);
        BigDecimal naturalytelse = BigDecimal.valueOf(2000);
        LocalDate naturalytelseOpphørFom = skjæringstidspunkt.plusDays(7);
        List<Periode> arbeidsgiversPeriode = List.of(Periode.of(skjæringstidspunkt, skjæringstidspunkt.plusDays(6)),
            Periode.of(skjæringstidspunkt.plusDays(10), skjæringstidspunkt.plusDays(18)));
        Beregningsgrunnlag beregningsgrunnlag = opprettBeregningsgrunnlagFraInntektsmelding(skjæringstidspunkt, månedsinntekt,
            refusjonskrav, naturalytelse, naturalytelseOpphørFom);
        Beregningsgrunnlag.builder(beregningsgrunnlag).medBeregningForSykepenger(true);
        opprettSammenligningsgrunnlag(beregningsgrunnlag.getInntektsgrunnlag(), skjæringstidspunkt, BigDecimal.valueOf(40000));
        BeregningsgrunnlagPeriode grunnlag = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        BeregningsgrunnlagPrArbeidsforhold bgArbeidsforhold = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL).getArbeidsforhold().get(0);
        BeregningsgrunnlagPrArbeidsforhold.builder(bgArbeidsforhold).medArbeidsgiverperioder(arbeidsgiversPeriode).build();
        // Act
        Evaluation evaluation = new RegelForeslåBeregningsgrunnlag(grunnlag, unleash).evaluer(grunnlag);
        // Assert
        @SuppressWarnings("unused")
        String sporing = EvaluationSerializer.asJson(evaluation);
        RegelResultat resultat = RegelmodellOversetter.getRegelResultat(evaluation, "input");

        assertThat(resultat.getMerknader().stream().map(RegelMerknad::getMerknadKode).collect(Collectors.toList())).isEmpty();
        assertThat(bgArbeidsforhold.getNaturalytelseBortfaltPrÅr()).isEmpty();
        assertBeregningsgrunnlag(grunnlag, månedsinntekt, 0);
    }

    @Test
    public void skalIkkeBeregneSykepengerGrunnlagMedInntektsmeldingMedNaturalYtelserIArbeidsgiverperiodenNårIFPSAK() {
        // Arrange
        BigDecimal månedsinntekt = BigDecimal.valueOf(40000);
        BigDecimal refusjonskrav = BigDecimal.valueOf(10000);
        BigDecimal naturalytelse = BigDecimal.valueOf(2000);
        LocalDate naturalytelseOpphørFom = skjæringstidspunkt.plusDays(7);
        List<Periode> arbeidsgiversPeriode = List.of(Periode.of(skjæringstidspunkt, skjæringstidspunkt.plusDays(14)));
        Beregningsgrunnlag beregningsgrunnlag = opprettBeregningsgrunnlagFraInntektsmelding(skjæringstidspunkt, månedsinntekt,
            refusjonskrav, naturalytelse, naturalytelseOpphørFom);
        opprettSammenligningsgrunnlag(beregningsgrunnlag.getInntektsgrunnlag(), skjæringstidspunkt, BigDecimal.valueOf(40000));
        BeregningsgrunnlagPeriode grunnlag = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        BeregningsgrunnlagPrArbeidsforhold bgArbeidsforhold = grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL).getArbeidsforhold().get(0);
        BeregningsgrunnlagPrArbeidsforhold.builder(bgArbeidsforhold).medArbeidsgiverperioder(arbeidsgiversPeriode).build();
        // Act
        new RegelForeslåBeregningsgrunnlag(grunnlag, unleash).evaluer(grunnlag);
        // Assert
        assertThat(bgArbeidsforhold.getNaturalytelseBortfaltPrÅr()).isEmpty();
        assertBeregningsgrunnlag(grunnlag, månedsinntekt, 0);
    }

    private void assertBeregningsgrunnlag(BeregningsgrunnlagPeriode grunnlag, BigDecimal månedsinntekt, int naturalYtelsePrÅr) {
        assertThat(grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL).samletNaturalytelseBortfaltMinusTilkommetPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(naturalYtelsePrÅr));
        assertThat(grunnlag.getSammenligningsGrunnlag().getAvvikPromille()).isEqualTo(0);
        verifiserBeregningsgrunnlagBruttoPrPeriodeType(grunnlag, BeregningsgrunnlagHjemmel.HJEMMEL_BARE_ARBEIDSTAKER, AktivitetStatus.ATFL, 12 * månedsinntekt.doubleValue());
        verifiserBeregningsgrunnlagBeregnet(grunnlag, 12 * månedsinntekt.doubleValue());
    }
}

