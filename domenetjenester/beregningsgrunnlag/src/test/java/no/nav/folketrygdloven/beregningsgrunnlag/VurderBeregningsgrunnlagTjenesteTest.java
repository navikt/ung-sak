package no.nav.folketrygdloven.beregningsgrunnlag;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.finn.unleash.FakeUnleash;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.MapBeregningsgrunnlagFraRegelTilVL;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.MapBeregningsgrunnlagFraVLTilRegel;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.input.K9BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Sammenligningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningsgrunnlagRegelResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.beregningsgrunnlag.BeregningAktivitetTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.verdikjede.LagMapBeregningsgrunnlagFraVLTilRegel;
import no.nav.folketrygdloven.beregningsgrunnlag.verdikjede.VerdikjedeTestHjelper;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;

public class VurderBeregningsgrunnlagTjenesteTest {

    private static final double MÅNEDSINNTEKT1 = 12345d;
    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, Month.MAY, 10);
    private static final LocalDate SKJÆRINGSTIDSPUNKT_BEREGNING = SKJÆRINGSTIDSPUNKT_OPPTJENING;

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();
    private RepositoryProvider repositoryProvider = new RepositoryProvider(repoRule.getEntityManager());

    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private VerdikjedeTestHjelper verdikjedeTestHjelper = new VerdikjedeTestHjelper();

    private VurderBeregningsgrunnlagTjeneste tjeneste;

    private Collection<Inntektsmelding> inntektsmeldinger = List.of();

    @Before
    public void setup() {
        MapBeregningsgrunnlagFraVLTilRegel oversetterTilRegel = LagMapBeregningsgrunnlagFraVLTilRegel.lagMapper(
            repositoryProvider.getBeregningsgrunnlagRepository(), new FakeUnleash());
        MapBeregningsgrunnlagFraRegelTilVL oversetterFraRegel = new MapBeregningsgrunnlagFraRegelTilVL();

        tjeneste = new VurderBeregningsgrunnlagTjeneste(oversetterFraRegel, oversetterTilRegel);
    }

    @Test
    public void testVilkårsvurderingArbeidstakerMedBGOverHalvG() {
        // Arrange
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        BeregningAktivitetAggregatEntitet beregningAktiviteter = BeregningAktivitetTestUtil.opprettBeregningAktiviteter(SKJÆRINGSTIDSPUNKT_OPPTJENING,
            OpptjeningAktivitetType.ARBEID);
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag(400_000);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medRegisterAktiviteter(beregningAktiviteter)
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .build(1L, BeregningsgrunnlagTilstand.FORESLÅTT);
        verdikjedeTestHjelper.lagBehandlingForSN(scenario, BigDecimal.valueOf(12 * MÅNEDSINNTEKT1), 2015);
        BehandlingReferanse behandlingReferanse = scenario.lagre(repositoryProvider, iayTjeneste::lagreIayAggregat, iayTjeneste::lagreOppgittOpptjening);
        BehandlingReferanse ref = lagReferanseMedSkjæringstidspunkt(behandlingReferanse);
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt().medInntektsmeldinger(inntektsmeldinger).build();
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(ref, iayGrunnlag, null, AktivitetGradering.INGEN_GRADERING, foreldrepengerGrunnlag);

        // Act
        BeregningsgrunnlagRegelResultat resultat = tjeneste.vurderBeregningsgrunnlag(input, grunnlag);

        // Assert
        assertThat(resultat.getBeregningsgrunnlag()).isNotNull();
        assertThat(resultat.getAksjonspunkter()).isEmpty();
        assertThat(resultat.getVilkårOppfylt()).isTrue();
        assertThat(resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder()).hasSize(1);
        BeregningsgrunnlagPeriode periode = resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder().get(0);
        assertThat(periode.getRegelInputVilkårvurdering()).isNotEmpty();
        assertThat(periode.getRegelEvalueringVilkårvurdering()).isNotEmpty();
    }

    @Test
    public void testVilkårsvurderingArbeidstakerMedBGUnderHalvG() {
        // Arrange
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        BeregningAktivitetAggregatEntitet beregningAktiviteter = BeregningAktivitetTestUtil.opprettBeregningAktiviteter(SKJÆRINGSTIDSPUNKT_OPPTJENING,
            OpptjeningAktivitetType.ARBEID);
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag(40_000);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medRegisterAktiviteter(beregningAktiviteter)
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .build(1L, BeregningsgrunnlagTilstand.FORESLÅTT);
        verdikjedeTestHjelper.lagBehandlingForSN(scenario, BigDecimal.valueOf(12 * MÅNEDSINNTEKT1), 2015);
        BehandlingReferanse behandlingReferanse = scenario.lagre(repositoryProvider, iayTjeneste::lagreIayAggregat, iayTjeneste::lagreOppgittOpptjening);

        BehandlingReferanse ref = lagReferanseMedSkjæringstidspunkt(behandlingReferanse);
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt().medInntektsmeldinger(inntektsmeldinger).build();
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(ref, iayGrunnlag, null, AktivitetGradering.INGEN_GRADERING, foreldrepengerGrunnlag);

        // Act
        BeregningsgrunnlagRegelResultat resultat = tjeneste.vurderBeregningsgrunnlag(input, grunnlag);

        // Assert
        assertThat(resultat.getBeregningsgrunnlag()).isNotNull();
        assertThat(resultat.getAksjonspunkter()).isEmpty();
        assertThat(resultat.getVilkårOppfylt()).isFalse();
        assertThat(resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder()).hasSize(1);
        BeregningsgrunnlagPeriode periode = resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder().get(0);
        assertThat(periode.getRegelInputVilkårvurdering()).isNotEmpty();
        assertThat(periode.getRegelEvalueringVilkårvurdering()).isNotEmpty();
    }

    private BeregningsgrunnlagEntitet lagBeregningsgrunnlag(int inntekt) {
        BeregningsgrunnlagEntitet bg = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_BEREGNING)
            .medGrunnbeløp(BigDecimal.valueOf(600_000))
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .build();
        BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT_BEREGNING, null)
            .build(bg);
        Sammenligningsgrunnlag.builder()
            .medSammenligningsperiode(SKJÆRINGSTIDSPUNKT_BEREGNING, SKJÆRINGSTIDSPUNKT_BEREGNING)
            .medRapportertPrÅr(BigDecimal.ZERO)
            .medAvvikPromille(0L)
            .build(bg);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder()
                .medArbeidsperiodeFom(SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(12))
                .medArbeidsperiodeTom(SKJÆRINGSTIDSPUNKT_BEREGNING)
                .medArbeidsgiver(Arbeidsgiver.virksomhet("1234"))
                .medRefusjonskravPrÅr(BigDecimal.valueOf(inntekt)))
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medBeregningsperiode(SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(3).withDayOfMonth(1), SKJÆRINGSTIDSPUNKT_BEREGNING.withDayOfMonth(1).minusDays(1))
            .medBeregnetPrÅr(BigDecimal.valueOf(inntekt))
            .build(periode);
        return bg;
    }

    private static BehandlingReferanse lagReferanseMedSkjæringstidspunkt(BehandlingReferanse behandlingReferanse) {
        return behandlingReferanse.medSkjæringstidspunkt(Skjæringstidspunkt.builder()
            .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING)
            .medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_BEREGNING)
            .build());
    }

}
