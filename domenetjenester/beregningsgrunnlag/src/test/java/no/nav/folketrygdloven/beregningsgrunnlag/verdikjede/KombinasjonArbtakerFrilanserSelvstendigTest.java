package no.nav.folketrygdloven.beregningsgrunnlag.verdikjede;

import static no.nav.folketrygdloven.beregningsgrunnlag.verdikjede.BeregningsgrunnlagGrunnlagTestUtil.nyttGrunnlag;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collections;
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
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningSatsType;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;
import no.nav.k9.kodeverk.beregningsgrunnlag.Hjemmel;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;

public class KombinasjonArbtakerFrilanserSelvstendigTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT_BEREGNING = VerdikjedeTestHjelper.SKJÆRINGSTIDSPUNKT_OPPTJENING;

    private static final String ORGNR1 = "915933149";
    private static final String ORGNR2 = "974760673";
    private static final String ORGNR3 = "974761076";

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private RepositoryProvider repositoryProvider = Mockito.spy(new RepositoryProvider(repoRule.getEntityManager()));

    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private VerdikjedeTestHjelper verdikjedeTestHjelper = new VerdikjedeTestHjelper(new InntektsmeldingTjeneste(iayTjeneste));

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository = repositoryProvider.getBeregningsgrunnlagRepository();

    private long seksG;
    private Long gverdi;
    private TestScenarioBuilder scenario;

    private BeregningTjenesteWrapper beregningTjenesteWrapper;

    @Before
    public void setup() {
        beregningTjenesteWrapper = BeregningTjenesteProvider.provide(repoRule, iayTjeneste);

        scenario = TestScenarioBuilder.nyttScenario();
        gverdi = beregningsgrunnlagRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, SKJÆRINGSTIDSPUNKT_BEREGNING).getVerdi();
        seksG = gverdi * 6;
    }

    @Test
    public void toArbeidsforholdOgFrilansMedBgOver6gOgRefusjonUnder6G() {

        final List<Double> ÅRSINNTEKT = List.of(12 * 28000d, 12 * 14_000d);
        final List<Double> refusjonsKrav = List.of(12 * 20000d, 12 * 15_000d);
        final Double frilansÅrsinntekt = 12 * 23000d;

        final double årsinntekt1 = 4.0 * beregningsgrunnlagRepository.finnEksaktSats(BeregningSatsType.GSNITT, LocalDate.of(2014, Month.JANUARY, 1)).getVerdi();
        final double årsinntekt2 = 4.0 * beregningsgrunnlagRepository.finnEksaktSats(BeregningSatsType.GSNITT, LocalDate.of(2015, Month.JANUARY, 1)).getVerdi();
        final double årsinntekt3 = 4.0 * beregningsgrunnlagRepository.finnEksaktSats(BeregningSatsType.GSNITT, LocalDate.of(2016, Month.JANUARY, 1)).getVerdi();
        final List<Double> ÅRSINNTEKT_SN = List.of(årsinntekt1, årsinntekt2, årsinntekt3);
        final List<Integer> ÅR = List.of(2014, 2015, 2016);

        double forventetFlyttetTilArbeidsforhold = Math.max(refusjonsKrav.get(0) - ÅRSINNTEKT.get(0), 0L) + Math.max(refusjonsKrav.get(1) - ÅRSINNTEKT.get(1), 0L);
        List<Double> forventetFordelt = List.of(Math.max(refusjonsKrav.get(0), ÅRSINNTEKT.get(0)), Math.max(refusjonsKrav.get(1), ÅRSINNTEKT.get(1)));

        final double forventetBeregnetSN = BigDecimal.ZERO.max(BigDecimal.valueOf(4.0 * gverdi - (ÅRSINNTEKT.get(0) + ÅRSINNTEKT.get(1)))).doubleValue();

        boolean kanFlytteAltTilSN = (forventetBeregnetSN - forventetFlyttetTilArbeidsforhold) >= 0;
        double forventetFordeltSN = kanFlytteAltTilSN ? forventetBeregnetSN - forventetFlyttetTilArbeidsforhold : 0;
        double flyttesFraFL = forventetFlyttetTilArbeidsforhold - forventetBeregnetSN;
        double forventetFordeltFL = frilansÅrsinntekt - flyttesFraFL;

        double forventetRedusert1 = refusjonsKrav.get(0);
        double forventetRedusert2 = refusjonsKrav.get(1);

        double forventetRedusertFLogSN = Math.max(0, seksG - (forventetFordelt.stream().mapToDouble(Double::doubleValue).sum()));
        double forventetBrukersAndelFL = forventetRedusertFLogSN * forventetFordeltFL / (forventetFordeltFL + forventetFordeltSN);

        final double forventetAvkortetSN = forventetRedusertFLogSN * forventetFordeltSN / (forventetFordeltFL + forventetFordeltSN);
        final double forventetRedusertSN = forventetAvkortetSN;

        double forventetBrukersAndel1 = forventetFordelt.get(0) - forventetRedusert1;
        double forventetBrukersAndel2 = forventetFordelt.get(1) - forventetRedusert2;

        final List<Double> forventetRedusert = List.of(forventetRedusert1, forventetRedusert2);
        final List<Double> forventetRedusertBrukersAndel = List.of(forventetBrukersAndel1, forventetBrukersAndel2);

        List<BigDecimal> månedsinntekterAT = ÅRSINNTEKT.stream().map((v) -> BigDecimal.valueOf(v / 12)).collect(Collectors.toList());
        List<BigDecimal> årsinntekterSN = ÅRSINNTEKT_SN.stream().map(BigDecimal::valueOf).collect(Collectors.toList());

        List<String> virksomhetene = List.of(ORGNR1, ORGNR2);

        // Arrange
        verdikjedeTestHjelper.lagBehandlingATogFLogSN(scenario, månedsinntekterAT,
            virksomhetene,
            BigDecimal.valueOf(frilansÅrsinntekt / 12),
            årsinntekterSN,
            ÅR.get(0),
            null);
        BehandlingReferanse behandlingReferanse = lagre(scenario);

        var im1 = verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, Arbeidsgiver.virksomhet(ORGNR1), månedsinntekterAT.get(0),
            BigDecimal.valueOf(refusjonsKrav.get(0) / 12));
        var im2 = verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, Arbeidsgiver.virksomhet(ORGNR2), månedsinntekterAT.get(1),
            BigDecimal.valueOf(refusjonsKrav.get(1) / 12));
        var opptjeningPeriode = Periode.of(SKJÆRINGSTIDSPUNKT_BEREGNING.minusYears(1), SKJÆRINGSTIDSPUNKT_BEREGNING.minusDays(1));
        var opptjeningAktiviteter = new OpptjeningAktiviteter(
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.FRILANS, opptjeningPeriode, ORGNR3),
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, ORGNR1),
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, ORGNR2),
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.NÆRING, opptjeningPeriode, ORGNR3)
            );

        var iayGrunnlag =InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(behandlingReferanse.getId())).medInntektsmeldinger(im1, im2).build();
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(lagReferanse(behandlingReferanse), iayGrunnlag, opptjeningAktiviteter, AktivitetGradering.INGEN_GRADERING, foreldrepengerGrunnlag);

        // Act 1: kontroller fakta om beregning
        BeregningsgrunnlagGrunnlagEntitet grunnlag = kjørStegOgLagreGrunnlag(input);
        input = input.medBeregningsgrunnlagGrunnlag(grunnlag);
        List<BeregningAksjonspunktResultat> aksjonspunktResultat = beregningTjenesteWrapper.getAksjonspunktUtlederFaktaOmBeregning().utledAksjonspunkterFor(input, grunnlag,
            false);

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
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(resultat, Hjemmel.F_14_7_8_43);

        BeregningsgrunnlagEntitet foreslåttBeregningsgrunnlag = resultat.getBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periode = foreslåttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 4);
        verdikjedeTestHjelper.verifiserBGSNførAvkorting(periode, forventetFordeltSN, forventetBeregnetSN, 2016);
        assertThat(foreslåttBeregningsgrunnlag.getSammenligningsgrunnlag()).isNull();

        verdikjedeTestHjelper.verifiserBGATførAvkorting(periode, ÅRSINNTEKT, virksomhetene);
        verdikjedeTestHjelper.verifiserFLførAvkorting(periode, frilansÅrsinntekt);

        // Act 3: fordel beregningsgrunnlag
        resultat = beregningTjenesteWrapper.getVurderBeregningsgrunnlagTjeneste().vurderBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, foreslåttBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));
        assertThat(resultat.getVilkårOppfylt()).isTrue();
        BeregningsgrunnlagEntitet fordeltBeregningsgrunnlag = fordelBeregningsgrunnlag(input, resultat);

        // Act 4: fastsette beregningsgrunnlag
        var fastsattBeregningsgrunnlag = beregningTjenesteWrapper.getFullføreBeregningsgrunnlagTjeneste().fullføreBeregningsgrunnlag(input,
            BeregningsgrunnlagGrunnlagTestUtil.nyttGrunnlag(grunnlag, fordeltBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));

        // Assert 3
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(fastsattBeregningsgrunnlag, Hjemmel.F_14_7_8_43);

        periode = fastsattBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserBGSNetterAvkorting(periode, forventetBeregnetSN, forventetFordeltSN, forventetAvkortetSN, forventetRedusertSN, 2016);

        verdikjedeTestHjelper.verifiserBGATetterAvkorting(periode,
            ÅRSINNTEKT, forventetFordelt, virksomhetene, forventetFordelt, forventetRedusert, forventetRedusert, forventetRedusertBrukersAndel, false);
        verdikjedeTestHjelper.verifiserFLetterAvkorting(periode, frilansÅrsinntekt, forventetFordeltFL, forventetBrukersAndelFL, forventetBrukersAndelFL);
    }

    private BehandlingReferanse lagReferanse(BehandlingReferanse behandlingReferanse) {
        return behandlingReferanse.medSkjæringstidspunkt(
            Skjæringstidspunkt.builder()
                .medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_BEREGNING)
                .medSkjæringstidspunktBeregning(SKJÆRINGSTIDSPUNKT_BEREGNING)
                .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT_BEREGNING)
                .medFørsteUttaksdato(SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(1))
                .build());
    }

    @Test
    public void frilansOgSNMedBgFraArbeidsforholdUnder6G() {

        final List<Double> ÅRSINNTEKT = new ArrayList<>();
        final Double frilansÅrsinntekt = 12 * 23000d;

        final double årsinntekt1 = 4.0 * beregningsgrunnlagRepository.finnEksaktSats(BeregningSatsType.GSNITT, LocalDate.of(2014, Month.JANUARY, 1)).getVerdi();
        final double årsinntekt2 = 4.0 * beregningsgrunnlagRepository.finnEksaktSats(BeregningSatsType.GSNITT, LocalDate.of(2015, Month.JANUARY, 1)).getVerdi();
        final double årsinntekt3 = 4.0 * beregningsgrunnlagRepository.finnEksaktSats(BeregningSatsType.GSNITT, LocalDate.of(2016, Month.JANUARY, 1)).getVerdi();
        final List<Double> ÅRSINNTEKT_SN = List.of(årsinntekt1, årsinntekt2, årsinntekt3);
        final List<Integer> ÅR = List.of(2014, 2015, 2016);

        final double forventetBruttoSN = 4 * gverdi - frilansÅrsinntekt;

        double forventetBrukersAndelFL = frilansÅrsinntekt;

        final double forventetAvkortetSN = forventetBruttoSN;
        final double forventetRedusertSN = forventetAvkortetSN;

        final List<Double> forventetRedusert = new ArrayList<>();
        final List<Double> forventetRedusertBrukersAndel = new ArrayList<>();

        List<BigDecimal> månedsinntekterAT = ÅRSINNTEKT.stream().map((v) -> BigDecimal.valueOf(v / 12)).collect(Collectors.toList());
        List<BigDecimal> årsinntekterSN = ÅRSINNTEKT_SN.stream().map(BigDecimal::valueOf).collect(Collectors.toList());

        List<String> virksomhetene = new ArrayList<>();

        // Arrange 1
        verdikjedeTestHjelper.lagBehandlingATogFLogSN(scenario, månedsinntekterAT,
            virksomhetene,
            BigDecimal.valueOf(frilansÅrsinntekt / 12),
            årsinntekterSN,
            ÅR.get(0),
            null);

        BehandlingReferanse behandlingReferanse = lagre(scenario);

        var opptjeningPeriode = Periode.of(SKJÆRINGSTIDSPUNKT_BEREGNING.minusYears(1), SKJÆRINGSTIDSPUNKT_BEREGNING.minusDays(1));
        var opptjeningAktiviteter = new OpptjeningAktiviteter(
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.FRILANS, opptjeningPeriode, ORGNR3),
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.NÆRING, opptjeningPeriode, ORGNR3)
            );
        List<Inntektsmelding> inntektsmeldinger = List.of();

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId())).medInntektsmeldinger(inntektsmeldinger).build();
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(lagReferanse(behandlingReferanse), iayGrunnlag, opptjeningAktiviteter, AktivitetGradering.INGEN_GRADERING, foreldrepengerGrunnlag);

        // Act 1: kontroller fakta om beregning
        BeregningsgrunnlagGrunnlagEntitet grunnlag = kjørStegOgLagreGrunnlag(input);
        input = input.medBeregningsgrunnlagGrunnlag(grunnlag);
        List<BeregningAksjonspunktResultat> aksjonspunktResultat = beregningTjenesteWrapper.getAksjonspunktUtlederFaktaOmBeregning().utledAksjonspunkterFor(input, grunnlag,
            false);

        // Assert 1
        assertThat(aksjonspunktResultat).hasSize(1);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().orElse(null);
        List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller = beregningsgrunnlag.getFaktaOmBeregningTilfeller();
        assertThat(faktaOmBeregningTilfeller).hasSize(1);
        assertThat(faktaOmBeregningTilfeller.get(0)).isEqualTo(FaktaOmBeregningTilfelle.VURDER_MOTTAR_YTELSE);

        // Act 2: foreslå beregningsgrunnlag
        BeregningsgrunnlagRegelResultat resultat = beregningTjenesteWrapper.getForeslåBeregningsgrunnlagTjeneste().foreslåBeregningsgrunnlag(input, grunnlag);

        // Assert 2
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(resultat, Hjemmel.F_14_7_8_42);

        BeregningsgrunnlagEntitet foreslåttBeregningsgrunnlag = resultat.getBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periode = foreslåttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 2);
        verdikjedeTestHjelper.verifiserBGSNførAvkorting(periode, forventetBruttoSN, forventetBruttoSN, 2016);
        assertThat(beregningsgrunnlag.getSammenligningsgrunnlag()).isNull();

        verdikjedeTestHjelper.verifiserBGATførAvkorting(periode, ÅRSINNTEKT, virksomhetene);
        verdikjedeTestHjelper.verifiserFLførAvkorting(periode, frilansÅrsinntekt);

        // Act 3: fordel beregningsgrunnlag
        resultat = beregningTjenesteWrapper.getVurderBeregningsgrunnlagTjeneste().vurderBeregningsgrunnlag(input,
            nyttGrunnlag(grunnlag, foreslåttBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));
        assertThat(resultat.getVilkårOppfylt()).isTrue();
        BeregningsgrunnlagEntitet fordeltBeregningsgrunnlag = fordelBeregningsgrunnlag(input, resultat);

        // Act 4: fastsette beregningsgrunnlag
        var fastsattBeregningsgrunnlag = beregningTjenesteWrapper.getFullføreBeregningsgrunnlagTjeneste().fullføreBeregningsgrunnlag(input,
            BeregningsgrunnlagGrunnlagTestUtil.nyttGrunnlag(grunnlag, fordeltBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT));

        // Assert 3
        verdikjedeTestHjelper.verifiserBeregningsgrunnlagBasis(fastsattBeregningsgrunnlag, Hjemmel.F_14_7_8_42);

        periode = fastsattBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verdikjedeTestHjelper.verifiserBGSNetterAvkorting(periode, forventetBruttoSN, forventetBruttoSN, forventetAvkortetSN, forventetRedusertSN, 2016);

        verdikjedeTestHjelper.verifiserBGATetterAvkorting(periode,
            ÅRSINNTEKT, ÅRSINNTEKT, virksomhetene, ÅRSINNTEKT, forventetRedusert, forventetRedusert, forventetRedusertBrukersAndel, false);
        verdikjedeTestHjelper.verifiserFLetterAvkorting(periode, frilansÅrsinntekt, frilansÅrsinntekt, forventetBrukersAndelFL, forventetBrukersAndelFL);
    }

    private BeregningsgrunnlagGrunnlagEntitet kjørStegOgLagreGrunnlag(BeregningsgrunnlagInput input) {
        return verdikjedeTestHjelper.kjørStegOgLagreGrunnlag(input, beregningTjenesteWrapper);
    }

    private BeregningsgrunnlagEntitet fordelBeregningsgrunnlag(BeregningsgrunnlagInput input, BeregningsgrunnlagRegelResultat resultat) {
        return beregningTjenesteWrapper.getFordelBeregningsgrunnlagTjeneste().fordelBeregningsgrunnlag(input,
            resultat.getBeregningsgrunnlag());
    }

    private BehandlingReferanse lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider, iayTjeneste::lagreIayAggregat, iayTjeneste::lagreOppgittOpptjening);
    }
}
