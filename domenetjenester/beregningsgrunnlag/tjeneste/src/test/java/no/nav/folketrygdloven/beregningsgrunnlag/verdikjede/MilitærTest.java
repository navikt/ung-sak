package no.nav.folketrygdloven.beregningsgrunnlag.verdikjede;

import static no.nav.folketrygdloven.beregningsgrunnlag.verdikjede.BeregningsgrunnlagGrunnlagTestUtil.nyttGrunnlag;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.input.K9BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagTilstand;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Hjemmel;
import no.nav.folketrygdloven.beregningsgrunnlag.opptjening.OpptjeningAktiviteter;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningAksjonspunktResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningsgrunnlagRegelResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningIAYTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;

public class MilitærTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT_BEREGNING = VerdikjedeTestHjelper.SKJÆRINGSTIDSPUNKT_OPPTJENING;

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private RepositoryProvider repositoryProvider = new RepositoryProvider(repoRule.getEntityManager());

    private BehandlingReferanse behandlingReferanse;

    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private VerdikjedeTestHjelper verdikjedeTestHjelper = new VerdikjedeTestHjelper();

    private BeregningTjenesteWrapper beregningTjenesteWrapper;

    @Before
    public void setup() {

        beregningTjenesteWrapper = BeregningTjenesteProvider.provide(repoRule, iayTjeneste);

        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = scenario.lagre(repositoryProvider);
    }

    @Test
    public void militærSettesTil3G() {
        // Arrange
        Periode opptjeningPeriode = Periode.of(SKJÆRINGSTIDSPUNKT_BEREGNING.minusYears(1), SKJÆRINGSTIDSPUNKT_BEREGNING.minusDays(1));
        var opptjeningAktiviteter = OpptjeningAktiviteter.fra(OpptjeningAktivitetType.MILITÆR_ELLER_SIVILTJENESTE, opptjeningPeriode);
        BehandlingReferanse ref = lagReferanse(behandlingReferanse);

        byggMilitærForBehandling(behandlingReferanse, beregningTjenesteWrapper.getBeregningIAYTestUtil(), SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(10),
            SKJÆRINGSTIDSPUNKT_BEREGNING.minusDays(1));
        List<Inntektsmelding> inntektsmeldinger = List.of();
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt().medInntektsmeldinger(inntektsmeldinger).build();
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(lagReferanse(behandlingReferanse), iayGrunnlag, opptjeningAktiviteter, AktivitetGradering.INGEN_GRADERING, foreldrepengerGrunnlag);

        // Act 1: Fastsett beregningaktiviteter og kontroller fakta beregning
        BeregningsgrunnlagGrunnlagEntitet grunnlag = kjørStegOgLagreGrunnlag(input);
        input = input.medBeregningsgrunnlagGrunnlag(grunnlag);
        List<BeregningAksjonspunktResultat> aksjonspunktResultat = beregningTjenesteWrapper.getAksjonspunktUtlederFaktaOmBeregning().utledAksjonspunkterFor(input,
            grunnlag, false);

        // Assert 1
        assertThat(aksjonspunktResultat).isEmpty();

        // Act 2: foreslå beregningsgrunnlag
        BeregningsgrunnlagRegelResultat resultat = beregningTjenesteWrapper.getForeslåBeregningsgrunnlagTjeneste().foreslåBeregningsgrunnlag(input, grunnlag);

        // Assert 2
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(resultat, Hjemmel.F_14_7);

        BeregningsgrunnlagEntitet foreslåttBeregningsgrunnlag = resultat.getBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periode = foreslåttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 1);

        // Act 3: fordel beregningsgrunnlag
        resultat = beregningTjenesteWrapper.getVurderBeregningsgrunnlagTjeneste().vurderBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, foreslåttBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));
        assertThat(resultat.getVilkårOppfylt()).isTrue();
        assertThat(resultat.getAksjonspunkter()).isEmpty();
        BeregningsgrunnlagEntitet fordeltBeregningsgrunnlag = fordelBeregningsgrunnlag(ref, grunnlag, resultat);

        // Act 4: fastsette beregningsgrunnlag
        var fastsattBeregningsgrunnlag = beregningTjenesteWrapper.getFullføreBeregningsgrunnlagTjeneste().fullføreBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, fordeltBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));

        // Assert 3
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(fastsattBeregningsgrunnlag, Hjemmel.F_14_7);

        periode = fastsattBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        BigDecimal treG = BigDecimal.valueOf(3 * 93634);
        assertThat(periode.getBeregnetPrÅr()).isEqualByComparingTo(treG);
        assertThat(periode.getRedusertPrÅr()).isEqualByComparingTo(treG);
        assertThat(periode.getDagsats()).isEqualTo(1080);
    }

    private BeregningsgrunnlagGrunnlagEntitet kjørStegOgLagreGrunnlag(BeregningsgrunnlagInput input) {
        return verdikjedeTestHjelper.kjørStegOgLagreGrunnlag(input, beregningTjenesteWrapper);
    }

    private BeregningsgrunnlagEntitet fordelBeregningsgrunnlag(BehandlingReferanse ref, BeregningsgrunnlagGrunnlagEntitet grunnlag,
                                                               BeregningsgrunnlagRegelResultat resultat) {
        var iayGrunnlag = iayTjeneste.hentGrunnlag(ref.getBehandlingId());
        var input = new BeregningsgrunnlagInput(ref, iayGrunnlag, null, AktivitetGradering.INGEN_GRADERING, null)
                .medBeregningsgrunnlagGrunnlag(grunnlag);
        return beregningTjenesteWrapper.getFordelBeregningsgrunnlagTjeneste().fordelBeregningsgrunnlag(input, resultat.getBeregningsgrunnlag());
    }

    private BehandlingReferanse lagReferanse(BehandlingReferanse behandlingReferanse) {
        return behandlingReferanse.medSkjæringstidspunkt(
            Skjæringstidspunkt.builder()
                .medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_BEREGNING)
                .medSkjæringstidspunktBeregning(SKJÆRINGSTIDSPUNKT_BEREGNING)
                .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT_BEREGNING)
                .medFørsteUttaksdato(SKJÆRINGSTIDSPUNKT_BEREGNING.minusDays(10))
                .build());
    }

    private void byggMilitærForBehandling(BehandlingReferanse behandlingReferanse, BeregningIAYTestUtil iayTestUtil, LocalDate fom, LocalDate tom) {
        InntektArbeidYtelseTjeneste iayTjeneste = beregningTjenesteWrapper.getIayTjeneste();
        InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder = iayTjeneste.opprettBuilderForRegister(behandlingReferanse.getId());
        iayTjeneste.lagreIayAggregat(behandlingReferanse.getId(), inntektArbeidYtelseAggregatBuilder);
        iayTestUtil.lagAnnenAktivitetOppgittOpptjening(behandlingReferanse, ArbeidType.MILITÆR_ELLER_SIVILTJENESTE, fom, tom);
    }

}
