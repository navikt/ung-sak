package no.nav.folketrygdloven.beregningsgrunnlag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

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
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagTilstand;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningsgrunnlagRegelResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.beregningsgrunnlag.BeregningAktivitetTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.verdikjede.LagMapBeregningsgrunnlagFraVLTilRegel;
import no.nav.folketrygdloven.beregningsgrunnlag.verdikjede.VerdikjedeTestHjelper;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;

public class ForeslåBeregningsgrunnlagSNTest {

    private static final double MÅNEDSINNTEKT1 = 12345d;

    private static final double BEREGNINGSGRUNNLAG = 148989.08d;

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, Month.MAY, 10);

    private static final LocalDate SKJÆRINGSTIDSPUNKT_BEREGNING = SKJÆRINGSTIDSPUNKT_OPPTJENING;
    private static final BigDecimal GRUNNBELØP = BigDecimal.valueOf(90000);
    private FakeUnleash unleash = new FakeUnleash();

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();
    private RepositoryProvider repositoryProvider = new RepositoryProvider(repoRule.getEntityManager());

    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private VerdikjedeTestHjelper verdikjedeTestHjelper = new VerdikjedeTestHjelper();

    private ForeslåBeregningsgrunnlag tjeneste;

    @Before
    public void setup() {
        MapBeregningsgrunnlagFraVLTilRegel oversetterTilRegel = LagMapBeregningsgrunnlagFraVLTilRegel.lagMapper(
            repositoryProvider.getBeregningsgrunnlagRepository(), new FakeUnleash());
        MapBeregningsgrunnlagFraRegelTilVL oversetterFraRegel = new MapBeregningsgrunnlagFraRegelTilVL();
        AksjonspunktUtlederForeslåBeregning aksjonspunktUtleder = new AksjonspunktUtlederForeslåBeregning();

        tjeneste = new ForeslåBeregningsgrunnlag(oversetterTilRegel, oversetterFraRegel, aksjonspunktUtleder, unleash);
    }

    @Test
    public void testBeregningsgrunnlagSelvstendigNæringsdrivende() {
        // Arrange
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        BeregningAktivitetAggregatEntitet beregningAktiviteter = BeregningAktivitetTestUtil.opprettBeregningAktiviteter(SKJÆRINGSTIDSPUNKT_OPPTJENING,
            OpptjeningAktivitetType.NÆRING);
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag(scenario);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medRegisterAktiviteter(beregningAktiviteter)
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .build(1L, BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
        verdikjedeTestHjelper.lagBehandlingForSN(scenario, BigDecimal.valueOf(12 * MÅNEDSINNTEKT1), 2014);
        BehandlingReferanse behandlingReferanse = scenario.lagre(repositoryProvider, iayTjeneste::lagreIayAggregat, iayTjeneste::lagreOppgittOpptjening);
        Collection<Inntektsmelding> inntektsmeldinger = List.of();
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(Optional.of(iayTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId()))).medInntektsmeldinger(inntektsmeldinger).build();
        BehandlingReferanse ref = lagReferanseMedStp(behandlingReferanse);
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(ref, iayGrunnlag, null, AktivitetGradering.INGEN_GRADERING, foreldrepengerGrunnlag)
            .medBeregningsgrunnlagGrunnlag(grunnlag);

        // Act
        BeregningsgrunnlagRegelResultat resultat = tjeneste.foreslåBeregningsgrunnlag(input, grunnlag);

        // Assert
        assertThat(resultat.getBeregningsgrunnlag()).isNotNull();
        assertThat(resultat.getAksjonspunkter()).isEmpty();
        assertThat(resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder()).hasSize(1);
        BeregningsgrunnlagPeriode periode = resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder().get(0);
        verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 1);
        verifiserBGSN(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(0));
    }

    private BeregningsgrunnlagEntitet lagBeregningsgrunnlag(TestScenarioBuilder scenario) {
        BeregningsgrunnlagEntitet.Builder beregningsgrunnlagBuilder = scenario.medBeregningsgrunnlag();
        beregningsgrunnlagBuilder.medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_BEREGNING)
            .medGrunnbeløp(GRUNNBELØP);
        beregningsgrunnlagBuilder.leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder()
            .medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE));
        beregningsgrunnlagBuilder.leggTilBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT_BEREGNING, null)
            .leggTilBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPrStatusOgAndel.builder()
                .medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)));
        return beregningsgrunnlagBuilder.build();
    }

    private void verifiserPeriode(BeregningsgrunnlagPeriode periode, LocalDate fom, LocalDate tom, int antallAndeler) {
        assertThat(periode.getBeregningsgrunnlagPeriodeFom()).isEqualTo(fom);
        assertThat(periode.getBeregningsgrunnlagPeriodeTom()).isEqualTo(tom);
        assertThat(periode.getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(antallAndeler);
        assertThat(periode.getBruttoPrÅr().doubleValue()).isCloseTo(BEREGNINGSGRUNNLAG, within(0.01));
        assertThat(periode.getRedusertPrÅr()).isNull();
        assertThat(periode.getAvkortetPrÅr()).isNull();
    }

    private void verifiserBGSN(BeregningsgrunnlagPrStatusOgAndel bgpsa) {
        assertThat(bgpsa.getAktivitetStatus()).isEqualTo(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
        assertThat(bgpsa.getBeregningsperiodeFom()).isEqualTo(LocalDate.of(2014, Month.JANUARY, 1));
        assertThat(bgpsa.getBeregningsperiodeTom()).isEqualTo(LocalDate.of(2016, Month.DECEMBER, 31));
        assertThat(bgpsa.getBgAndelArbeidsforhold()).isEmpty();
        assertThat(bgpsa.getArbeidsforholdType()).isEqualTo(OpptjeningAktivitetType.UDEFINERT);
        assertThat(bgpsa.getBeregnetPrÅr().doubleValue()).isCloseTo(BEREGNINGSGRUNNLAG, within(0.01));
        assertThat(bgpsa.getBruttoPrÅr().doubleValue()).isCloseTo(BEREGNINGSGRUNNLAG, within(0.01));
        assertThat(bgpsa.getOverstyrtPrÅr()).isNull();
        assertThat(bgpsa.getRedusertPrÅr()).isNull();
        assertThat(bgpsa.getAvkortetPrÅr()).isNull();
    }

    private static BehandlingReferanse lagReferanseMedStp(BehandlingReferanse behandlingReferanse) {
        return behandlingReferanse.medSkjæringstidspunkt(Skjæringstidspunkt.builder()
            .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING)
            .medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_BEREGNING)
            .build());
    }
}
