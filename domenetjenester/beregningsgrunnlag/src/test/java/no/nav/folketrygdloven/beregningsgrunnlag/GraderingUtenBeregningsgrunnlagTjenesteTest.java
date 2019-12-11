package no.nav.folketrygdloven.beregningsgrunnlag;

import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AndelGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.AbstractTestScenario;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.vedtak.util.FPDateUtil;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class GraderingUtenBeregningsgrunnlagTjenesteTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = FPDateUtil.iDag();
    private static final String ORGNR = "915933149";

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);

    private RepositoryProvider repositoryProvider = new RepositoryProvider(repoRule.getEntityManager());
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag();

    private BehandlingReferanse lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider, iayTjeneste::lagreIayAggregat, iayTjeneste::lagreOppgittOpptjening);
    }

    @Test
    public void skalIkkeFåAksjonspunkterArbeidstakerMedBG() {
        // Arrange
        var scenario = TestScenarioBuilder.nyttScenario();
        @SuppressWarnings("unused")
        BehandlingReferanse behandlingReferanse = lagre(scenario);
        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null);
        lagBeregningsgrunnlagAndel(beregningsgrunnlagPeriode, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, BigDecimal.TEN);


        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(AktivitetGradering.INGEN_GRADERING);

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isFalse();
    }

    @Test
    public void skalIkkeFåAksjonspunkterArbeidstakerUtenGradering() {
        // Arrange
        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null);
        lagBeregningsgrunnlagAndel(beregningsgrunnlagPeriode, AktivitetStatus.ARBEIDSTAKER, BigDecimal.ZERO);


        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(AktivitetGradering.INGEN_GRADERING);

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isFalse();
    }

    @Test
    public void skalFåAksjonspunkterArbeidstakerNårGraderingOgHarIkkeBG() {
        // Arrange
        LocalDate skjæringstidspunkt = SKJÆRINGSTIDSPUNKT;
        LocalDate graderingFom = skjæringstidspunkt;
        LocalDate graderingTom = skjæringstidspunkt.plusMonths(4);

        AktivitetGradering aktivitetGradering = new AktivitetGradering(AndelGradering.builder()
            .medArbeidsgiver(arbeidsgiver)
            .medStatus(AktivitetStatus.ARBEIDSTAKER)
            .medGradering(graderingFom, graderingTom, 50)
            .build());

        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(skjæringstidspunkt, null);
        lagBeregningsgrunnlagAndel(beregningsgrunnlagPeriode, AktivitetStatus.ARBEIDSTAKER, BigDecimal.ZERO);

        // Act

        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(aktivitetGradering);

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isTrue();
    }

    @Test
    public void skalFåAksjonspunkterSelvstendigNårGraderingOgHarIkkeBG() {
        // Arrange
        LocalDate skjæringstidspunkt = SKJÆRINGSTIDSPUNKT;
        LocalDate graderingFom = skjæringstidspunkt;
        LocalDate graderingTom = skjæringstidspunkt.plusMonths(4);

        AktivitetGradering aktivitetGradering = new AktivitetGradering(AndelGradering.builder()
            .medStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medGradering(graderingFom, graderingTom, 50)
            .build());

        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null);
        lagBeregningsgrunnlagAndel(beregningsgrunnlagPeriode, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, BigDecimal.ZERO);

        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(aktivitetGradering);

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isTrue();
    }

    @Test
    public void skalIkkeFåAksjonspunkterSelvstendigNårGraderingOgHarBG() {
        // Arrange
        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null);
        lagBeregningsgrunnlagAndel(beregningsgrunnlagPeriode, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, BigDecimal.TEN);


        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(AktivitetGradering.INGEN_GRADERING);

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isFalse();
    }

    @Test
    public void skalIkkeFåAksjonspunkterSelvstendigNårIkkeGradering() {
        // Arrange
        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null);
        lagBeregningsgrunnlagAndel(beregningsgrunnlagPeriode, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, BigDecimal.ZERO);


        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(AktivitetGradering.INGEN_GRADERING);

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isFalse();
    }

    @Test
    public void skalFåAksjonspunkterFrilanserNårGraderingOgHarIkkeBG() {
        // Arrange
        LocalDate skjæringstidspunkt = SKJÆRINGSTIDSPUNKT;
        LocalDate graderingFom = skjæringstidspunkt;
        LocalDate graderingTom = skjæringstidspunkt.plusMonths(6);

        AktivitetGradering aktivitetGradering = new AktivitetGradering(AndelGradering.builder()
            .medStatus(AktivitetStatus.FRILANSER)
            .medGradering(graderingFom, graderingTom, 50)
            .build());

        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null);
        lagBeregningsgrunnlagAndel(beregningsgrunnlagPeriode, AktivitetStatus.FRILANSER, BigDecimal.ZERO);

        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(aktivitetGradering);

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isTrue();
    }

    @Test
    public void skalIkkeFåAksjonspunkterFrilanserNårGraderingOgHarBG() {
        // Arrange
        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null);
        lagBeregningsgrunnlagAndel(beregningsgrunnlagPeriode, AktivitetStatus.FRILANSER, BigDecimal.TEN);


        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(AktivitetGradering.INGEN_GRADERING);

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isFalse();
    }

    @Test
    public void skalIkkeFåAksjonspunkterFrilanserNårIkkeGradering() {
        // Arrange
        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null);
        lagBeregningsgrunnlagAndel(beregningsgrunnlagPeriode, AktivitetStatus.FRILANSER, BigDecimal.ZERO);


        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(AktivitetGradering.INGEN_GRADERING);

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isFalse();
    }

    @Test
    public void skalIkkeFåAksjonspunkterFrilanserNårGraderingUtenforPeriodeUtenBeregningsgrunnlag() {
        // Arrange
        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(3));
        lagBeregningsgrunnlagAndel(beregningsgrunnlagPeriode, AktivitetStatus.FRILANSER, BigDecimal.ZERO);


        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(AktivitetGradering.INGEN_GRADERING);

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isFalse();
    }

    @Test
    public void skalIkkeFåAksjonspunkterSNNårGraderingUtenforPeriodeUtenBeregningsgrunnlag() {
        // Arrange
        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT.plusMonths(2), null);
        lagBeregningsgrunnlagAndel(beregningsgrunnlagPeriode, AktivitetStatus.FRILANSER, BigDecimal.ZERO);


        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(AktivitetGradering.INGEN_GRADERING);

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isFalse();
    }

    @Test
    public void skalIkkeFåAksjonspunkterArbeidstakerNårGraderingUtenforPeriodeUtenBeregningsgrunnlag2() {
        // Arrange
        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(3));
        lagBeregningsgrunnlagAndel(beregningsgrunnlagPeriode, AktivitetStatus.ARBEIDSTAKER, BigDecimal.ZERO);


        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(AktivitetGradering.INGEN_GRADERING);

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isFalse();
    }

    @Test
    public void skalIkkeFåAksjonspunkterNårAAP() {
        // Arrange
        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null);
        lagBeregningsgrunnlagAndel(beregningsgrunnlagPeriode, AktivitetStatus.ARBEIDSAVKLARINGSPENGER, BigDecimal.ZERO);

        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(AktivitetGradering.INGEN_GRADERING);

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isFalse();
    }

    @Test
    public void skal_ikke_finne_andel_når_det_er_sn_med_gradering_med_inntekt_på_grunnlag() {
        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null);
        lagBeregningsgrunnlagAndel(beregningsgrunnlagPeriode, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, BigDecimal.TEN);


        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(AktivitetGradering.INGEN_GRADERING);
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = finnAndelerMedGraderingUtenBG(beregningsgrunnlag, AktivitetGradering.INGEN_GRADERING);

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isFalse();
        assertThat(andeler).isEmpty();
    }

    @Test
    public void skal_ikke_finne_andel_når_det_er_gradering_men_ikke_fastsatt_redusert_pr_år() {
        // Arrange
        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null);
        lagBeregningsgrunnlagAndel(beregningsgrunnlagPeriode, AktivitetStatus.ARBEIDSTAKER, null);


        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(AktivitetGradering.INGEN_GRADERING);
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = finnAndelerMedGraderingUtenBG(beregningsgrunnlag, AktivitetGradering.INGEN_GRADERING);

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isFalse();
        assertThat(andeler).isEmpty();
    }

    @Test
    public void skal_ikke_finne_andel_når_det_er_gradering_men_fastsatt_grunnlag_over_0_redusert_pr_år() {
        // Arrange
        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null);
        lagBeregningsgrunnlagAndel(beregningsgrunnlagPeriode, AktivitetStatus.ARBEIDSTAKER, BigDecimal.TEN);


        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(AktivitetGradering.INGEN_GRADERING);
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = finnAndelerMedGraderingUtenBG(beregningsgrunnlag, AktivitetGradering.INGEN_GRADERING);

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isFalse();
        assertThat(andeler).isEmpty();
    }

    @Test
    public void skal_finne_andel_når_det_er_gradering() {
        // Arrange
        LocalDate skjæringstidspunkt = SKJÆRINGSTIDSPUNKT;
        LocalDate graderingFom = skjæringstidspunkt;
        LocalDate graderingTom = skjæringstidspunkt.plusMonths(4);

        AktivitetGradering aktivitetGradering = new AktivitetGradering(AndelGradering.builder()
            .medArbeidsgiver(arbeidsgiver)
            .medStatus(AktivitetStatus.ARBEIDSTAKER)
            .medGradering(graderingFom, graderingTom, 50)
            .build());

        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null);
        lagBeregningsgrunnlagAndel(beregningsgrunnlagPeriode, AktivitetStatus.ARBEIDSTAKER, BigDecimal.ZERO);

        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(aktivitetGradering);
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = finnAndelerMedGraderingUtenBG(beregningsgrunnlag, aktivitetGradering);

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isTrue();
        assertThat(andeler).hasSize(1);
    }

    @Test
    public void skal_finne_andel_når_det_er_sn_med_gradering_uten_inntekt_på_grunnlag() {
        // Arrange
        LocalDate skjæringstidspunkt = SKJÆRINGSTIDSPUNKT;
        LocalDate graderingFom = skjæringstidspunkt;
        LocalDate graderingTom = skjæringstidspunkt.plusMonths(6);

        AktivitetGradering aktivitetGradering = new AktivitetGradering(AndelGradering.builder()
            .medStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medGradering(graderingFom, graderingTom, 50)
            .build());

        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(skjæringstidspunkt, null);
        lagBeregningsgrunnlagAndel(beregningsgrunnlagPeriode, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, BigDecimal.ZERO);

        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(aktivitetGradering);
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = finnAndelerMedGraderingUtenBG(beregningsgrunnlag, aktivitetGradering);

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isTrue();
        assertThat(andeler).hasSize(1);
        assertThat(andeler.get(0).getAktivitetStatus()).isEqualTo(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
    }

    @Test
    public void skal_finne_riktig_andel_når_det_er_flere_med_gradering_men_kun_en_mangler_inntekt() {
        // Arrange
        LocalDate skjæringstidspunkt = SKJÆRINGSTIDSPUNKT;
        LocalDate graderingFom = skjæringstidspunkt;
        LocalDate graderingTom = skjæringstidspunkt.plusMonths(4);

        AktivitetGradering aktivitetGradering = new AktivitetGradering(AndelGradering.builder()
            .medStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medGradering(graderingFom, graderingTom, 50)
            .build());

        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null);
        lagBeregningsgrunnlagAndel(beregningsgrunnlagPeriode, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, BigDecimal.ZERO);

        // Arrange AT andel
        lagBeregningsgrunnlagAndel(beregningsgrunnlagPeriode, AktivitetStatus.ARBEIDSTAKER, BigDecimal.TEN);


        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(aktivitetGradering);
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = finnAndelerMedGraderingUtenBG(beregningsgrunnlag, aktivitetGradering);

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isTrue();
        assertThat(andeler).hasSize(1);
        assertThat(andeler.get(0).getAktivitetStatus()).isEqualTo(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
    }

    @Test
    public void skal_gi_false_når_to_andeler_i_graderingsperiode_men_ikke_0_på_andel_som_skal_graderes() {
        // Arrange
        LocalDate skjæringstidspunkt = SKJÆRINGSTIDSPUNKT;
        LocalDate graderingFom = skjæringstidspunkt;
        LocalDate graderingTom = skjæringstidspunkt.plusMonths(4);

        AktivitetGradering aktivitetGradering = new AktivitetGradering(AndelGradering.builder()
            .medStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medGradering(graderingFom, graderingTom, 50)
            .build());

        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null);
        lagBeregningsgrunnlagAndel(beregningsgrunnlagPeriode, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, BigDecimal.TEN);

        // Arrange AT andel
        lagBeregningsgrunnlagAndel(beregningsgrunnlagPeriode, AktivitetStatus.FRILANSER, BigDecimal.ZERO);


        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(aktivitetGradering);

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isFalse();
    }


    private List<BeregningsgrunnlagPrStatusOgAndel> finnAndelerMedGraderingUtenBG(BeregningsgrunnlagEntitet beregningsgrunnlag, AktivitetGradering aktivitetGradering) {
        return GraderingUtenBeregningsgrunnlagTjeneste.finnAndelerMedGraderingUtenBG(beregningsgrunnlag, aktivitetGradering);
    }

    private boolean harAndelerMedGraderingUtenGrunnlag(AktivitetGradering aktivitetGradering) {
        return GraderingUtenBeregningsgrunnlagTjeneste.harAndelerMedGraderingUtenGrunnlag(beregningsgrunnlag, aktivitetGradering);
    }

    private BeregningsgrunnlagPeriode lagBeregningsgrunnlagPeriode(LocalDate periodeFom, LocalDate periodeTom) {
        return BeregningsgrunnlagPeriode.builder().medBeregningsgrunnlagPeriode(periodeFom, periodeTom).build(beregningsgrunnlag);
    }

    private void lagBeregningsgrunnlagAndel(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode, AktivitetStatus aktivitetStatus, BigDecimal redusertPrÅr) {

        BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(aktivitetStatus).build(beregningsgrunnlag);
        BGAndelArbeidsforhold.Builder bgAndelBuilder = BGAndelArbeidsforhold.builder().medArbeidsgiver(arbeidsgiver);
        BeregningsgrunnlagPrStatusOgAndel.Builder andelBuilder = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(aktivitetStatus)
            .medRedusertPrÅr(redusertPrÅr);

        if (aktivitetStatus.erArbeidstaker()) {
            andelBuilder.medBGAndelArbeidsforhold(bgAndelBuilder);
        }

        andelBuilder.build(beregningsgrunnlagPeriode);
    }

    private BeregningsgrunnlagEntitet lagBeregningsgrunnlag() {
        return BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .build();
    }

}
