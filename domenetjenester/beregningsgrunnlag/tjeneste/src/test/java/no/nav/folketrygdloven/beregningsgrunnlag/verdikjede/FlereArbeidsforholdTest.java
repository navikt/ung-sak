package no.nav.folketrygdloven.beregningsgrunnlag.verdikjede;

import static no.nav.folketrygdloven.beregningsgrunnlag.verdikjede.BeregningsgrunnlagGrunnlagTestUtil.nyttGrunnlag;
import static no.nav.folketrygdloven.beregningsgrunnlag.verdikjede.VerdikjedeTestHjelper.SKJÆRINGSTIDSPUNKT_OPPTJENING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.input.K9BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagTilstand;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Hjemmel;
import no.nav.folketrygdloven.beregningsgrunnlag.opptjening.OpptjeningAktiviteter;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningAksjonspunktResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningsgrunnlagRegelResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.AbstractTestScenario;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class FlereArbeidsforholdTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT_BEREGNING = SKJÆRINGSTIDSPUNKT_OPPTJENING;
    private static final LocalDate MINUS_YEARS_1 = SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(1);
    private static final String ARBEIDSFORHOLD_ORGNR1 = "890412882";
    private static final String ARBEIDSFORHOLD_ORGNR2 = "915933149";
    private static final String ARBEIDSFORHOLD_ORGNR3 = "923609016";
    private static final String ARBEIDSFORHOLD_ORGNR4 = "973152351";
    private static double seksG;

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private RepositoryProvider repositoryProvider = Mockito.spy(new RepositoryProvider(repoRule.getEntityManager()));

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository = repositoryProvider.getBeregningsgrunnlagRepository();

    private String beregningVirksomhet1 = ARBEIDSFORHOLD_ORGNR1;
    private String beregningVirksomhet2 = ARBEIDSFORHOLD_ORGNR2;
    private String beregningVirksomhet3 = ARBEIDSFORHOLD_ORGNR3;
    private String beregningVirksomhet4 = ARBEIDSFORHOLD_ORGNR4;
    private TestScenarioBuilder scenario;

    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private InntektsmeldingTjeneste inntektsmeldingTjeneste = new InntektsmeldingTjeneste(iayTjeneste);

    private VerdikjedeTestHjelper verdikjedeTestHjelper = new VerdikjedeTestHjelper(inntektsmeldingTjeneste);
    private BeregningTjenesteWrapper beregningTjenesteWrapper;

    @Before
    public void setup() {
        beregningTjenesteWrapper = BeregningTjenesteProvider.provide(repoRule, iayTjeneste);

        scenario = TestScenarioBuilder.nyttScenario();

        seksG = beregningsgrunnlagRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, SKJÆRINGSTIDSPUNKT_OPPTJENING).getVerdi() * 6;
    }

    private BehandlingReferanse lagBehandlingAT(AbstractTestScenario<?> scenario,
                                       BigDecimal inntektSammenligningsgrunnlag,
                                       List<String> beregningVirksomhet) {
        LocalDate fraOgMed = MINUS_YEARS_1;
        LocalDate tilOgMed = fraOgMed.plusYears(4);

        var inntektArbeidYtelseBuilder = scenario.getInntektArbeidYtelseScenarioTestBuilder().getKladd();
        var aktørId = scenario.getSøkerAktørId();
        beregningVirksomhet
            .forEach(virksomhetOrgnr -> verdikjedeTestHjelper.lagAktørArbeid(inntektArbeidYtelseBuilder, aktørId, Arbeidsgiver.virksomhet(virksomhetOrgnr),
                fraOgMed, tilOgMed, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD));

        for (LocalDate dt = fraOgMed; dt.isBefore(tilOgMed); dt = dt.plusMonths(1)) {
            verdikjedeTestHjelper.lagInntektForSammenligning(inntektArbeidYtelseBuilder, aktørId, dt, dt.plusMonths(1), inntektSammenligningsgrunnlag,
                Arbeidsgiver.virksomhet(beregningVirksomhet.get(0)));
        }

        BehandlingReferanse behandlingReferanse = scenario.lagre(repositoryProvider, iayTjeneste::lagreIayAggregat, iayTjeneste::lagreOppgittOpptjening);
        return behandlingReferanse;
    }

    @Test
    public void ettArbeidsforholdMedAvrundetDagsats() {
        String orgnr1 = ARBEIDSFORHOLD_ORGNR1;

        final double DAGSATS = 1959.76;
        final List<Double> ÅRSINNTEKT = List.of(DAGSATS * 260);
        final Double bg = ÅRSINNTEKT.get(0);

        final double forventetAvkortet = ÅRSINNTEKT.get(0);
        final double forventetRedusert = forventetAvkortet;

        final long forventetDagsats = 1960;
        List<BigDecimal> månedsinntekter = ÅRSINNTEKT.stream().map((v) -> BigDecimal.valueOf(v / 12)).collect(Collectors.toList());
        List<String> virksomhetene = List.of(orgnr1);

        // Arrange 1
        BehandlingReferanse behandlingReferanse = lagBehandlingAT(scenario,
            BigDecimal.valueOf(ÅRSINNTEKT.get(0) / 12),
            virksomhetene);
        BehandlingReferanse ref = lagReferanse(behandlingReferanse);

        var im1 = verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, Arbeidsgiver.virksomhet(beregningVirksomhet1),
            månedsinntekter.get(0), månedsinntekter.get(0));
        List<Inntektsmelding> inntektsmeldinger = List.of(im1);

        var opptjeningAktiviteter = OpptjeningAktiviteter.fraOrgnr(OpptjeningAktivitetType.ARBEID,
            Periode.of(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1)), orgnr1);

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(behandlingReferanse.getId())).medInntektsmeldinger(inntektsmeldinger).build();
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(ref, iayGrunnlag, opptjeningAktiviteter, AktivitetGradering.INGEN_GRADERING, foreldrepengerGrunnlag);

        // Act 1: kontroller fakta for beregning
        BeregningsgrunnlagGrunnlagEntitet grunnlag = verdikjedeTestHjelper.kjørStegOgLagreGrunnlag(input, beregningTjenesteWrapper);
        input = input.medBeregningsgrunnlagGrunnlag(grunnlag);

        List<BeregningAksjonspunktResultat> aksjonspunktResultat = beregningTjenesteWrapper.getAksjonspunktUtlederFaktaOmBeregning().utledAksjonspunkterFor(input,
            grunnlag, false);
        assertThat(aksjonspunktResultat).isEmpty();

        // Act 2: foreslå beregningsgrunnlag
        BeregningsgrunnlagRegelResultat resultat = beregningTjenesteWrapper.getForeslåBeregningsgrunnlagTjeneste().foreslåBeregningsgrunnlag(input, grunnlag);

        // Assert 1
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(resultat, Hjemmel.F_14_7_8_30);

        BeregningsgrunnlagEntitet foreslåttBeregningsgrunnlag = resultat.getBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periode = foreslåttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 1);
        verdikjedeTestHjelper.verifiserBGATførAvkorting(periode, ÅRSINNTEKT, virksomhetene);
        verdikjedeTestHjelper.verifiserSammenligningsgrunnlag(foreslåttBeregningsgrunnlag.getSammenligningsgrunnlag(),
            bg, SKJÆRINGSTIDSPUNKT_BEREGNING.minusYears(1).withDayOfMonth(1),
            SKJÆRINGSTIDSPUNKT_BEREGNING.withDayOfMonth(1).minusDays(1), 0L);

        // Act 3: fordel beregningsgrunnlag
        resultat = beregningTjenesteWrapper.getVurderBeregningsgrunnlagTjeneste().vurderBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, foreslåttBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));
        assertThat(resultat.getVilkårOppfylt()).isTrue();
        BeregningsgrunnlagEntitet fordeltBeregningsgrunnlag = fordelBeregningsgrunnlag(input, grunnlag, resultat);

        // Act 4: fastsette beregningsgrunnlag
        var fastsattBeregningsgrunnlag = beregningTjenesteWrapper.getFullføreBeregningsgrunnlagTjeneste().fullføreBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, fordeltBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));

        // Assert 3
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(fastsattBeregningsgrunnlag, Hjemmel.F_14_7_8_30);

        periode = fastsattBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 1, forventetDagsats);
        verdikjedeTestHjelper.verifiserBGATetterAvkorting(periode,
            ÅRSINNTEKT, ÅRSINNTEKT, virksomhetene, List.of(forventetRedusert), ÅRSINNTEKT, List.of(forventetRedusert), List.of(0.0d), false);
    }

    @Test
    public void ettArbeidsforholdMedOverstyringUnder6G() {

        String orgnr1 = ARBEIDSFORHOLD_ORGNR1;
        final List<Double> ÅRSINNTEKT = List.of(180000d);
        final Double bg = ÅRSINNTEKT.get(0);
        final Double overstyrt = 200000d;

        final double forventetAvkortet1 = ÅRSINNTEKT.get(0);
        final double forventetRedusert1 = forventetAvkortet1;

        final long forventetDagsats = Math.round(overstyrt / 260);

        List<BigDecimal> månedsinntekter = ÅRSINNTEKT.stream().map((v) -> BigDecimal.valueOf(v / 12)).collect(Collectors.toList());
        List<String> virksomhetene = List.of(orgnr1);

        // Arrange 1
        BehandlingReferanse behandlingReferanse = lagBehandlingAT(scenario,
            BigDecimal.valueOf(ÅRSINNTEKT.get(0) / 12 / 2),
            virksomhetene);
        BehandlingReferanse ref = lagReferanse(behandlingReferanse);

        var im1 = verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, Arbeidsgiver.virksomhet(beregningVirksomhet1),
            månedsinntekter.get(0), månedsinntekter.get(0));
        List<Inntektsmelding> inntektsmeldinger = List.of(im1);

        var opptjeningAktiviteter = OpptjeningAktiviteter.fraOrgnr(OpptjeningAktivitetType.ARBEID,
            Periode.of(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1)), orgnr1);

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(behandlingReferanse.getId())).medInntektsmeldinger(inntektsmeldinger).build();
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(ref, iayGrunnlag, opptjeningAktiviteter, AktivitetGradering.INGEN_GRADERING, foreldrepengerGrunnlag);

        // Act 1: kontroller fakta for beregning
        BeregningsgrunnlagGrunnlagEntitet grunnlag = verdikjedeTestHjelper.kjørStegOgLagreGrunnlag(input, beregningTjenesteWrapper);
        input = input.medBeregningsgrunnlagGrunnlag(grunnlag);
        List<BeregningAksjonspunktResultat> aksjonspunktResultat = beregningTjenesteWrapper.getAksjonspunktUtlederFaktaOmBeregning().utledAksjonspunkterFor(input,
            grunnlag, false);

        // Assert 1
        assertThat(aksjonspunktResultat).isEmpty();

        // Act 2: foreslå beregningsgrunnlag
        BeregningsgrunnlagRegelResultat resultat = beregningTjenesteWrapper.getForeslåBeregningsgrunnlagTjeneste().foreslåBeregningsgrunnlag(input, grunnlag);

        // Assert 2
        verifiserBeregningsgrunnlagMedAksjonspunkt(resultat);

        BeregningsgrunnlagEntitet foreslåttBeregningsgrunnlag = resultat.getBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periode = foreslåttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 1);
        verdikjedeTestHjelper.verifiserBGATførAvkorting(periode, ÅRSINNTEKT, virksomhetene);
        verdikjedeTestHjelper.verifiserSammenligningsgrunnlag(foreslåttBeregningsgrunnlag.getSammenligningsgrunnlag(),
            bg / 2, SKJÆRINGSTIDSPUNKT_BEREGNING.minusYears(1).withDayOfMonth(1),
            SKJÆRINGSTIDSPUNKT_BEREGNING.withDayOfMonth(1).minusDays(1), 1000L);

        // Arrange 2: Overstyring
        periode = foreslåttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        BeregningsgrunnlagPrStatusOgAndel.builder(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(0))
            .medOverstyrtPrÅr(BigDecimal.valueOf(overstyrt))
            .build(periode);

        // Act 3: fordel beregningsgrunnlag
        resultat = beregningTjenesteWrapper.getVurderBeregningsgrunnlagTjeneste().vurderBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, foreslåttBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));
        assertThat(resultat.getVilkårOppfylt()).isTrue();
        BeregningsgrunnlagEntitet fordeltBeregningsgrunnlag = fordelBeregningsgrunnlag(input, grunnlag, resultat);

        // Act 4: fastsette beregningsgrunnlag
        var fastsattBeregningsgrunnlag = beregningTjenesteWrapper.getFullføreBeregningsgrunnlagTjeneste().fullføreBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, fordeltBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));

        // Assert 3
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(fastsattBeregningsgrunnlag, Hjemmel.F_14_7_8_30);

        periode = fastsattBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 1, forventetDagsats);
        verifiserBGATetterOverstyring(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(0),
            bg, beregningVirksomhet1, overstyrt, overstyrt, overstyrt, bg, forventetAvkortet1, overstyrt - forventetAvkortet1, forventetRedusert1,
            overstyrt - forventetRedusert1);
    }

    @Test
    public void ettArbeidsforholdMedOverstyringOver6G() {
        String orgnr1 = ARBEIDSFORHOLD_ORGNR1;

        final List<Double> ÅRSINNTEKT = List.of(480000d);
        final Double bg = ÅRSINNTEKT.get(0);
        final Double overstyrt = 700000d;

        final double forventetAvkortet1 = ÅRSINNTEKT.get(0);
        final double forventetRedusert1 = forventetAvkortet1;

        List<BigDecimal> månedsinntekter = ÅRSINNTEKT.stream().map((v) -> BigDecimal.valueOf(v / 12)).collect(Collectors.toList());
        List<String> virksomhetene = List.of(orgnr1);

        final long forventetDagsats = Math.round(seksG / 260);

        // Arrange 1
        BehandlingReferanse behandlingReferanse = lagBehandlingAT(scenario,
            BigDecimal.valueOf(ÅRSINNTEKT.get(0) / 12 / 2),
            virksomhetene);
        BehandlingReferanse ref = lagReferanse(behandlingReferanse);

        var im1 = verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, Arbeidsgiver.virksomhet(beregningVirksomhet1),
            månedsinntekter.get(0), månedsinntekter.get(0));
        List<Inntektsmelding> inntektsmeldinger = List.of(im1);

        var opptjeningAktiviteter = OpptjeningAktiviteter.fraOrgnr(OpptjeningAktivitetType.ARBEID,
            Periode.of(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1)), orgnr1);

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(behandlingReferanse.getId())).medInntektsmeldinger(inntektsmeldinger).build();
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(ref, iayGrunnlag, opptjeningAktiviteter, AktivitetGradering.INGEN_GRADERING, foreldrepengerGrunnlag);

        // Act 1: kontroller fakta for beregning
        BeregningsgrunnlagGrunnlagEntitet grunnlag = verdikjedeTestHjelper.kjørStegOgLagreGrunnlag(input, beregningTjenesteWrapper);
        input = input.medBeregningsgrunnlagGrunnlag(grunnlag);
        List<BeregningAksjonspunktResultat> aksjonspunktResultat = beregningTjenesteWrapper.getAksjonspunktUtlederFaktaOmBeregning().utledAksjonspunkterFor(input,
            grunnlag,
            false);

        // Assert 1
        assertThat(aksjonspunktResultat).isEmpty();

        // Act 2: foreslå beregningsgrunnlag
        BeregningsgrunnlagRegelResultat resultat = beregningTjenesteWrapper.getForeslåBeregningsgrunnlagTjeneste().foreslåBeregningsgrunnlag(input, grunnlag);

        // Assert 2
        verifiserBeregningsgrunnlagMedAksjonspunkt(resultat);

        BeregningsgrunnlagEntitet foreslåttBeregningsgrunnlag = resultat.getBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periode = foreslåttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 1);
        verdikjedeTestHjelper.verifiserBGATførAvkorting(periode, ÅRSINNTEKT, virksomhetene);
        verdikjedeTestHjelper.verifiserSammenligningsgrunnlag(foreslåttBeregningsgrunnlag.getSammenligningsgrunnlag(),
            bg / 2, SKJÆRINGSTIDSPUNKT_BEREGNING.minusYears(1).withDayOfMonth(1),
            SKJÆRINGSTIDSPUNKT_BEREGNING.withDayOfMonth(1).minusDays(1), 1000L);

        // Arrange 2: Overstyring
        periode = foreslåttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        BeregningsgrunnlagPrStatusOgAndel.builder(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(0))
            .medOverstyrtPrÅr(BigDecimal.valueOf(overstyrt))
            .build(periode);

        // Act 3: fordel beregningsgrunnlag
        resultat = beregningTjenesteWrapper.getVurderBeregningsgrunnlagTjeneste().vurderBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, foreslåttBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));
        assertThat(resultat.getVilkårOppfylt()).isTrue();
        BeregningsgrunnlagEntitet fordeltBeregningsgrunnlag = fordelBeregningsgrunnlag(input, grunnlag, resultat);

        // Act 4: fastsette beregningsgrunnlag
        var fastsattBeregningsgrunnlag = beregningTjenesteWrapper.getFullføreBeregningsgrunnlagTjeneste().fullføreBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, fordeltBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));

        // Assert 3
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(fastsattBeregningsgrunnlag, Hjemmel.F_14_7_8_30);

        periode = fastsattBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 1, forventetDagsats);
        verifiserBGATetterOverstyring(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(0),
            bg, beregningVirksomhet1, overstyrt, seksG, seksG, bg, forventetAvkortet1, seksG - forventetAvkortet1, forventetRedusert1,
            seksG - forventetRedusert1);
    }

    @Test
    public void ettArbeidsforholdMedOverstyringOver6GOgReduksjon() {
        String orgnr1 = ARBEIDSFORHOLD_ORGNR1;

        final List<Double> ÅRSINNTEKT = List.of(480000d);
        final Double bg = ÅRSINNTEKT.get(0);
        final double overstyrt = 700000d;

        final double forventetAvkortet = seksG;
        final double forventetRedusert = forventetAvkortet;
        final double forventetAvkortet1 = ÅRSINNTEKT.get(0);
        final double forventetRedusert1 = forventetAvkortet1;

        final long forventetDagsats = Math.round(forventetRedusert / 260);
        List<BigDecimal> månedsinntekter = ÅRSINNTEKT.stream().map((v) -> BigDecimal.valueOf(v / 12)).collect(Collectors.toList());
        List<String> virksomhetene = List.of(orgnr1);

        // Arrange 1
        BehandlingReferanse behandlingReferanse = lagBehandlingAT(scenario,
            BigDecimal.valueOf(ÅRSINNTEKT.get(0) / 12 / 2),
            virksomhetene);
        BehandlingReferanse ref = lagReferanse(behandlingReferanse);

        var im1 = verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, Arbeidsgiver.virksomhet(beregningVirksomhet1),
            månedsinntekter.get(0), månedsinntekter.get(0));
        List<Inntektsmelding> inntektsmeldinger = List.of(im1);

        var opptjeningAktiviteter = OpptjeningAktiviteter.fraOrgnr(OpptjeningAktivitetType.ARBEID,
            Periode.of(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1)), orgnr1);

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(behandlingReferanse.getId())).medInntektsmeldinger(inntektsmeldinger).build();
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(ref, iayGrunnlag, opptjeningAktiviteter, AktivitetGradering.INGEN_GRADERING, foreldrepengerGrunnlag);

        // Act 1: kontroller fakta for beregning
        var grunnlag = verdikjedeTestHjelper.kjørStegOgLagreGrunnlag(input, beregningTjenesteWrapper);
        input = input.medBeregningsgrunnlagGrunnlag(grunnlag);
        List<BeregningAksjonspunktResultat> aksjonspunktResultat = beregningTjenesteWrapper.getAksjonspunktUtlederFaktaOmBeregning().utledAksjonspunkterFor(input, grunnlag, false);

        // Assert 1
        assertThat(aksjonspunktResultat).isEmpty();

        // Act 2: foreslå beregningsgrunnlag
        BeregningsgrunnlagRegelResultat resultat = beregningTjenesteWrapper.getForeslåBeregningsgrunnlagTjeneste().foreslåBeregningsgrunnlag(input, grunnlag);

        // Assert 2
        verifiserBeregningsgrunnlagMedAksjonspunkt(resultat);

        BeregningsgrunnlagEntitet foreslåttBeregningsgrunnlag = resultat.getBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periode = foreslåttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 1);
        verdikjedeTestHjelper.verifiserBGATførAvkorting(periode, ÅRSINNTEKT, virksomhetene);
        verdikjedeTestHjelper.verifiserSammenligningsgrunnlag(foreslåttBeregningsgrunnlag.getSammenligningsgrunnlag(),
            bg / 2, SKJÆRINGSTIDSPUNKT_BEREGNING.minusYears(1).withDayOfMonth(1),
            SKJÆRINGSTIDSPUNKT_BEREGNING.withDayOfMonth(1).minusDays(1), 1000L);

        // Arrange 2: Overstyring
        periode = foreslåttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        BeregningsgrunnlagPrStatusOgAndel.builder(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(0))
            .medOverstyrtPrÅr(BigDecimal.valueOf(overstyrt))
            .build(periode);

        // Act 3: fordel beregningsgrunnlag
        resultat = beregningTjenesteWrapper.getVurderBeregningsgrunnlagTjeneste().vurderBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, foreslåttBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));
        assertThat(resultat.getVilkårOppfylt()).isTrue();
        BeregningsgrunnlagEntitet fordeltBeregningsgrunnlag = fordelBeregningsgrunnlag(input, grunnlag, resultat);

        // Act 4: fastsette beregningsgrunnlag
        var fastsattBeregningsgrunnlag = beregningTjenesteWrapper.getFullføreBeregningsgrunnlagTjeneste().fullføreBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, fordeltBeregningsgrunnlag, BeregningsgrunnlagTilstand.FASTSATT));

        // Assert 3
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(fastsattBeregningsgrunnlag, Hjemmel.F_14_7_8_30);

        periode = fastsattBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 1, forventetDagsats);
        verifiserBGATetterOverstyring(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(0),
            bg, beregningVirksomhet1, overstyrt, forventetAvkortet, forventetRedusert, bg,
            forventetAvkortet1, seksG - forventetAvkortet1,
            forventetRedusert1, seksG - forventetRedusert1);
    }

    @Test
    public void toArbeidsforholdMedBgUnder6gOgFullRefusjon() {
        String orgnr1 = ARBEIDSFORHOLD_ORGNR1;
        String orgnr2 = ARBEIDSFORHOLD_ORGNR2;

        final List<Double> ÅRSINNTEKT = List.of(180000d, 72000d);
        final Double totalÅrsinntekt = ÅRSINNTEKT.stream().reduce((v1, v2) -> v1 + v2).orElse(null);

        final double forventetRedusert1 = ÅRSINNTEKT.get(0);
        final double forventetRedusert2 = ÅRSINNTEKT.get(1);

        final List<Double> forventetRedusert = List.of(forventetRedusert1, forventetRedusert2);
        final List<Double> forventetRedusertBrukersAndel = List.of(0d, 0d);

        final long forventetDagsats = Math.round(forventetRedusert1 / 260) + Math.round(forventetRedusert2 / 260);

        List<BigDecimal> månedsinntekter = ÅRSINNTEKT.stream().map((v) -> BigDecimal.valueOf(v / 12)).collect(Collectors.toList());
        List<String> virksomhetene = List.of(orgnr1, orgnr2);

        // Arrange 1
        BehandlingReferanse behandlingReferanse = lagBehandlingAT(scenario,
            BigDecimal.valueOf(totalÅrsinntekt / 12),
            virksomhetene);
        BehandlingReferanse ref = lagReferanse(behandlingReferanse);

        var im1 = verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, Arbeidsgiver.virksomhet(beregningVirksomhet1),
            månedsinntekter.get(0), månedsinntekter.get(0));
        var im2 = verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, Arbeidsgiver.virksomhet(beregningVirksomhet2),
            månedsinntekter.get(1), månedsinntekter.get(1));
        List<Inntektsmelding> inntektsmeldinger = List.of(im1, im2);

        Periode opptjeningPeriode = Periode.of(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1));
        var opptjeningAktiviteter = new OpptjeningAktiviteter(
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, orgnr1),
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, orgnr2));

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(behandlingReferanse.getId())).medInntektsmeldinger(inntektsmeldinger).build();
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(ref, iayGrunnlag, opptjeningAktiviteter, AktivitetGradering.INGEN_GRADERING, foreldrepengerGrunnlag);

        // Act 1: kontroller fakta for beregning
        BeregningsgrunnlagGrunnlagEntitet grunnlag = verdikjedeTestHjelper.kjørStegOgLagreGrunnlag(input, beregningTjenesteWrapper);
        input = input.medBeregningsgrunnlagGrunnlag(grunnlag);
        List<BeregningAksjonspunktResultat> aksjonspunktResultat = beregningTjenesteWrapper.getAksjonspunktUtlederFaktaOmBeregning().utledAksjonspunkterFor(input, grunnlag, false);

        // Assert 1
        assertThat(aksjonspunktResultat).isEmpty();

        // Act 2: foreslå beregningsgrunnlag
        BeregningsgrunnlagRegelResultat resultat = beregningTjenesteWrapper.getForeslåBeregningsgrunnlagTjeneste().foreslåBeregningsgrunnlag(input, grunnlag);

        // Assert 2
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(resultat, Hjemmel.F_14_7_8_30);

        BeregningsgrunnlagEntitet foreslåttBeregningsgrunnlag = resultat.getBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periode = foreslåttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 2);
        verdikjedeTestHjelper.verifiserBGATførAvkorting(periode, ÅRSINNTEKT, virksomhetene);
        verdikjedeTestHjelper.verifiserSammenligningsgrunnlag(foreslåttBeregningsgrunnlag.getSammenligningsgrunnlag(),
            totalÅrsinntekt, SKJÆRINGSTIDSPUNKT_BEREGNING.minusYears(1).withDayOfMonth(1),
            SKJÆRINGSTIDSPUNKT_BEREGNING.withDayOfMonth(1).minusDays(1), 0L);

        // Act 3: fordel beregningsgrunnlag
        resultat = beregningTjenesteWrapper.getVurderBeregningsgrunnlagTjeneste().vurderBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, foreslåttBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));
        assertThat(resultat.getVilkårOppfylt()).isTrue();
        BeregningsgrunnlagEntitet fordeltBeregningsgrunnlag = fordelBeregningsgrunnlag(input, grunnlag, resultat);

        // Act 4: fastsette beregningsgrunnlag
        var fastsattBeregningsgrunnlag = beregningTjenesteWrapper.getFullføreBeregningsgrunnlagTjeneste().fullføreBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, fordeltBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));

        // Assert 3
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(fastsattBeregningsgrunnlag, Hjemmel.F_14_7_8_30);

        periode = fastsattBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 2, forventetDagsats);
        verdikjedeTestHjelper.verifiserBGATetterAvkorting(periode,
            ÅRSINNTEKT, ÅRSINNTEKT, virksomhetene, forventetRedusert, ÅRSINNTEKT, forventetRedusert, forventetRedusertBrukersAndel, false);
    }

    @Test
    public void toArbeidsforholdMedBgOver6gOgFullRefusjon() {
        String orgnr1 = ARBEIDSFORHOLD_ORGNR1;
        String orgnr2 = ARBEIDSFORHOLD_ORGNR2;

        final List<Double> ÅRSINNTEKT = List.of(448000d, 336000d);
        final Double totalÅrsinntekt = ÅRSINNTEKT.stream().reduce((v1, v2) -> v1 + v2).orElse(null);

        final double forventetRedusert1 = seksG * ÅRSINNTEKT.get(0) / (ÅRSINNTEKT.get(0) + ÅRSINNTEKT.get(1));
        final double forventetRedusert2 = seksG * ÅRSINNTEKT.get(1) / (ÅRSINNTEKT.get(0) + ÅRSINNTEKT.get(1));

        final List<Double> forventetRedusert = List.of(forventetRedusert1, forventetRedusert2);
        final List<Double> forventetRedusertBrukersAndel = List.of(0d, 0d);

        List<BigDecimal> månedsinntekter = ÅRSINNTEKT.stream().map((v) -> BigDecimal.valueOf(v / 12)).collect(Collectors.toList());
        List<String> virksomhetene = List.of(orgnr1, orgnr2);

        final long forventetDagsats = forventetRedusert.stream().mapToLong(dv -> Math.round(dv / 260)).sum() +
            forventetRedusertBrukersAndel.stream().mapToLong(dv -> Math.round(dv / 260)).sum();

        // Arrange 1
        BehandlingReferanse behandlingReferanse = lagBehandlingAT(scenario,
            BigDecimal.valueOf(totalÅrsinntekt / 12),
            virksomhetene);
        BehandlingReferanse ref = lagReferanse(behandlingReferanse);

        var im1 = verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, Arbeidsgiver.virksomhet(beregningVirksomhet1),
            månedsinntekter.get(0), månedsinntekter.get(0));
        var im2 = verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, Arbeidsgiver.virksomhet(beregningVirksomhet2),
            månedsinntekter.get(1), månedsinntekter.get(1));
        List<Inntektsmelding> inntektsmeldinger = List.of(im1, im2);

        Periode opptjeningPeriode = Periode.of(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1));
        var opptjeningAktiviteter = new OpptjeningAktiviteter(
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, orgnr1),
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, orgnr2));

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(behandlingReferanse.getId())).medInntektsmeldinger(inntektsmeldinger).build();
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(ref, iayGrunnlag, opptjeningAktiviteter, AktivitetGradering.INGEN_GRADERING, foreldrepengerGrunnlag);

        // Act 1: kontroller fakta for beregning
        BeregningsgrunnlagGrunnlagEntitet grunnlag = verdikjedeTestHjelper.kjørStegOgLagreGrunnlag(input, beregningTjenesteWrapper);
        input = input.medBeregningsgrunnlagGrunnlag(grunnlag);
        List<BeregningAksjonspunktResultat> aksjonspunktResultat = beregningTjenesteWrapper.getAksjonspunktUtlederFaktaOmBeregning().utledAksjonspunkterFor(input, grunnlag, false);

        // Assert 1
        assertThat(aksjonspunktResultat).isEmpty();

        // Act 2: foreslå beregningsgrunnlag
        BeregningsgrunnlagRegelResultat resultat = beregningTjenesteWrapper.getForeslåBeregningsgrunnlagTjeneste().foreslåBeregningsgrunnlag(input, grunnlag);

        // Assert 2
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(resultat, Hjemmel.F_14_7_8_30);

        BeregningsgrunnlagEntitet foreslåttBeregningsgrunnlag = resultat.getBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periode = foreslåttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 2);
        verdikjedeTestHjelper.verifiserBGATførAvkorting(periode, ÅRSINNTEKT, virksomhetene);
        verdikjedeTestHjelper.verifiserSammenligningsgrunnlag(foreslåttBeregningsgrunnlag.getSammenligningsgrunnlag(),
            totalÅrsinntekt, SKJÆRINGSTIDSPUNKT_BEREGNING.minusYears(1).withDayOfMonth(1),
            SKJÆRINGSTIDSPUNKT_BEREGNING.withDayOfMonth(1).minusDays(1), 0L);

        // Act 3: fordel beregningsgrunnlag
        resultat = beregningTjenesteWrapper.getVurderBeregningsgrunnlagTjeneste().vurderBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, foreslåttBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));
        assertThat(resultat.getVilkårOppfylt()).isTrue();
        BeregningsgrunnlagEntitet fordeltBeregningsgrunnlag = fordelBeregningsgrunnlag(input, grunnlag, resultat);

        // Act 4: fastsette beregningsgrunnlag
        var fastsattBeregningsgrunnlag = beregningTjenesteWrapper.getFullføreBeregningsgrunnlagTjeneste().fullføreBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, fordeltBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));

        // Assert 3
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(fastsattBeregningsgrunnlag, Hjemmel.F_14_7_8_30);

        periode = fastsattBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 2, forventetDagsats);
        verdikjedeTestHjelper.verifiserBGATetterAvkorting(periode,
            ÅRSINNTEKT, ÅRSINNTEKT, virksomhetene, forventetRedusert, ÅRSINNTEKT, forventetRedusert, forventetRedusertBrukersAndel, false);
    }

    @Test
    public void fireArbeidsforholdMedBgOver6gOgDelvisRefusjonUnder6G() {
        String orgnr1 = ARBEIDSFORHOLD_ORGNR1;
        String orgnr2 = ARBEIDSFORHOLD_ORGNR2;
        String orgnr3 = ARBEIDSFORHOLD_ORGNR3;
        String orgnr4 = ARBEIDSFORHOLD_ORGNR4;

        final List<Double> ÅRSINNTEKT = List.of(400000d, 500000d, 300000d, 100000d);
        final List<Double> refusjonsKrav = List.of(200000d, 150000d, 300000d, 100000d);
        final Double totalÅrsinntekt = ÅRSINNTEKT.stream().reduce((v1, v2) -> v1 + v2).orElse(null);

        double fordelingRunde2 = seksG - (refusjonsKrav.get(0) + refusjonsKrav.get(1));
        double forventetRedusert1 = refusjonsKrav.get(0);
        double forventetRedusert2 = refusjonsKrav.get(1);
        double forventetRedusert3 = fordelingRunde2 * ÅRSINNTEKT.get(2) / (ÅRSINNTEKT.get(2) + ÅRSINNTEKT.get(3));
        double forventetRedusert4 = fordelingRunde2 * ÅRSINNTEKT.get(3) / (ÅRSINNTEKT.get(2) + ÅRSINNTEKT.get(3));

        final List<Double> forventetRedusert = List.of(forventetRedusert1, forventetRedusert2, forventetRedusert3, forventetRedusert4);
        final List<Double> forventetRedusertBrukersAndel = List.of(0d, 0d, 0d, 0d);

        List<BigDecimal> månedsinntekter = ÅRSINNTEKT.stream().map((v) -> BigDecimal.valueOf(v / 12)).collect(Collectors.toList());
        List<String> virksomhetene = List.of(orgnr1, orgnr2, orgnr3, orgnr4);

        final long forventetDagsats = forventetRedusert.stream().mapToLong(dv -> Math.round(dv / 260)).sum() +
            forventetRedusertBrukersAndel.stream().mapToLong(dv -> Math.round(dv / 260)).sum();

        // Arrange 1
        BehandlingReferanse behandlingReferanse = lagBehandlingAT(scenario,
            BigDecimal.valueOf(totalÅrsinntekt / 12),
            virksomhetene);
        BehandlingReferanse ref = lagReferanse(behandlingReferanse);

        var im1 = verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, Arbeidsgiver.virksomhet(beregningVirksomhet1),
            månedsinntekter.get(0), BigDecimal.valueOf(refusjonsKrav.get(0) / 12));
        var im2 = verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, Arbeidsgiver.virksomhet(beregningVirksomhet2),
            månedsinntekter.get(1), BigDecimal.valueOf(refusjonsKrav.get(1) / 12));
        var im3 = verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, Arbeidsgiver.virksomhet(beregningVirksomhet3),
            månedsinntekter.get(2), BigDecimal.valueOf(refusjonsKrav.get(2) / 12));
        var im4 = verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, Arbeidsgiver.virksomhet(beregningVirksomhet4),
            månedsinntekter.get(3), BigDecimal.valueOf(refusjonsKrav.get(3) / 12));
        List<Inntektsmelding> inntektsmeldinger = List.of(im1, im2, im3, im4);

        Periode opptjeningPeriode = Periode.of(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1));
        var opptjeningAktiviteter = new OpptjeningAktiviteter(
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, orgnr1),
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, orgnr2),
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, orgnr3),
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, orgnr4));

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(behandlingReferanse.getId())).medInntektsmeldinger(inntektsmeldinger).build();
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(ref, iayGrunnlag, opptjeningAktiviteter, AktivitetGradering.INGEN_GRADERING, foreldrepengerGrunnlag);

        // Act 1: kontroller fakta for beregning
        BeregningsgrunnlagGrunnlagEntitet grunnlag = verdikjedeTestHjelper.kjørStegOgLagreGrunnlag(input, beregningTjenesteWrapper);
        input = input.medBeregningsgrunnlagGrunnlag(grunnlag);
        List<BeregningAksjonspunktResultat> aksjonspunktResultat = beregningTjenesteWrapper.getAksjonspunktUtlederFaktaOmBeregning().utledAksjonspunkterFor(input, grunnlag, false);

        // Assert 1
        assertThat(aksjonspunktResultat).isEmpty();

        // Act 2: foreslå beregningsgrunnlag
        BeregningsgrunnlagRegelResultat resultat = beregningTjenesteWrapper.getForeslåBeregningsgrunnlagTjeneste().foreslåBeregningsgrunnlag(input, grunnlag);

        // Assert 2
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(resultat, Hjemmel.F_14_7_8_30);

        BeregningsgrunnlagEntitet foreslåttBeregningsgrunnlag = resultat.getBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periode = foreslåttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 4);
        verdikjedeTestHjelper.verifiserBGATførAvkorting(periode, ÅRSINNTEKT, virksomhetene);
        verdikjedeTestHjelper.verifiserSammenligningsgrunnlag(foreslåttBeregningsgrunnlag.getSammenligningsgrunnlag(),
            totalÅrsinntekt, SKJÆRINGSTIDSPUNKT_BEREGNING.minusYears(1).withDayOfMonth(1),
            SKJÆRINGSTIDSPUNKT_BEREGNING.withDayOfMonth(1).minusDays(1), 0L);

        // Act 3: fordel beregningsgrunnlag
        resultat = beregningTjenesteWrapper.getVurderBeregningsgrunnlagTjeneste().vurderBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, foreslåttBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));
        assertThat(resultat.getVilkårOppfylt()).isTrue();
        BeregningsgrunnlagEntitet fordeltBeregningsgrunnlag = fordelBeregningsgrunnlag(input, grunnlag, resultat);

        // Act 4: fastsette beregningsgrunnlag
        var fastsattBeregningsgrunnlag = beregningTjenesteWrapper.getFullføreBeregningsgrunnlagTjeneste().fullføreBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, fordeltBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));

        // Assert 3
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(fastsattBeregningsgrunnlag, Hjemmel.F_14_7_8_30);

        periode = fastsattBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 4, forventetDagsats);
        verdikjedeTestHjelper.verifiserBGATetterAvkorting(periode,
            ÅRSINNTEKT, ÅRSINNTEKT, virksomhetene, forventetRedusert, refusjonsKrav, forventetRedusert, forventetRedusertBrukersAndel, false);
    }

    @Test
    public void fireArbeidsforholdMedBgOver6gOgDelvisRefusjonOver6G() {
        String orgnr1 = ARBEIDSFORHOLD_ORGNR1;
        String orgnr2 = ARBEIDSFORHOLD_ORGNR2;
        String orgnr3 = ARBEIDSFORHOLD_ORGNR3;
        String orgnr4 = ARBEIDSFORHOLD_ORGNR4;

        final List<Double> ÅRSINNTEKT = List.of(400000d, 500000d, 300000d, 100000d);
        final List<Double> refusjonsKrav = List.of(200000d, 150000d, 100000d, 42000d);

        final Double totalÅrsinntekt = ÅRSINNTEKT.stream().reduce((v1, v2) -> v1 + v2).orElse(null);

        double arb1 = refusjonsKrav.get(0);
        double arb2 = refusjonsKrav.get(1);
        double arb3 = refusjonsKrav.get(2);
        double arb4 = refusjonsKrav.get(3);

        double rest = seksG - (arb1 + arb4);
        double bruker1 = 0.0d;
        double bruker2 = rest * ÅRSINNTEKT.get(1) / (ÅRSINNTEKT.get(1) + ÅRSINNTEKT.get(2)) - arb2;
        double bruker3 = rest * ÅRSINNTEKT.get(2) / (ÅRSINNTEKT.get(1) + ÅRSINNTEKT.get(2)) - arb3;
        double bruker4 = 0.0d;

        final List<Double> forventetRedusert = List.of(arb1, arb2, arb3, arb4);

        final List<Double> forventetRedusertBrukersAndel = List.of(bruker1, bruker2, bruker3, bruker4);

        final long forventetDagsats = forventetRedusert.stream().mapToLong(dv -> Math.round(dv / 260)).sum() +
            forventetRedusertBrukersAndel.stream().mapToLong(dv -> Math.round(dv / 260)).sum();

        final List<Double> forventetAvkortet = List.of(arb1 + bruker1, arb2 + bruker2, arb3 + bruker3, arb4 + bruker4);

        List<BigDecimal> månedsinntekter = ÅRSINNTEKT.stream().map((v) -> BigDecimal.valueOf(v / 12)).collect(Collectors.toList());
        List<String> virksomhetene = List.of(orgnr1, orgnr2, orgnr3, orgnr4);

        // Arrange 1
        BehandlingReferanse behandlingReferanse = lagBehandlingAT(scenario,
            BigDecimal.valueOf(totalÅrsinntekt / 12),
            virksomhetene);
        BehandlingReferanse ref = lagReferanse(behandlingReferanse);

        var im1 = verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, Arbeidsgiver.virksomhet(beregningVirksomhet1),
            månedsinntekter.get(0), BigDecimal.valueOf(refusjonsKrav.get(0) / 12));
        var im2 = verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, Arbeidsgiver.virksomhet(beregningVirksomhet2),
            månedsinntekter.get(1), BigDecimal.valueOf(refusjonsKrav.get(1) / 12));
        var im3 = verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, Arbeidsgiver.virksomhet(beregningVirksomhet3),
            månedsinntekter.get(2), BigDecimal.valueOf(refusjonsKrav.get(2) / 12));
        var im4 = verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, Arbeidsgiver.virksomhet(beregningVirksomhet4),
            månedsinntekter.get(3), BigDecimal.valueOf(refusjonsKrav.get(3) / 12));
        List<Inntektsmelding> inntektsmeldinger = List.of(im1, im2, im3, im4);

        Periode opptjeningPeriode = Periode.of(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1));
        var opptjeningAktiviteter = new OpptjeningAktiviteter(
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, orgnr1),
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, orgnr2),
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, orgnr3),
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, orgnr4));

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(behandlingReferanse.getId())).medInntektsmeldinger(inntektsmeldinger).build();
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(ref, iayGrunnlag, opptjeningAktiviteter, AktivitetGradering.INGEN_GRADERING, foreldrepengerGrunnlag);

        // Act 1: kontroller fakta for beregning
        BeregningsgrunnlagGrunnlagEntitet grunnlag = verdikjedeTestHjelper.kjørStegOgLagreGrunnlag(input, beregningTjenesteWrapper);
        input = input.medBeregningsgrunnlagGrunnlag(grunnlag);
        List<BeregningAksjonspunktResultat> aksjonspunktResultat = beregningTjenesteWrapper.getAksjonspunktUtlederFaktaOmBeregning().utledAksjonspunkterFor(input, grunnlag, false);

        // Assert 1
        assertThat(aksjonspunktResultat).isEmpty();

        // Act 2: foreslå beregningsgrunnlag
        BeregningsgrunnlagRegelResultat resultat = beregningTjenesteWrapper.getForeslåBeregningsgrunnlagTjeneste().foreslåBeregningsgrunnlag(input, grunnlag);

        // Assert 2
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(resultat, Hjemmel.F_14_7_8_30);

        BeregningsgrunnlagEntitet foreslåttBeregningsgrunnlag = resultat.getBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periode = foreslåttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 4);
        verdikjedeTestHjelper.verifiserBGATførAvkorting(periode, ÅRSINNTEKT, virksomhetene);
        verdikjedeTestHjelper.verifiserSammenligningsgrunnlag(foreslåttBeregningsgrunnlag.getSammenligningsgrunnlag(),
            totalÅrsinntekt, SKJÆRINGSTIDSPUNKT_BEREGNING.minusYears(1).withDayOfMonth(1),
            SKJÆRINGSTIDSPUNKT_BEREGNING.withDayOfMonth(1).minusDays(1), 0L);

        // Act 3: fordel beregningsgrunnlag
        resultat = beregningTjenesteWrapper.getVurderBeregningsgrunnlagTjeneste().vurderBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, foreslåttBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));
        assertThat(resultat.getVilkårOppfylt()).isTrue();
        BeregningsgrunnlagEntitet fordeltBeregningsgrunnlag = fordelBeregningsgrunnlag(input, grunnlag, resultat);

        // Act 4: fastsette beregningsgrunnlag
        var fastsattBeregningsgrunnlag = beregningTjenesteWrapper.getFullføreBeregningsgrunnlagTjeneste().fullføreBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, fordeltBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));

        // Assert 3
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(fastsattBeregningsgrunnlag, Hjemmel.F_14_7_8_30);

        periode = fastsattBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 4, forventetDagsats);
        verdikjedeTestHjelper.verifiserBGATetterAvkorting(periode,
            ÅRSINNTEKT, ÅRSINNTEKT, virksomhetene, forventetAvkortet, refusjonsKrav, forventetRedusert, forventetRedusertBrukersAndel, false);
    }

    @Test
    public void toArbeidsforholdMedOverstyringEtterTilbakeføringOver6GMedRefusjonOver6G() {
        String orgnr1 = ARBEIDSFORHOLD_ORGNR1;
        String orgnr2 = ARBEIDSFORHOLD_ORGNR2;

        final List<Double> ÅRSINNTEKT = List.of(720000d, 720000d);
        final Double totalÅrsinntekt = ÅRSINNTEKT.stream().reduce((v1, v2) -> v1 + v2).orElse(null);
        final List<Double> refusjonsKrav = List.of(seksG, seksG);

        final double forventetRedusert1 = seksG * ÅRSINNTEKT.get(0) / (ÅRSINNTEKT.get(0) + ÅRSINNTEKT.get(1));
        final double forventetRedusert2 = seksG * ÅRSINNTEKT.get(1) / (ÅRSINNTEKT.get(0) + ÅRSINNTEKT.get(1));

        final List<Double> forventetRedusert = List.of(forventetRedusert1, forventetRedusert2);
        final List<Double> forventetRedusertBrukersAndel = List.of(0d, 0d);

        List<BigDecimal> månedsinntekter = ÅRSINNTEKT.stream().map((v) -> BigDecimal.valueOf(v / 12)).collect(Collectors.toList());

        // Arrange 1
        List<String> virksomhetene = List.of(orgnr1, orgnr2);
        BehandlingReferanse behandlingReferanse = lagBehandlingAT(scenario,
            BigDecimal.valueOf(totalÅrsinntekt / 12),
            virksomhetene);
        BehandlingReferanse ref = lagReferanse(behandlingReferanse);

        var im1 = verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, Arbeidsgiver.virksomhet(beregningVirksomhet1),
            månedsinntekter.get(0), BigDecimal.valueOf(refusjonsKrav.get(0) / 12));
        var im2 = verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, Arbeidsgiver.virksomhet(beregningVirksomhet2),
            månedsinntekter.get(1), BigDecimal.valueOf(refusjonsKrav.get(1) / 12));
        List<Inntektsmelding> inntektsmeldinger = List.of(im1, im2);

        Periode opptjeningPeriode = Periode.of(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1));
        var opptjeningAktiviteter = new OpptjeningAktiviteter(
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, orgnr1),
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, orgnr2));

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(behandlingReferanse.getId())).medInntektsmeldinger(inntektsmeldinger).build();
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(ref, iayGrunnlag, opptjeningAktiviteter, AktivitetGradering.INGEN_GRADERING, foreldrepengerGrunnlag);

        // Act 1: kontroller fakta for beregning
        BeregningsgrunnlagGrunnlagEntitet grunnlag = verdikjedeTestHjelper.kjørStegOgLagreGrunnlag(input, beregningTjenesteWrapper);
        input = input.medBeregningsgrunnlagGrunnlag(grunnlag);
        List<BeregningAksjonspunktResultat> aksjonspunktResultat = beregningTjenesteWrapper.getAksjonspunktUtlederFaktaOmBeregning().utledAksjonspunkterFor(input, grunnlag, false);

        // Assert 1
        assertThat(aksjonspunktResultat).isEmpty();

        // Act 2: foreslå beregningsgrunnlag
        BeregningsgrunnlagRegelResultat resultat = beregningTjenesteWrapper.getForeslåBeregningsgrunnlagTjeneste().foreslåBeregningsgrunnlag(input, grunnlag);

        // Assert 2
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(resultat, Hjemmel.F_14_7_8_30);

        BeregningsgrunnlagEntitet beregningsgrunnlagEtter1 = resultat.getBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periodeEtter1 = beregningsgrunnlagEtter1.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserPeriode(periodeEtter1, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 2);
        verdikjedeTestHjelper.verifiserBGATførAvkorting(periodeEtter1, ÅRSINNTEKT, virksomhetene);
        verdikjedeTestHjelper.verifiserSammenligningsgrunnlag(beregningsgrunnlagEtter1.getSammenligningsgrunnlag(),
            totalÅrsinntekt, SKJÆRINGSTIDSPUNKT_BEREGNING.minusYears(1).withDayOfMonth(1),
            SKJÆRINGSTIDSPUNKT_BEREGNING.withDayOfMonth(1).minusDays(1), 0L);

        // Arrange 2: Overstyring
        double overstyrt = 700000.0;
        final BeregningsgrunnlagPeriode periode1 = periodeEtter1;
        periodeEtter1.getBeregningsgrunnlagPrStatusOgAndelList().forEach(af -> BeregningsgrunnlagPrStatusOgAndel.builder(af)
            .medOverstyrtPrÅr(BigDecimal.valueOf(overstyrt))
            .build(periode1));

        // Act 3: fordel beregningsgrunnlag
        resultat = beregningTjenesteWrapper.getVurderBeregningsgrunnlagTjeneste().vurderBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, beregningsgrunnlagEtter1, BeregningsgrunnlagTilstand.FORESLÅTT));
        assertThat(resultat.getVilkårOppfylt()).isTrue();
        beregningsgrunnlagEtter1 = fordelBeregningsgrunnlag(input, grunnlag, resultat);

        // Act 4: fastsette beregningsgrunnlag
        var fastsattBeregningsgrunnlag = beregningTjenesteWrapper.getFullføreBeregningsgrunnlagTjeneste().fullføreBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, beregningsgrunnlagEtter1, BeregningsgrunnlagTilstand.FASTSATT_INN));

        // Arrange 3: Tilbakehopp med Overstyring
        double overstyrt2 = 720000.0;
        BeregningsgrunnlagEntitet beregningsgrunnlagEtter2 = fastsattBeregningsgrunnlag;
        final BeregningsgrunnlagPeriode periodeEtter2 = beregningsgrunnlagEtter2.getBeregningsgrunnlagPerioder().get(0);
        periodeEtter2.getBeregningsgrunnlagPrStatusOgAndelList().forEach(af -> BeregningsgrunnlagPrStatusOgAndel.builder(af)
            .medOverstyrtPrÅr(BigDecimal.valueOf(overstyrt2))
            .build(periodeEtter2));

        // Act 3: fordel beregningsgrunnlag
        resultat = beregningTjenesteWrapper.getVurderBeregningsgrunnlagTjeneste().vurderBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, beregningsgrunnlagEtter2, BeregningsgrunnlagTilstand.FORESLÅTT));
        assertThat(resultat.getVilkårOppfylt()).isTrue();
        beregningsgrunnlagEtter2 = fordelBeregningsgrunnlag(input, grunnlag, resultat);

        // Act 4: fastsette beregningsgrunnlag
        fastsattBeregningsgrunnlag = beregningTjenesteWrapper.getFullføreBeregningsgrunnlagTjeneste().fullføreBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, beregningsgrunnlagEtter2, BeregningsgrunnlagTilstand.FORESLÅTT_UT));

        // Assert 3-4
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(fastsattBeregningsgrunnlag, Hjemmel.F_14_7_8_30);

        BeregningsgrunnlagPeriode periodeEtter3 = fastsattBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        Long forvetetAndelSum = Math.round((seksG / 2) / 260) * 2;
        verdikjedeTestHjelper.verifiserPeriode(periodeEtter3, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 2, forvetetAndelSum);
        verdikjedeTestHjelper.verifiserBGATetterAvkorting(periodeEtter3,
            ÅRSINNTEKT, ÅRSINNTEKT, virksomhetene, forventetRedusert, refusjonsKrav, forventetRedusert, forventetRedusertBrukersAndel, true);
    }

    private BeregningsgrunnlagEntitet fordelBeregningsgrunnlag(BeregningsgrunnlagInput input, BeregningsgrunnlagGrunnlagEntitet grunnlag,
                                                               BeregningsgrunnlagRegelResultat resultat) {
        return beregningTjenesteWrapper.getFordelBeregningsgrunnlagTjeneste().fordelBeregningsgrunnlag(input.medBeregningsgrunnlagGrunnlag(grunnlag),
            resultat.getBeregningsgrunnlag());
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

    private void verifiserBeregningsgrunnlagMedAksjonspunkt(BeregningsgrunnlagRegelResultat resultat) {
        assertThat(resultat.getBeregningsgrunnlag()).isNotNull();
        assertThat(resultat.getAksjonspunkter()).hasSize(1);
        assertThat(resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder()).hasSize(1);
    }

    private void verifiserBGATetterOverstyring(BeregningsgrunnlagPrStatusOgAndel bgpsa,
                                               Double bg,
                                               String virksomhetOrgnr,
                                               Double overstyrt,
                                               Double avkortet,
                                               Double redusert,
                                               Double maksimalRefusjon,
                                               Double avkortetRefusjon,
                                               Double avkortetBrukersAndel,
                                               Double redusertRefusjon,
                                               Double redusertBrukersAndel) {
        assertThat(bgpsa.getAktivitetStatus()).isEqualTo(AktivitetStatus.ARBEIDSTAKER);
        assertThat(bgpsa.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver))
            .hasValueSatisfying(arbeidsgiver -> assertThat(arbeidsgiver.getOrgnr()).isEqualTo(virksomhetOrgnr));
        assertThat(bgpsa.getBgAndelArbeidsforhold()
            .map(BGAndelArbeidsforhold::getArbeidsforholdRef)
            .map(InternArbeidsforholdRef::gjelderForSpesifiktArbeidsforhold).orElse(false))
                .as("gjelderSpesifiktArbeidsforhold").isFalse();
        assertThat(bgpsa.getArbeidsforholdType()).isEqualTo(OpptjeningAktivitetType.ARBEID);
        assertThat(bgpsa.getBeregnetPrÅr().doubleValue()).isEqualTo(bg);
        assertThat(bgpsa.getBruttoPrÅr().doubleValue()).isEqualTo(overstyrt);

        assertThat(bgpsa.getOverstyrtPrÅr().doubleValue()).as("OverstyrtPrÅr")
            .isEqualTo(overstyrt);
        assertThat(bgpsa.getAvkortetPrÅr().doubleValue()).as("AvkortetPrÅr")
            .isCloseTo(avkortet, within(0.01));
        assertThat(bgpsa.getRedusertPrÅr().doubleValue()).as("RedusertPrÅr")
            .isCloseTo(redusert, within(0.01));

        assertThat(bgpsa.getBgAndelArbeidsforhold().flatMap(BGAndelArbeidsforhold::getNaturalytelseBortfaltPrÅr))
            .as("NaturalytelseBortfaltPrÅr")
            .isEmpty();

        assertThat(bgpsa.getMaksimalRefusjonPrÅr().doubleValue()).as("MaksimalRefusjonPrÅr")
            .isCloseTo(maksimalRefusjon, within(0.01));
        assertThat(bgpsa.getAvkortetRefusjonPrÅr().doubleValue()).as("AvkortetRefusjonPrÅr")
            .isCloseTo(avkortetRefusjon, within(0.01));
        assertThat(bgpsa.getRedusertRefusjonPrÅr().doubleValue()).as("RedusertRefusjonPrÅr")
            .isCloseTo(redusertRefusjon, within(0.01));

        assertThat(bgpsa.getAvkortetBrukersAndelPrÅr().doubleValue()).as("AvkortetBrukersAndelPrÅr")
            .isCloseTo(avkortetBrukersAndel, within(0.01));
        assertThat(bgpsa.getRedusertBrukersAndelPrÅr().doubleValue()).as("RedusertBrukersAndelPrÅr")
            .isCloseTo(redusertBrukersAndel, within(0.01));
    }

}
