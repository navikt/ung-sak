package no.nav.folketrygdloven.beregningsgrunnlag.verdikjede;

import static no.nav.folketrygdloven.beregningsgrunnlag.verdikjede.BeregningsgrunnlagGrunnlagTestUtil.nyttGrunnlag;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.opptjening.OpptjeningAktiviteter;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningAksjonspunktResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningsgrunnlagRegelResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.AbstractTestScenario;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningSatsType;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;
import no.nav.k9.kodeverk.beregningsgrunnlag.Hjemmel;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;

public class FrilanserTest {

    private static final String DUMMY_ORGNR = "974760673";
    private static final LocalDate SKJÆRINGSTIDSPUNKT_BEREGNING = VerdikjedeTestHjelper.SKJÆRINGSTIDSPUNKT_OPPTJENING;
    private static final LocalDate MINUS_YEARS_1 = SKJÆRINGSTIDSPUNKT_BEREGNING.minusYears(1);
    private static final String ARBEIDSFORHOLD_ORGNR1 = "915933149";
    private static final String ARBEIDSFORHOLD_ORGNR2 = "923609016";
    private static final String ARBEIDSFORHOLD_ORGNR3 = "973152351";

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private RepositoryProvider repositoryProvider = new RepositoryProvider(repoRule.getEntityManager());
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository = repositoryProvider.getBeregningsgrunnlagRepository();
    private double seksG;
    private TestScenarioBuilder scenario;

    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private VerdikjedeTestHjelper verdikjedeTestHjelper = new VerdikjedeTestHjelper(new InntektsmeldingTjeneste(iayTjeneste));
    private BeregningTjenesteWrapper beregningTjenesteWrapper;

    @Before
    public void setup() {
        beregningTjenesteWrapper = BeregningTjenesteProvider.provide(repoRule, iayTjeneste);

        scenario = TestScenarioBuilder.nyttScenario();
        seksG = beregningsgrunnlagRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, SKJÆRINGSTIDSPUNKT_BEREGNING).getVerdi() * 6;
    }

    @Test
    public void toArbeidsforholdOgFrilansMedBgOver6gOgRefusjonUnder6G() {

        final List<Double> ÅRSINNTEKT = List.of(12 * 28_000d, 12 * 14_000d);
        final List<Double> refusjonsKrav = List.of(12 * 20_000d, 12 * 15_000d);
        final double sammenligning = 12 * 67_500d;
        final double frilansÅrsinntekt = 12 * 23_000d;

        double forventetRedusert1 = refusjonsKrav.get(0);
        double forventetRedusert2 = refusjonsKrav.get(1);

        double forventetFlyttetTilArbeidsforhold = Math.max(refusjonsKrav.get(0) - ÅRSINNTEKT.get(0), 0L) + Math.max(refusjonsKrav.get(1) - ÅRSINNTEKT.get(1), 0L);
        List<Double> forventetFordelt = List.of(Math.max(refusjonsKrav.get(0), ÅRSINNTEKT.get(0)), Math.max(refusjonsKrav.get(1), ÅRSINNTEKT.get(1)));

        double forventetBrukersAndel1 = forventetFordelt.get(0) - forventetRedusert1;
        double forventetBrukersAndel2 = forventetFordelt.get(1) - forventetRedusert2;


        double fordeltÅrsinntektFL = frilansÅrsinntekt - forventetFlyttetTilArbeidsforhold;
        double forventetBrukersAndelFL = Math.min(fordeltÅrsinntektFL, seksG - (forventetFordelt.stream().mapToDouble(Double::doubleValue).sum()));

        final List<Double> forventetRedusert = List.of(forventetRedusert1, forventetRedusert2);
        final List<Double> forventetRedusertBrukersAndel = List.of(forventetBrukersAndel1, forventetBrukersAndel2);

        List<String> virksomhetene = List.of(ARBEIDSFORHOLD_ORGNR1, ARBEIDSFORHOLD_ORGNR2);
        List<BigDecimal> månedsinntekter = ÅRSINNTEKT.stream().map((v) -> BigDecimal.valueOf(v / 12)).collect(Collectors.toList());

        // Arrange
        BehandlingReferanse behandlingReferanse = lagBehandlingATogFL(scenario, BigDecimal.valueOf(sammenligning / 12),
            månedsinntekter,
            BigDecimal.valueOf(frilansÅrsinntekt / 12),
            virksomhetene);

        var im1 = verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), månedsinntekter.get(0),
            BigDecimal.valueOf(refusjonsKrav.get(0) / 12));
        var im2 = verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR2), månedsinntekter.get(1),
            BigDecimal.valueOf(refusjonsKrav.get(1) / 12));
        List<Inntektsmelding> inntektsmeldinger = List.of(im1, im2);
        var opptjeningPeriode = Periode.of(SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(10), SKJÆRINGSTIDSPUNKT_BEREGNING.minusDays(1));

        var opptjeningAktiviteter = new OpptjeningAktiviteter(
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, ARBEIDSFORHOLD_ORGNR1),
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, ARBEIDSFORHOLD_ORGNR2),
            OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.FRILANS, opptjeningPeriode)
            );

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(behandlingReferanse.getId())).medInntektsmeldinger(inntektsmeldinger).build();
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(behandlingReferanse, iayGrunnlag, opptjeningAktiviteter, AktivitetGradering.INGEN_GRADERING, foreldrepengerGrunnlag);

        // Act 1: kontroller fakta om beregning
        BeregningsgrunnlagGrunnlagEntitet grunnlag = kjørStegOgLagreGrunnlag(input);
        input = input.medBeregningsgrunnlagGrunnlag(grunnlag);

        List<BeregningAksjonspunktResultat> aksjonspunktResultat = beregningTjenesteWrapper.getAksjonspunktUtlederFaktaOmBeregning().utledAksjonspunkterFor(input, grunnlag, false);

        // Assert 1
        assertThat(aksjonspunktResultat).hasSize(1);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().orElse(null);
        List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller = beregningsgrunnlag.getFaktaOmBeregningTilfeller();
        assertThat(faktaOmBeregningTilfeller).hasSize(1);
        assertThat(faktaOmBeregningTilfeller.get(0)).isEqualTo(FaktaOmBeregningTilfelle.VURDER_MOTTAR_YTELSE);

        // Act 2: foreslå beregningsgrunnlag
        BeregningsgrunnlagRegelResultat resultat = beregningTjenesteWrapper.getForeslåBeregningsgrunnlagTjeneste().foreslåBeregningsgrunnlag(input, grunnlag);

        // Assert 2
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(resultat, Hjemmel.F_14_7_8_40);

        BeregningsgrunnlagEntitet foreslåttBeregningsgrunnlag = resultat.getBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periode = foreslåttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 3);
        verdikjedeTestHjelper.verifiserBGATførAvkorting(periode, ÅRSINNTEKT, virksomhetene);
        verdikjedeTestHjelper.verifiserFLførAvkorting(periode, frilansÅrsinntekt);
        verdikjedeTestHjelper.verifiserSammenligningsgrunnlag(foreslåttBeregningsgrunnlag.getSammenligningsgrunnlag(),
            sammenligning, SKJÆRINGSTIDSPUNKT_BEREGNING.minusYears(1).withDayOfMonth(1),
            SKJÆRINGSTIDSPUNKT_BEREGNING.withDayOfMonth(1).minusDays(1), 37L);

        // Act 3: fordel beregningsgrunnlag
        resultat = beregningTjenesteWrapper.getVurderBeregningsgrunnlagTjeneste().vurderBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, foreslåttBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));
        assertThat(resultat.getVilkårOppfylt()).isTrue();
        BeregningsgrunnlagEntitet fordeltBeregningsgrunnlag = fordelBeregningsgrunnlag(input,
            resultat.getBeregningsgrunnlag(), grunnlag);


        // Act 4: fastsette beregningsgrunnlag
        var fastsattBeregningsgrunnlag = beregningTjenesteWrapper.getFullføreBeregningsgrunnlagTjeneste().fullføreBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, fordeltBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));

        // Assert 3
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(fastsattBeregningsgrunnlag, Hjemmel.F_14_7_8_40);

        periode = fastsattBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserBGATetterAvkorting(periode,
            ÅRSINNTEKT, forventetFordelt, virksomhetene, forventetFordelt, forventetRedusert, forventetRedusert, forventetRedusertBrukersAndel, false);
        verdikjedeTestHjelper.verifiserFLetterAvkorting(periode, frilansÅrsinntekt, fordeltÅrsinntektFL, forventetBrukersAndelFL, forventetBrukersAndelFL);
    }

    @Test
    public void treArbeidsforholdOgFrilansMedBgUnder6gOgRefusjonUnder6G() {

        final List<Double> ÅRSINNTEKT = List.of(12 * 13_000d, 12 * 12_000d, 12 * 8_000d);
        final List<Double> refusjonsKrav = List.of(12 * 15_500d, 12 * 9_000d, 12 * 17_781d);
        final Double sammenligning = 12 * 60000d;
        final Double frilansÅrsinntekt = 12 * 13000d;

        double forventetRedusert1 = refusjonsKrav.get(0);
        double forventetRedusert2 = refusjonsKrav.get(1);
        double forventetRedusert3 = refusjonsKrav.get(2);

        double forventetFlyttetTilArbeidsforhold = Math.max(refusjonsKrav.get(0) - ÅRSINNTEKT.get(0), 0L) + Math.max(refusjonsKrav.get(1) - ÅRSINNTEKT.get(1), 0L) + Math.max(refusjonsKrav.get(2) - ÅRSINNTEKT.get(2), 0L);
        List<Double> forventetFordelt = List.of(Math.max(refusjonsKrav.get(0), ÅRSINNTEKT.get(0)), Math.max(refusjonsKrav.get(1), ÅRSINNTEKT.get(1)), Math.max(refusjonsKrav.get(2), ÅRSINNTEKT.get(2)));

        double forventetBrukersAndel1 = forventetFordelt.get(0) - forventetRedusert1;
        double forventetBrukersAndel2 = forventetFordelt.get(1) - forventetRedusert2;
        double forventetBrukersAndel3 = forventetFordelt.get(2) - forventetRedusert3;
        double fordeltÅrsinntektFL = frilansÅrsinntekt - forventetFlyttetTilArbeidsforhold;
        double forventetBrukersAndelFL = Math.min(fordeltÅrsinntektFL, seksG - (forventetFordelt.stream().mapToDouble(Double::doubleValue).sum()));

        final List<Double> forventetRedusert = List.of(forventetRedusert1, forventetRedusert2, forventetRedusert3);
        final List<Double> forventetRedusertBrukersAndel = List.of(forventetBrukersAndel1, forventetBrukersAndel2, forventetBrukersAndel3);

        List<String> virksomhetene = List.of(ARBEIDSFORHOLD_ORGNR1, ARBEIDSFORHOLD_ORGNR2, ARBEIDSFORHOLD_ORGNR3);

        List<BigDecimal> månedsinntekter = ÅRSINNTEKT.stream().map((v) -> BigDecimal.valueOf(v / 12)).collect(Collectors.toList());

        // Arrange
        BehandlingReferanse behandlingReferanse = lagBehandlingATogFL(scenario, BigDecimal.valueOf(sammenligning / 12),
            månedsinntekter,
            BigDecimal.valueOf(frilansÅrsinntekt / 12),
            virksomhetene);

        var im1 = verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), månedsinntekter.get(0),
            BigDecimal.valueOf(refusjonsKrav.get(0) / 12));
        var im2 = verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR2), månedsinntekter.get(1),
            BigDecimal.valueOf(refusjonsKrav.get(1) / 12));
        var im3 = verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR3), månedsinntekter.get(2),
            BigDecimal.valueOf(refusjonsKrav.get(2) / 12));
        List<Inntektsmelding> inntektsmeldinger = List.of(im1, im2, im3);

        var opptjeningPeriode = Periode.of(SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(10), SKJÆRINGSTIDSPUNKT_BEREGNING.minusDays(1));

        var opptjeningAktiviteter = new OpptjeningAktiviteter(
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, ARBEIDSFORHOLD_ORGNR1),
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, ARBEIDSFORHOLD_ORGNR2),
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, ARBEIDSFORHOLD_ORGNR3),
            OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.FRILANS, opptjeningPeriode)
            );


        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(behandlingReferanse.getId())).medInntektsmeldinger(inntektsmeldinger).build();
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(behandlingReferanse, iayGrunnlag, opptjeningAktiviteter, AktivitetGradering.INGEN_GRADERING, foreldrepengerGrunnlag);

        // Act 1: kontroller fakta om beregning
        BeregningsgrunnlagGrunnlagEntitet grunnlag = kjørStegOgLagreGrunnlag(input);
        input = input.medBeregningsgrunnlagGrunnlag(grunnlag);
        List<BeregningAksjonspunktResultat> aksjonspunktResultat = beregningTjenesteWrapper.getAksjonspunktUtlederFaktaOmBeregning().utledAksjonspunkterFor(input, grunnlag, false);

        // Assert 1
        assertThat(aksjonspunktResultat).hasSize(1);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().orElse(null);
        List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller = beregningsgrunnlag.getFaktaOmBeregningTilfeller();
        assertThat(faktaOmBeregningTilfeller).hasSize(1);
        assertThat(faktaOmBeregningTilfeller.get(0)).isEqualTo(FaktaOmBeregningTilfelle.VURDER_MOTTAR_YTELSE);

        // Act 2: foreslå beregningsgrunnlag
        BeregningsgrunnlagRegelResultat resultat = beregningTjenesteWrapper.getForeslåBeregningsgrunnlagTjeneste().foreslåBeregningsgrunnlag(input, grunnlag);

        // Assert 2
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(resultat, Hjemmel.F_14_7_8_40);

        BeregningsgrunnlagEntitet foreslåttBeregningsgrunnlag = resultat.getBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periode = foreslåttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 4);
        verdikjedeTestHjelper.verifiserBGATførAvkorting(periode, ÅRSINNTEKT, virksomhetene);
        verdikjedeTestHjelper.verifiserFLførAvkorting(periode, frilansÅrsinntekt);
        verdikjedeTestHjelper.verifiserSammenligningsgrunnlag(foreslåttBeregningsgrunnlag.getSammenligningsgrunnlag(),
            sammenligning, SKJÆRINGSTIDSPUNKT_BEREGNING.minusYears(1).withDayOfMonth(1),
            SKJÆRINGSTIDSPUNKT_BEREGNING.withDayOfMonth(1).minusDays(1), 233L);

        // Act 3: fordel beregningsgrunnlag
        resultat = beregningTjenesteWrapper.getVurderBeregningsgrunnlagTjeneste().vurderBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, foreslåttBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));
        assertThat(resultat.getVilkårOppfylt()).isTrue();
        BeregningsgrunnlagEntitet fordeltBeregningsgrunnlag = fordelBeregningsgrunnlag(input,
            resultat.getBeregningsgrunnlag(), grunnlag);

        // Act 4: fastsette beregningsgrunnlag
        var fastsattBeregningsgrunnlag = beregningTjenesteWrapper.getFullføreBeregningsgrunnlagTjeneste().fullføreBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, fordeltBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));

        // Assert 3
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(fastsattBeregningsgrunnlag, Hjemmel.F_14_7_8_40);

        periode = fastsattBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserBGATetterAvkorting(periode,
            ÅRSINNTEKT, forventetFordelt, virksomhetene, forventetFordelt, forventetRedusert, forventetRedusert, forventetRedusertBrukersAndel, false);
        verdikjedeTestHjelper.verifiserFLetterAvkorting(periode, frilansÅrsinntekt, fordeltÅrsinntektFL, fordeltÅrsinntektFL, forventetBrukersAndelFL);
    }

    @Test
    public void treArbeidsforholdOgFrilansMedBgOver6gOgRefusjonUnder6G() {

        final List<Double> ÅRSINNTEKT = List.of(12 * 13_000d, 12 * 12_000d, 12 * 22_500d);
        final List<Double> refusjonsKrav = List.of(12 * 15_500d, 12 * 9_000d, 12 * 17_781d);
        final double sammenligning = 12 * 61_000d;
        final double frilansÅrsinntekt = 12 * 14_000d;

        double forventetRedusert1 = refusjonsKrav.get(0);
        double forventetRedusert2 = refusjonsKrav.get(1);
        double forventetRedusert3 = refusjonsKrav.get(2);

        double forventetFlyttetTilArbeidsforhold = Math.max(refusjonsKrav.get(0) - ÅRSINNTEKT.get(0), 0L) + Math.max(refusjonsKrav.get(1) - ÅRSINNTEKT.get(1), 0L) + Math.max(refusjonsKrav.get(2) - ÅRSINNTEKT.get(2), 0L);
        double forventetFordeltFL = frilansÅrsinntekt - forventetFlyttetTilArbeidsforhold;
        List<Double> forventetFordelt = List.of(Math.max(refusjonsKrav.get(0), ÅRSINNTEKT.get(0)), Math.max(refusjonsKrav.get(1), ÅRSINNTEKT.get(1)), Math.max(refusjonsKrav.get(2), ÅRSINNTEKT.get(2)));

        double forventetBrukersAndel1 = 0;
        double andel = forventetFordelt.get(1) / (forventetFordelt.get(1) + forventetFordelt.get(2));
        double forventetBrukersAndel2 = (seksG - forventetRedusert1) * andel - forventetRedusert2;
        double forventetBrukersAndel3 = seksG - forventetRedusert1 - forventetBrukersAndel2 - forventetRedusert2 - forventetRedusert3;
        double forventetBrukersAndelFL = 0;

        final List<Double> avkortetBG = List.of(forventetBrukersAndel1 + forventetRedusert1, forventetBrukersAndel2 + forventetRedusert2,
            forventetBrukersAndel3 + forventetRedusert3);

        final List<Double> forventetRedusert = List.of(forventetRedusert1, forventetRedusert2, forventetRedusert3);
        final List<Double> forventetRedusertBrukersAndel = List.of(forventetBrukersAndel1, forventetBrukersAndel2, forventetBrukersAndel3);

        List<BigDecimal> månedsinntekter = ÅRSINNTEKT.stream().map((v) -> BigDecimal.valueOf(v / 12)).collect(Collectors.toList());

        List<String> virksomhetene = List.of(ARBEIDSFORHOLD_ORGNR1, ARBEIDSFORHOLD_ORGNR2, ARBEIDSFORHOLD_ORGNR3);

        // Arrange
        BehandlingReferanse behandlingReferanse = lagBehandlingATogFL(scenario, BigDecimal.valueOf(sammenligning / 12),
            månedsinntekter,
            BigDecimal.valueOf(frilansÅrsinntekt / 12),
            virksomhetene);

        var im1 = verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), månedsinntekter.get(0),
            BigDecimal.valueOf(refusjonsKrav.get(0) / 12));
        var im2 = verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR2), månedsinntekter.get(1),
            BigDecimal.valueOf(refusjonsKrav.get(1) / 12));
        var im3 = verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR3), månedsinntekter.get(2),
            BigDecimal.valueOf(refusjonsKrav.get(2) / 12));
        List<Inntektsmelding> inntektsmeldinger = List.of(im1, im2, im3);

        var opptjeningPeriode = Periode.of(SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(10), SKJÆRINGSTIDSPUNKT_BEREGNING.minusDays(1));

        var opptjeningAktiviteter = new OpptjeningAktiviteter(
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, ARBEIDSFORHOLD_ORGNR1),
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, ARBEIDSFORHOLD_ORGNR2),
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, ARBEIDSFORHOLD_ORGNR3),
            OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.FRILANS, opptjeningPeriode)
            );

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(behandlingReferanse.getId())).medInntektsmeldinger(inntektsmeldinger).build();
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(behandlingReferanse, iayGrunnlag, opptjeningAktiviteter, AktivitetGradering.INGEN_GRADERING, foreldrepengerGrunnlag);

        // Act 1: kontroller fakta om beregning
        BeregningsgrunnlagGrunnlagEntitet grunnlag = kjørStegOgLagreGrunnlag(input);
        input = input.medBeregningsgrunnlagGrunnlag(grunnlag);

        List<BeregningAksjonspunktResultat> aksjonspunktResultat = beregningTjenesteWrapper.getAksjonspunktUtlederFaktaOmBeregning().utledAksjonspunkterFor(input, grunnlag, false);

        // Assert 1
        assertThat(aksjonspunktResultat).hasSize(1);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().orElse(null);
        List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller = beregningsgrunnlag.getFaktaOmBeregningTilfeller();
        assertThat(faktaOmBeregningTilfeller).hasSize(1);
        assertThat(faktaOmBeregningTilfeller.get(0)).isEqualTo(FaktaOmBeregningTilfelle.VURDER_MOTTAR_YTELSE);

        // Act 2: foreslå beregningsgrunnlag
        BeregningsgrunnlagRegelResultat resultat = beregningTjenesteWrapper.getForeslåBeregningsgrunnlagTjeneste().foreslåBeregningsgrunnlag(input, grunnlag);
        input = input.medBeregningsgrunnlagGrunnlag(grunnlag);

        // Assert 2
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(resultat, Hjemmel.F_14_7_8_40);

        BeregningsgrunnlagEntitet foreslåttBeregningsgrunnlag = resultat.getBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periode = foreslåttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 4);
        verdikjedeTestHjelper.verifiserBGATførAvkorting(periode, ÅRSINNTEKT, virksomhetene);
        verdikjedeTestHjelper.verifiserFLførAvkorting(periode, frilansÅrsinntekt);
        verdikjedeTestHjelper.verifiserSammenligningsgrunnlag(foreslåttBeregningsgrunnlag.getSammenligningsgrunnlag(),
            sammenligning, SKJÆRINGSTIDSPUNKT_BEREGNING.minusYears(1).withDayOfMonth(1),
            SKJÆRINGSTIDSPUNKT_BEREGNING.withDayOfMonth(1).minusDays(1), 8L);

        // Act 3: fordel beregningsgrunnlag
        resultat = beregningTjenesteWrapper.getVurderBeregningsgrunnlagTjeneste().vurderBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, foreslåttBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));
        assertThat(resultat.getVilkårOppfylt()).isTrue();
        BeregningsgrunnlagEntitet fordeltBeregningsgrunnlag = fordelBeregningsgrunnlag(input,
            resultat.getBeregningsgrunnlag(), grunnlag);

        // Act 4: fastsette beregningsgrunnlag
        var foreslåttGrunnlag = nyttGrunnlag(grunnlag, fordeltBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT);
        var fastsattBeregningsgrunnlag = beregningTjenesteWrapper.getFullføreBeregningsgrunnlagTjeneste().fullføreBeregningsgrunnlag(input,
            foreslåttGrunnlag);

        // Assert 3
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(fastsattBeregningsgrunnlag, Hjemmel.F_14_7_8_40);

        periode = fastsattBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserBGATetterAvkorting(periode,
            ÅRSINNTEKT, forventetFordelt, virksomhetene, avkortetBG, forventetRedusert, forventetRedusert, forventetRedusertBrukersAndel, false);
        verdikjedeTestHjelper.verifiserFLetterAvkorting(periode, frilansÅrsinntekt, forventetFordeltFL, 0.0d, forventetBrukersAndelFL);
    }

    @Test
    public void bareFrilansMedBgUnder6g() {

        // Arrange
        final Double sammenligning = 12 * 14000d;
        final Double frilansÅrsinntekt = 12 * 14000d;

        double forventetBrukersAndelFL = frilansÅrsinntekt;

        BehandlingReferanse behandlingReferanse = lagBehandlingFL(scenario,
            BigDecimal.valueOf(sammenligning / 12),
            BigDecimal.valueOf(frilansÅrsinntekt / 12),
            ARBEIDSFORHOLD_ORGNR1);

        List<Inntektsmelding> inntektsmeldinger = List.of();

        var opptjeningPeriode = Periode.of(SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(10), SKJÆRINGSTIDSPUNKT_BEREGNING.minusDays(1));
        var opptjeningAktiviteter = new OpptjeningAktiviteter(
            OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.FRILANS, opptjeningPeriode)
            );

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId())).medInntektsmeldinger(inntektsmeldinger).build();
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(behandlingReferanse, iayGrunnlag, opptjeningAktiviteter, AktivitetGradering.INGEN_GRADERING, foreldrepengerGrunnlag);

        // Act 1: kontroller fakta om beregning
        BeregningsgrunnlagGrunnlagEntitet grunnlag = kjørStegOgLagreGrunnlag(input);
        input = input.medBeregningsgrunnlagGrunnlag(grunnlag);
        List<BeregningAksjonspunktResultat> aksjonspunktResultat = beregningTjenesteWrapper.getAksjonspunktUtlederFaktaOmBeregning().utledAksjonspunkterFor(input, grunnlag, false);

        // Assert 1
        assertThat(aksjonspunktResultat).hasSize(1);
        List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller = grunnlag.getBeregningsgrunnlag()
            .map(BeregningsgrunnlagEntitet::getFaktaOmBeregningTilfeller)
            .orElse(Collections.emptyList());
        assertThat(faktaOmBeregningTilfeller).hasSize(1);
        assertThat(faktaOmBeregningTilfeller.get(0)).isEqualTo(FaktaOmBeregningTilfelle.VURDER_MOTTAR_YTELSE);

        // Act 2: foreslå beregningsgrunnlag
        BeregningsgrunnlagRegelResultat resultat = beregningTjenesteWrapper.getForeslåBeregningsgrunnlagTjeneste().foreslåBeregningsgrunnlag(input, grunnlag);

        // Assert 2
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(resultat, Hjemmel.F_14_7_8_38);

        BeregningsgrunnlagEntitet foreslåttBeregningsgrunnlag = resultat.getBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periode = foreslåttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 1);
        verdikjedeTestHjelper.verifiserFLførAvkorting(periode, frilansÅrsinntekt);
        verdikjedeTestHjelper.verifiserSammenligningsgrunnlag(foreslåttBeregningsgrunnlag.getSammenligningsgrunnlag(),
            sammenligning, SKJÆRINGSTIDSPUNKT_BEREGNING.minusYears(1).withDayOfMonth(1),
            SKJÆRINGSTIDSPUNKT_BEREGNING.withDayOfMonth(1).minusDays(1), 0L);

        // Act 3: fordel beregningsgrunnlag
        resultat = beregningTjenesteWrapper.getVurderBeregningsgrunnlagTjeneste().vurderBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, foreslåttBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));
        assertThat(resultat.getVilkårOppfylt()).isTrue();
        BeregningsgrunnlagEntitet fordeltBeregningsgrunnlag = fordelBeregningsgrunnlag(input,
            resultat.getBeregningsgrunnlag(), grunnlag);

        // Act 4: fastsette beregningsgrunnlag
        var fastsattBeregningsgrunnlag = beregningTjenesteWrapper.getFullføreBeregningsgrunnlagTjeneste().fullføreBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, fordeltBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));

        // Assert 3
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(fastsattBeregningsgrunnlag, Hjemmel.F_14_7_8_38);

        periode = fastsattBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserFLetterAvkorting(periode, frilansÅrsinntekt, frilansÅrsinntekt, frilansÅrsinntekt, forventetBrukersAndelFL);
    }

    @Test
    public void bareFrilansMedBgOver6g() {

        final double sammenligning = 12 * 70000d;
        final double frilansÅrsinntekt = 12 * 70000d;

        double forventetBrukersAndelFL = Math.min(seksG, frilansÅrsinntekt);

        BehandlingReferanse behandlingReferanse = lagBehandlingFL(scenario,
            BigDecimal.valueOf(sammenligning / 12),
            BigDecimal.valueOf(frilansÅrsinntekt / 12),
            ARBEIDSFORHOLD_ORGNR1);

        // Arrange
        List<Inntektsmelding> inntektsmeldinger = List.of();
        var opptjeningPeriode = Periode.of(SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(10), SKJÆRINGSTIDSPUNKT_BEREGNING.minusDays(1));
        var opptjeningAktiviteter = new OpptjeningAktiviteter(
            OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.FRILANS, opptjeningPeriode)
            );

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId())).medInntektsmeldinger(inntektsmeldinger).build();
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(behandlingReferanse, iayGrunnlag, opptjeningAktiviteter, AktivitetGradering.INGEN_GRADERING, foreldrepengerGrunnlag);

        // Act 1: kontroller fakta om beregning
        BeregningsgrunnlagGrunnlagEntitet grunnlag = kjørStegOgLagreGrunnlag(input);
        input = input.medBeregningsgrunnlagGrunnlag(grunnlag);
        List<BeregningAksjonspunktResultat> aksjonspunktResultat = beregningTjenesteWrapper.getAksjonspunktUtlederFaktaOmBeregning().utledAksjonspunkterFor(input, grunnlag, false);

        // Assert 1
        assertThat(aksjonspunktResultat).hasSize(1);
        List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller = grunnlag.getBeregningsgrunnlag()
            .map(BeregningsgrunnlagEntitet::getFaktaOmBeregningTilfeller)
            .orElse(Collections.emptyList());
        assertThat(faktaOmBeregningTilfeller).hasSize(1);
        assertThat(faktaOmBeregningTilfeller.get(0)).isEqualTo(FaktaOmBeregningTilfelle.VURDER_MOTTAR_YTELSE);

        // Act 2: foreslå beregningsgrunnlag
        BeregningsgrunnlagRegelResultat resultat = beregningTjenesteWrapper.getForeslåBeregningsgrunnlagTjeneste().foreslåBeregningsgrunnlag(input, grunnlag);

        // Assert 2
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(resultat, Hjemmel.F_14_7_8_38);

        BeregningsgrunnlagEntitet foreslåttBeregningsgrunnlag = resultat.getBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periode = foreslåttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 1);
        verdikjedeTestHjelper.verifiserFLførAvkorting(periode, frilansÅrsinntekt);
        verdikjedeTestHjelper.verifiserSammenligningsgrunnlag(foreslåttBeregningsgrunnlag.getSammenligningsgrunnlag(),
            sammenligning, SKJÆRINGSTIDSPUNKT_BEREGNING.minusYears(1).withDayOfMonth(1),
            SKJÆRINGSTIDSPUNKT_BEREGNING.withDayOfMonth(1).minusDays(1), 0L);

        // Act 3: fordel beregningsgrunnlag
        resultat = beregningTjenesteWrapper.getVurderBeregningsgrunnlagTjeneste().vurderBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, foreslåttBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));
        assertThat(resultat.getVilkårOppfylt()).isTrue();
        BeregningsgrunnlagEntitet fordeltBeregningsgrunnlag = fordelBeregningsgrunnlag(input,
            resultat.getBeregningsgrunnlag(), grunnlag);

        // Act 4: fastsette beregningsgrunnlag
        var fastsattBeregningsgrunnlag = beregningTjenesteWrapper.getFullføreBeregningsgrunnlagTjeneste().fullføreBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, fordeltBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));

        // Assert 3
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(fastsattBeregningsgrunnlag, Hjemmel.F_14_7_8_38);

        periode = fastsattBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserFLetterAvkorting(periode, frilansÅrsinntekt, frilansÅrsinntekt, seksG, forventetBrukersAndelFL);
    }

    private BeregningsgrunnlagGrunnlagEntitet kjørStegOgLagreGrunnlag(BeregningsgrunnlagInput input) {
       return verdikjedeTestHjelper.kjørStegOgLagreGrunnlag(input, beregningTjenesteWrapper);
    }

    private BeregningsgrunnlagEntitet fordelBeregningsgrunnlag(BeregningsgrunnlagInput input,
                                                               BeregningsgrunnlagEntitet beregningsgrunnlag,
                                                               BeregningsgrunnlagGrunnlagEntitet grunnlag) {
        return beregningTjenesteWrapper.getFordelBeregningsgrunnlagTjeneste().fordelBeregningsgrunnlag(input.medBeregningsgrunnlagGrunnlag(grunnlag), beregningsgrunnlag);
    }

    private BehandlingReferanse lagBehandlingATogFL(AbstractTestScenario<?> scenario,
                                           BigDecimal inntektSammenligningsgrunnlag,
                                           List<BigDecimal> inntektBeregningsgrunnlag,
                                           BigDecimal inntektFrilans,
                                           List<String> virksomhetOrgnr) {
        LocalDate fraOgMed = MINUS_YEARS_1;
        LocalDate tilOgMed = fraOgMed.plusYears(1);

        InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseBuilder = scenario.getInntektArbeidYtelseScenarioTestBuilder().getKladd();

        verdikjedeTestHjelper.lagAktørArbeid(inntektArbeidYtelseBuilder, scenario.getSøkerAktørId(), Arbeidsgiver.virksomhet(DUMMY_ORGNR),
            fraOgMed, tilOgMed, ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER);

        virksomhetOrgnr
            .forEach(virksomhetEntitet -> verdikjedeTestHjelper.lagAktørArbeid(inntektArbeidYtelseBuilder, scenario.getSøkerAktørId(), Arbeidsgiver.virksomhet(virksomhetEntitet),
                fraOgMed, tilOgMed, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD));

        for (LocalDate dt = fraOgMed; dt.isBefore(tilOgMed); dt = dt.plusMonths(1)) {
            for (int i = 0; i < virksomhetOrgnr.size(); i++) {
                verdikjedeTestHjelper.lagInntektForArbeidsforhold(inntektArbeidYtelseBuilder,
                    scenario.getSøkerAktørId(),
                    dt, dt.plusMonths(1), inntektBeregningsgrunnlag.get(i),
                    Arbeidsgiver.virksomhet(virksomhetOrgnr.get(i)));
            }
            verdikjedeTestHjelper.lagInntektForSammenligning(inntektArbeidYtelseBuilder, scenario.getSøkerAktørId(), dt, dt.plusMonths(1), inntektSammenligningsgrunnlag,
                Arbeidsgiver.virksomhet(DUMMY_ORGNR));
            verdikjedeTestHjelper.lagInntektForArbeidsforhold(inntektArbeidYtelseBuilder, scenario.getSøkerAktørId(), dt, dt.plusMonths(1), inntektFrilans,
                Arbeidsgiver.virksomhet(DUMMY_ORGNR));
            verdikjedeTestHjelper.lagInntektForOpptjening(inntektArbeidYtelseBuilder, scenario.getSøkerAktørId(), dt, dt.plusMonths(1), inntektFrilans,
                DUMMY_ORGNR);
        }

        BehandlingReferanse behandlingReferanse = lagre(scenario);
        return behandlingReferanse;
    }

    private BehandlingReferanse lagBehandlingFL(AbstractTestScenario<?> scenario,
                                       BigDecimal inntektSammenligningsgrunnlag,
                                       BigDecimal inntektFrilans, String beregningVirksomhet) {
        LocalDate fraOgMed = MINUS_YEARS_1.withDayOfMonth(1);
        LocalDate tilOgMed = fraOgMed.plusYears(1);
        verdikjedeTestHjelper.initBehandlingFL(scenario, inntektSammenligningsgrunnlag, inntektFrilans, beregningVirksomhet, fraOgMed, tilOgMed);
        BehandlingReferanse behandlingReferanse = lagre(scenario);
        return behandlingReferanse;
    }

    private BehandlingReferanse lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider, iayTjeneste::lagreIayAggregat, iayTjeneste::lagreOppgittOpptjening)
            .medSkjæringstidspunkt(Skjæringstidspunkt.builder()
                .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT_BEREGNING)
                .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT_BEREGNING)
                .medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_BEREGNING)
                .medFørsteUttaksdato(SKJÆRINGSTIDSPUNKT_BEREGNING)
                .build());
    }

}
