package no.nav.folketrygdloven.beregningsgrunnlag.verdikjede;

import static no.nav.folketrygdloven.beregningsgrunnlag.verdikjede.BeregningsgrunnlagGrunnlagTestUtil.nyttGrunnlag;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.folketrygdloven.beregningsgrunnlag.GrunnbeløpTestKonstanter;
import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.input.K9BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Sammenligningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.opptjening.OpptjeningAktiviteter;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningAksjonspunktResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningsgrunnlagRegelResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.AbstractTestScenario;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.kodeverk.beregningsgrunnlag.Hjemmel;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;

public class SelvstendigNæringsdrivendeTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = VerdikjedeTestHjelper.SKJÆRINGSTIDSPUNKT_OPPTJENING;
    private static final LocalDate SKJÆRINGSTIDSPUNKT_BEREGNING = SKJÆRINGSTIDSPUNKT_OPPTJENING;
    private static final String ORGNR1 = "974761076";

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private RepositoryProvider repositoryProvider = Mockito.spy(new RepositoryProvider(repoRule.getEntityManager()));

    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private VerdikjedeTestHjelper verdikjedeTestHjelper = new VerdikjedeTestHjelper();

    private BeregningTjenesteWrapper beregningTjenesteWrapper;

    private TestScenarioBuilder scenario;

    @Before
    public void setup() {
        beregningTjenesteWrapper = BeregningTjenesteProvider.provide(repoRule, iayTjeneste);

        scenario = TestScenarioBuilder.nyttScenario();
    }

    @Test
    public void skalBeregneAvvikVedVarigEndring() {
        // Arrange
        //6Gsnitt<PGI<12Gsnitt: Bidrag til beregningsgrunnlaget = 6 + (PGI-6*Gsnitt)/3*Gsnitt
        final List<Double> ÅRSINNTEKT = List.of(7.0 * GrunnbeløpTestKonstanter.GSNITT_2014, 8.0 * GrunnbeløpTestKonstanter.GSNITT_2015, 9.0 * GrunnbeløpTestKonstanter.GSNITT_2016);

        final double varigEndringMånedsinntekt = 8.0 * GrunnbeløpTestKonstanter.GRUNNBELØP_2017 / 12;
        //Gjennomsnittlig PGI = SUM(Bidrag til beregningsgrunnlaget)/3 * G
        final double forventetBrutto = 624226.66666;
        final Long forventetAvvikPromille = 200L;
        final double forventetAvkortet = 6.0 * GrunnbeløpTestKonstanter.GRUNNBELØP_2017;
        final double forventetRedusert = forventetAvkortet;

        List<BigDecimal> årsinntekterSN = ÅRSINNTEKT.stream().map(BigDecimal::valueOf).collect(Collectors.toList());

        // Arrange
        verdikjedeTestHjelper.lagBehandlingATogFLogSN(scenario,
            List.of(), List.of(), null, årsinntekterSN, 2014, BigDecimal.valueOf(12 * varigEndringMånedsinntekt));
        BehandlingReferanse behandlingReferanse = lagre(scenario);
        BehandlingReferanse ref = lagReferanse(behandlingReferanse);

        var opptjeningPeriode = Periode.of(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1));
        var opptjeningAktiviteter = OpptjeningAktiviteter.fraOrgnr(OpptjeningAktivitetType.NÆRING, opptjeningPeriode, ORGNR1);
        List<Inntektsmelding> inntektsmeldinger = List.of();

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId())).medInntektsmeldinger(inntektsmeldinger).build();
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(ref, iayGrunnlag, opptjeningAktiviteter, AktivitetGradering.INGEN_GRADERING, foreldrepengerGrunnlag);

        // Act 1: kontroller fakta om beregning
        BeregningsgrunnlagGrunnlagEntitet grunnlag = kjørStegOgLagreGrunnlag(input);
        input = input.medBeregningsgrunnlagGrunnlag(grunnlag);

        List<BeregningAksjonspunktResultat> aksjonspunktResultat = beregningTjenesteWrapper.getAksjonspunktUtlederFaktaOmBeregning().utledAksjonspunkterFor(input, grunnlag, false);

        // Assert 1
        assertThat(aksjonspunktResultat).isEmpty();

        // Act 2: foreslå beregningsgrunnlag
        BeregningsgrunnlagRegelResultat resultat = beregningTjenesteWrapper.getForeslåBeregningsgrunnlagTjeneste().foreslåBeregningsgrunnlag(input, grunnlag);

        // Assert 2
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(resultat, Hjemmel.F_14_7_8_35);

        BeregningsgrunnlagEntitet foreslåttBeregningsgrunnlag = resultat.getBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periode = foreslåttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 1);
        verdikjedeTestHjelper.verifiserBGSNførAvkorting(periode, forventetBrutto, forventetBrutto, 2016);
        Sammenligningsgrunnlag sg = foreslåttBeregningsgrunnlag.getSammenligningsgrunnlag();
        assertThat(sg).isNotNull();
        assertThat(sg.getAvvikPromille()).isEqualTo(forventetAvvikPromille);

        // Act 3: fordel beregningsgrunnlag
        resultat = beregningTjenesteWrapper.getVurderBeregningsgrunnlagTjeneste().vurderBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, foreslåttBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));
        assertThat(resultat.getVilkårOppfylt()).isTrue();
        BeregningsgrunnlagEntitet fordeltBeregningsgrunnlag = fordelBeregningsgrunnlag(input, resultat);

        // Act 4: fastsette beregningsgrunnlag
        var fastsattBeregningsgrunnlag = beregningTjenesteWrapper.getFullføreBeregningsgrunnlagTjeneste().fullføreBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, fordeltBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));

        // Assert 3
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(fastsattBeregningsgrunnlag, Hjemmel.F_14_7_8_35);

        periode = fastsattBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserBGSNetterAvkorting(periode, forventetBrutto, forventetBrutto, forventetAvkortet, forventetRedusert, 2016);
    }

    @Test
    public void sammeLønnHvertÅrUnder6GIkkeVarigEndring() {

        //PGI <= 6xGsnitt: Bidrag til beregningsgrunnlaget = PGI/Gsnitt
        final double årsinntekt1 = 4.0 * GrunnbeløpTestKonstanter.GSNITT_2014;
        final double årsinntekt2 = 4.0 * GrunnbeløpTestKonstanter.GSNITT_2015;
        final double årsinntekt3 = 4.0 * GrunnbeløpTestKonstanter.GSNITT_2016;
        final List<Double> ÅRSINNTEKT = List.of(årsinntekt1, årsinntekt2, årsinntekt3);

        //Gjennomsnittlig PGI = SUM(Bidrag til beregningsgrunnlaget)/3 * G
        final double forventetBrutto = 4.0 * GrunnbeløpTestKonstanter.GRUNNBELØP_2017;
        final double forventetAvkortet = forventetBrutto;
        final double forventetRedusert = forventetAvkortet;
        List<BigDecimal> årsinntekterSN = ÅRSINNTEKT.stream().map(BigDecimal::valueOf).collect(Collectors.toList());

        // Arrange
        verdikjedeTestHjelper.lagBehandlingATogFLogSN(scenario, List.of(), List.of(), null, årsinntekterSN, 2014, null);
        BehandlingReferanse behandlingReferanse = lagre(scenario);
        BehandlingReferanse ref = lagReferanse(behandlingReferanse);

        var opptjeningPeriode = Periode.of(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1));
        var opptjeningAktiviteter = OpptjeningAktiviteter.fraOrgnr(OpptjeningAktivitetType.NÆRING, opptjeningPeriode, ORGNR1);

        List<Inntektsmelding> inntektsmeldinger = List.of();

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId())).medInntektsmeldinger(inntektsmeldinger).build();
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(ref, iayGrunnlag, opptjeningAktiviteter, AktivitetGradering.INGEN_GRADERING, foreldrepengerGrunnlag);

        // Act 1: kontroller fakta om beregning
        BeregningsgrunnlagGrunnlagEntitet grunnlag = kjørStegOgLagreGrunnlag(input);
        input = input.medBeregningsgrunnlagGrunnlag(grunnlag);

        List<BeregningAksjonspunktResultat> aksjonspunktResultat = beregningTjenesteWrapper.getAksjonspunktUtlederFaktaOmBeregning().utledAksjonspunkterFor(input, grunnlag, false);

        // Assert 1
        assertThat(aksjonspunktResultat).isEmpty();

        // Act 2: foreslå beregningsgrunnlag
        BeregningsgrunnlagRegelResultat resultat = beregningTjenesteWrapper.getForeslåBeregningsgrunnlagTjeneste().foreslåBeregningsgrunnlag(input, grunnlag);

        // Assert 2
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(resultat, Hjemmel.F_14_7_8_35);

        BeregningsgrunnlagEntitet foreslåttBeregningsgrunnlag = resultat.getBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periode = foreslåttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 1);
        verdikjedeTestHjelper.verifiserBGSNførAvkorting(periode, forventetBrutto, forventetBrutto, 2016);
        assertThat(foreslåttBeregningsgrunnlag.getSammenligningsgrunnlag()).isNull();

        // Act 3: fordel beregningsgrunnlag
        resultat = beregningTjenesteWrapper.getVurderBeregningsgrunnlagTjeneste().vurderBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, foreslåttBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));
        assertThat(resultat.getVilkårOppfylt()).isTrue();
        BeregningsgrunnlagEntitet fordeltBeregningsgrunnlag = fordelBeregningsgrunnlag(input, resultat);

        // Act 4: fastsette beregningsgrunnlag
        var fastsattBeregningsgrunnlag = beregningTjenesteWrapper.getFullføreBeregningsgrunnlagTjeneste().fullføreBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, fordeltBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));

        // Assert 3
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(fastsattBeregningsgrunnlag, Hjemmel.F_14_7_8_35);

        periode = fastsattBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserBGSNetterAvkorting(periode, forventetBrutto, forventetBrutto, forventetAvkortet, forventetRedusert, 2016);
    }

    @Test
    public void sammeLønnHvertÅrOver6GIkkeVarigEndring() {

        //6Gsnitt<PGI<12Gsnitt: Bidrag til beregningsgrunnlaget = 6 + (PGI-6*Gsnitt)/3*Gsnitt
        final double årsinntekt1 = 7.0 * GrunnbeløpTestKonstanter.GSNITT_2014;
        final double årsinntekt2 = 7.0 * GrunnbeløpTestKonstanter.GSNITT_2015;
        final double årsinntekt3 = 7.0 * GrunnbeløpTestKonstanter.GSNITT_2016;
        final List<Double> ÅRSINNTEKT = List.of(årsinntekt1, årsinntekt2, årsinntekt3);

        //Gjennomsnittlig PGI = SUM(Bidrag til beregningsgrunnlaget)/3 * G
        final double forventetBrutto = 6.333333 * GrunnbeløpTestKonstanter.GRUNNBELØP_2017;
        final double forventetAvkortet = 6.0 * GrunnbeløpTestKonstanter.GRUNNBELØP_2017;
        final double forventetRedusert = forventetAvkortet;

        List<BigDecimal> årsinntekterSN = ÅRSINNTEKT.stream().map(BigDecimal::valueOf).collect(Collectors.toList());

        // Arrange
        verdikjedeTestHjelper.lagBehandlingATogFLogSN(scenario, List.of(), List.of(), null, årsinntekterSN, 2014, null);
        BehandlingReferanse behandlingReferanse = lagre(scenario);
        BehandlingReferanse ref = lagReferanse(behandlingReferanse);

        var opptjeningPeriode = Periode.of(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1));
        var opptjeningAktiviteter = OpptjeningAktiviteter.fraOrgnr(OpptjeningAktivitetType.NÆRING, opptjeningPeriode, ORGNR1);
        List<Inntektsmelding> inntektsmeldinger = List.of();

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId())).medInntektsmeldinger(inntektsmeldinger).build();
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(ref, iayGrunnlag, opptjeningAktiviteter, AktivitetGradering.INGEN_GRADERING, foreldrepengerGrunnlag);

        // Act 1: kontroller fakta om beregning
        BeregningsgrunnlagGrunnlagEntitet grunnlag = kjørStegOgLagreGrunnlag(input);
        input = input.medBeregningsgrunnlagGrunnlag(grunnlag);

        List<BeregningAksjonspunktResultat> aksjonspunktResultat = beregningTjenesteWrapper.getAksjonspunktUtlederFaktaOmBeregning().utledAksjonspunkterFor(input, grunnlag, false);

        // Assert 1
        assertThat(aksjonspunktResultat).isEmpty();

        // Act 2: foreslå beregningsgrunnlag
        BeregningsgrunnlagRegelResultat resultat = beregningTjenesteWrapper.getForeslåBeregningsgrunnlagTjeneste().foreslåBeregningsgrunnlag(input, grunnlag);

        // Assert 2
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(resultat, Hjemmel.F_14_7_8_35);

        BeregningsgrunnlagEntitet foreslåttBeregningsgrunnlag = resultat.getBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periode = foreslåttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 1);
        verdikjedeTestHjelper.verifiserBGSNførAvkorting(periode, forventetBrutto, forventetBrutto, 2016);
        assertThat(foreslåttBeregningsgrunnlag.getSammenligningsgrunnlag()).isNull();

        // Act 3: fordel beregningsgrunnlag
        resultat = beregningTjenesteWrapper.getVurderBeregningsgrunnlagTjeneste().vurderBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, foreslåttBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));
        assertThat(resultat.getVilkårOppfylt()).isTrue();
        BeregningsgrunnlagEntitet fordeltBeregningsgrunnlag = fordelBeregningsgrunnlag(input, resultat);

        // Act 4: fastsette beregningsgrunnlag
        var fastsattBeregningsgrunnlag = beregningTjenesteWrapper.getFullføreBeregningsgrunnlagTjeneste().fullføreBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, fordeltBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));

        // Assert 3
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(fastsattBeregningsgrunnlag, Hjemmel.F_14_7_8_35);

        periode = fastsattBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserBGSNetterAvkorting(periode, forventetBrutto, forventetBrutto, forventetAvkortet, forventetRedusert, 2016);
    }

    @Test
    public void sammeLønnHvertÅrOver6GIkkeVarigEndringReduksjon() {

        //6Gsnitt<PGI<12Gsnitt: Bidrag til beregningsgrunnlaget = 6 + (PGI-6*Gsnitt)/3*Gsnitt
        final double årsinntekt1 = 7.0 * GrunnbeløpTestKonstanter.GSNITT_2014;
        final double årsinntekt2 = 7.0 * GrunnbeløpTestKonstanter.GSNITT_2015;
        final double årsinntekt3 = 7.0 * GrunnbeløpTestKonstanter.GSNITT_2016;
        final List<Double> ÅRSINNTEKT = List.of(årsinntekt1, årsinntekt2, årsinntekt3);

        //Gjennomsnittlig PGI = SUM(Bidrag til beregningsgrunnlaget)/3 * G
        final double forventetBrutto = 6.333333 * GrunnbeløpTestKonstanter.GRUNNBELØP_2017;
        final double forventetAvkortet = 6.0 * GrunnbeløpTestKonstanter.GRUNNBELØP_2017;
        final double forventetRedusert = forventetAvkortet;

        List<BigDecimal> årsinntekterSN = ÅRSINNTEKT.stream().map(BigDecimal::valueOf).collect(Collectors.toList());

        // Arrange
        verdikjedeTestHjelper.lagBehandlingATogFLogSN(scenario, List.of(), List.of(), null, årsinntekterSN, 2014, null);
        BehandlingReferanse behandlingReferanse = lagre(scenario);
        BehandlingReferanse ref = lagReferanse(behandlingReferanse);

        var opptjeningPeriode = Periode.of(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1));
        var opptjeningAktiviteter = OpptjeningAktiviteter.fraOrgnr(OpptjeningAktivitetType.NÆRING, opptjeningPeriode, ORGNR1);
        List<Inntektsmelding> inntektsmeldinger = List.of();

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId())).medInntektsmeldinger(inntektsmeldinger).build();
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(ref, iayGrunnlag, opptjeningAktiviteter, AktivitetGradering.INGEN_GRADERING, foreldrepengerGrunnlag);

        // Act 1: kontroller fakta om beregning
        BeregningsgrunnlagGrunnlagEntitet grunnlag = kjørStegOgLagreGrunnlag(input);
        input = input.medBeregningsgrunnlagGrunnlag(grunnlag);

        List<BeregningAksjonspunktResultat> aksjonspunktResultat = beregningTjenesteWrapper.getAksjonspunktUtlederFaktaOmBeregning().utledAksjonspunkterFor(input, grunnlag, false);

        // Assert 1
        assertThat(aksjonspunktResultat).isEmpty();

        // Act 2: foreslå beregningsgrunnlag
        BeregningsgrunnlagRegelResultat resultat = beregningTjenesteWrapper.getForeslåBeregningsgrunnlagTjeneste().foreslåBeregningsgrunnlag(input, grunnlag);

        // Assert 2
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(resultat, Hjemmel.F_14_7_8_35);

        BeregningsgrunnlagEntitet foreslåttBeregningsgrunnlag = resultat.getBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periode = foreslåttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 1);
        verdikjedeTestHjelper.verifiserBGSNførAvkorting(periode, forventetBrutto, forventetBrutto, 2016);
        assertThat(foreslåttBeregningsgrunnlag.getSammenligningsgrunnlag()).isNull();

        // Act 3: fordel beregningsgrunnlag
        resultat = beregningTjenesteWrapper.getVurderBeregningsgrunnlagTjeneste().vurderBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, foreslåttBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));
        assertThat(resultat.getVilkårOppfylt()).isTrue();
        BeregningsgrunnlagEntitet fordeltBeregningsgrunnlag = fordelBeregningsgrunnlag(input, resultat);

        // Act 4: fastsette beregningsgrunnlag
        var fastsattBeregningsgrunnlag = beregningTjenesteWrapper.getFullføreBeregningsgrunnlagTjeneste().fullføreBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, fordeltBeregningsgrunnlag, BeregningsgrunnlagTilstand.FASTSATT));

        // Assert 3
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(fastsattBeregningsgrunnlag, Hjemmel.F_14_7_8_35);

        periode = fastsattBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserBGSNetterAvkorting(periode, forventetBrutto, forventetBrutto, forventetAvkortet, forventetRedusert, 2016);
    }

    private BeregningsgrunnlagGrunnlagEntitet kjørStegOgLagreGrunnlag(BeregningsgrunnlagInput input) {
        return verdikjedeTestHjelper.kjørStegOgLagreGrunnlag(input, beregningTjenesteWrapper);
    }

    private BeregningsgrunnlagEntitet fordelBeregningsgrunnlag(BeregningsgrunnlagInput input, BeregningsgrunnlagRegelResultat resultat) {
        return beregningTjenesteWrapper.getFordelBeregningsgrunnlagTjeneste().fordelBeregningsgrunnlag(input, resultat.getBeregningsgrunnlag());
    }

    private BehandlingReferanse lagReferanse(BehandlingReferanse behandlingReferanse) {
        return behandlingReferanse.medSkjæringstidspunkt(
            Skjæringstidspunkt.builder()
                .medSkjæringstidspunktBeregning(SKJÆRINGSTIDSPUNKT_BEREGNING)
                .medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_BEREGNING)
                .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING)
                .medFørsteUttaksdato(SKJÆRINGSTIDSPUNKT_OPPTJENING.plusDays(1))
                .build());
    }

    private BehandlingReferanse lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider, iayTjeneste::lagreIayAggregat, iayTjeneste::lagreOppgittOpptjening);
    }
}
