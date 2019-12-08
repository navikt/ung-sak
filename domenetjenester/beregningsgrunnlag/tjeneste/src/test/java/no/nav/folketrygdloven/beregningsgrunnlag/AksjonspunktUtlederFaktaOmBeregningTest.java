package no.nav.folketrygdloven.beregningsgrunnlag;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import no.nav.folketrygdloven.beregningsgrunnlag.adapter.util.BeregningsgrunnlagTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.input.K9BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.FaktaOmBeregningTilfelleTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagTilstand;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.FaktaOmBeregningTilfelle;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Hjemmel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.PeriodeÅrsak;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningAksjonspunktDefinisjon;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningAksjonspunktResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningIAYTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningInntektsmeldingTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.AbstractTestScenario;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Gradering;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.felles.jpa.tid.ÅpenDatoIntervallEntitet;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class AksjonspunktUtlederFaktaOmBeregningTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, Month.MARCH, 23);
    private final InternArbeidsforholdRef arbId = InternArbeidsforholdRef.namedRef("A");
    private final String orgnr = "974760673";
    private final InternArbeidsforholdRef arbId2 = InternArbeidsforholdRef.namedRef("B");
    private final String orgnr2 = "974761424";

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();
    private RepositoryProvider repositoryProvider = Mockito.spy(new RepositoryProvider(entityManager));

    @Inject
    private FaktaOmBeregningTilfelleTjeneste faktaOmBeregningTilfelleTjeneste;

    private BeregningsgrunnlagAksjonspunktUtleder aksjonspunktUtlederFaktaOmBeregning;

    // Test utils
    private BeregningIAYTestUtil iayTestUtil;
    private BeregningsgrunnlagTestUtil beregningTestUtil;
    private BeregningInntektsmeldingTestUtil inntektsmeldingTestUtil;

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    private TestScenarioBuilder scenario;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository = repositoryProvider.getBeregningsgrunnlagRepository();
    private BeregningAktivitetAggregatEntitet.Builder beregningAktivitetBuilder = BeregningAktivitetAggregatEntitet.builder()
        .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING);
    private Arbeidsgiver arbeidsgiver;
    private Arbeidsgiver arbeidsgiver2;

    @Before
    public void setup() {
        aksjonspunktUtlederFaktaOmBeregning = new AksjonspunktUtlederFaktaOmBeregning(faktaOmBeregningTilfelleTjeneste);
        arbeidsgiver = Arbeidsgiver.virksomhet(orgnr);
        iayTestUtil = new BeregningIAYTestUtil(inntektArbeidYtelseTjeneste);
        beregningTestUtil = new BeregningsgrunnlagTestUtil(beregningsgrunnlagRepository, inntektArbeidYtelseTjeneste);
        inntektsmeldingTestUtil = new BeregningInntektsmeldingTestUtil(new InntektsmeldingTjeneste(inntektArbeidYtelseTjeneste));
    }

    private BehandlingReferanse lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider, inntektArbeidYtelseTjeneste::lagreIayAggregat, inntektArbeidYtelseTjeneste::lagreOppgittOpptjening).medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING);
    }

    private BehandlingReferanse lagBehandling() {
        lagScenario();
        return lagre(scenario).medSkjæringstidspunkt(Skjæringstidspunkt.builder()
            .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING)
            .medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING)
            .medSkjæringstidspunktBeregning(SKJÆRINGSTIDSPUNKT_OPPTJENING).build());
    }

    private void lagScenario() {
        scenario = TestScenarioBuilder.nyttScenario()
            .medDefaultInntektArbeidYtelse();
    }

    /**
     * orgnr gradering, orgnr2 med refusjon over 6G
     * SN ny i Arbeidslivet:
     */
    @Test
    public void skalUtledeAksjonspunktForSNNyIArbeidslivet() {
        // Arrange
        BehandlingReferanse behandlingReferanse = lagBehandling();
        LocalDate graderingStart = SKJÆRINGSTIDSPUNKT_OPPTJENING.plusWeeks(9);
        int refusjonskravAndel2 = 50000;

        HashMap<String, Periode> opptjeningMap = new HashMap<>();
        opptjeningMap.put(orgnr, Periode.månederFør(SKJÆRINGSTIDSPUNKT_OPPTJENING, 12));
        opptjeningMap.put(orgnr2, Periode.månederFør(SKJÆRINGSTIDSPUNKT_OPPTJENING, 12));
        iayTestUtil.lagOppgittOpptjeningForSN(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, true);
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(10), arbId, arbeidsgiver);
        arbeidsgiver2 = Arbeidsgiver.virksomhet(orgnr2);
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(1), arbId2, arbeidsgiver2);
        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING)
            .medGrunnbeløp(BigDecimal.valueOf(GrunnbeløpTestKonstanter.GRUNNBELØP_2018))
            .build();
        BeregningsgrunnlagAktivitetStatus.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medHjemmel(Hjemmel.F_14_7)
            .build(beregningsgrunnlag);
        BeregningsgrunnlagAktivitetStatus.builder()
            .medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medHjemmel(Hjemmel.F_14_7)
            .build(beregningsgrunnlag);

        BeregningsgrunnlagPeriode beregningsgrunnlagPeriode1 = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT_OPPTJENING, graderingStart.minusDays(1))
            .build(beregningsgrunnlag);

        leggTilAndel(beregningsgrunnlagPeriode1, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, null, 0);
        leggTilAndel(beregningsgrunnlagPeriode1, AktivitetStatus.ARBEIDSTAKER, orgnr, 0);
        leggTilAndel(beregningsgrunnlagPeriode1, AktivitetStatus.ARBEIDSTAKER, orgnr2, refusjonskravAndel2 * 12);

        BeregningsgrunnlagPeriode beregningsgrunnlagPeriode2 = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(graderingStart, null)
            .leggTilPeriodeÅrsak(PeriodeÅrsak.GRADERING)
            .build(beregningsgrunnlag);
        leggTilAndel(beregningsgrunnlagPeriode2, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, null, 0);
        leggTilAndel(beregningsgrunnlagPeriode2, AktivitetStatus.ARBEIDSTAKER, orgnr, 0);
        leggTilAndel(beregningsgrunnlagPeriode2, AktivitetStatus.ARBEIDSTAKER, orgnr2, refusjonskravAndel2 * 12);

        Gradering gradering = new Gradering(graderingStart, null, BigDecimal.valueOf(50));
        inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, orgnr, arbId, SKJÆRINGSTIDSPUNKT_OPPTJENING, singletonList(gradering),
            LocalDateTime.of(SKJÆRINGSTIDSPUNKT_OPPTJENING, LocalTime.MIDNIGHT));
        inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, orgnr2, arbId2, SKJÆRINGSTIDSPUNKT_OPPTJENING, 50000,
            LocalDateTime.of(SKJÆRINGSTIDSPUNKT_OPPTJENING, LocalTime.MIDNIGHT).plusSeconds(1));
        leggTilAktivitet(arbId, orgnr, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(10));
        leggTilAktivitet(arbId2, orgnr2, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(1));

        // Act
        BeregningsgrunnlagGrunnlagEntitet grunnlag = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .medRegisterAktiviteter(beregningAktivitetBuilder.build())
            .build(behandlingReferanse.getId(), BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
        var input = lagInput(behandlingReferanse);
        List<BeregningAksjonspunktResultat> resultat = aksjonspunktUtlederFaktaOmBeregning.utledAksjonspunkterFor(input, grunnlag, false);

        // Assert
        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).getBeregningAksjonspunktDefinisjon()).isEqualTo(BeregningAksjonspunktDefinisjon.VURDER_FAKTA_FOR_ATFL_SN);
        List<FaktaOmBeregningTilfelle> tilfeller = beregningsgrunnlag.getFaktaOmBeregningTilfeller();
        assertThat(tilfeller).containsExactlyInAnyOrder(FaktaOmBeregningTilfelle.VURDER_SN_NY_I_ARBEIDSLIVET);
    }

    private BeregningsgrunnlagInput lagInput(BehandlingReferanse ref) {
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        return new BeregningsgrunnlagInput(ref, inntektArbeidYtelseTjeneste.hentGrunnlag(ref.getBehandlingId()), null, AktivitetGradering.INGEN_GRADERING, foreldrepengerGrunnlag);
    }

    @Test
    public void skalUtledeAksjonspunktATFLSammeOrgLønnsendringNyoppstartetFL() {
        // Arrange
        BehandlingReferanse behandlingReferanse = lagBehandling();
        var arbId3 = InternArbeidsforholdRef.namedRef("3");
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(orgnr);
        final String orgnr3 = "567755757";
        HashMap<String, Periode> opptjeningMap = new HashMap<>();
        Periode periode = Periode.månederFør(SKJÆRINGSTIDSPUNKT_OPPTJENING, 12);
        opptjeningMap.put(orgnr, periode);
        opptjeningMap.put(orgnr3, periode);
        iayTestUtil.leggTilOppgittOpptjeningForFL(behandlingReferanse, true, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2));
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(10), arbId, arbeidsgiver, Optional.of(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2L)));
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(10), arbId3, Arbeidsgiver.virksomhet(orgnr3));
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(5).minusDays(2), (InternArbeidsforholdRef) null, arbeidsgiver,
            ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER, singletonList(BigDecimal.TEN), false, Optional.empty());
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING,
            AktivitetStatus.KOMBINERT_AT_FL);
        beregningTestUtil.leggTilFLTilknyttetOrganisasjon(behandlingReferanse, orgnr3, arbId3);

        // Act
        BeregningsgrunnlagGrunnlagBuilder grunnlagBuilder = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medRegisterAktiviteter(BeregningAktivitetAggregatEntitet.builder()
                .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING)
                .leggTilAktivitet(lagBeregningAktivitetArbeid(periode, arbeidsgiver, arbId))
                .leggTilAktivitet(lagBeregningAktivitetArbeid(periode, Arbeidsgiver.virksomhet(orgnr3), InternArbeidsforholdRef.nullRef()))
                .leggTilAktivitet(lagBeregningAktivitetFL(periode))
                .build())
            .medBeregningsgrunnlag(beregningsgrunnlag);
        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), grunnlagBuilder, BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = grunnlagBuilder.build(behandlingReferanse.getId(), BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
        var input = lagInput(behandlingReferanse);
        List<BeregningAksjonspunktResultat> resultat = aksjonspunktUtlederFaktaOmBeregning.utledAksjonspunkterFor(input, grunnlag, false);

        // Assert
        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).getBeregningAksjonspunktDefinisjon()).isEqualTo(BeregningAksjonspunktDefinisjon.VURDER_FAKTA_FOR_ATFL_SN);
        List<FaktaOmBeregningTilfelle> tilfeller = beregningsgrunnlag.getFaktaOmBeregningTilfeller();
        assertThat(tilfeller).containsExactlyInAnyOrder(
            FaktaOmBeregningTilfelle.VURDER_AT_OG_FL_I_SAMME_ORGANISASJON,
            FaktaOmBeregningTilfelle.VURDER_LØNNSENDRING,
            FaktaOmBeregningTilfelle.VURDER_NYOPPSTARTET_FL,
            FaktaOmBeregningTilfelle.VURDER_MOTTAR_YTELSE);
    }

    /**
     * orgnr har gradering fra og med STP+2 uker
     * orgnr2 er kortvarig arbeidsforhold med slutt STP+1 måned, søker refusjon
     */
    @Test
    public void skalUtledeAksjonspunktKortvarigeArbeidsforhold() {
        // Arrange
        BehandlingReferanse behandlingReferanse = lagBehandling();
        LocalDate graderingStart = SKJÆRINGSTIDSPUNKT_OPPTJENING.plusWeeks(9);
        int refusjonskravAndel2 = 50000;

        HashMap<String, Periode> opptjeningMap = new HashMap<>();
        opptjeningMap.put(orgnr, Periode.månederFør(SKJÆRINGSTIDSPUNKT_OPPTJENING, 12));
        opptjeningMap.put(orgnr2, Periode.månederFør(SKJÆRINGSTIDSPUNKT_OPPTJENING, 12));
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(10), arbId, Arbeidsgiver.virksomhet(orgnr));
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(1), arbId2, Arbeidsgiver.virksomhet(orgnr2));
        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING)
            .medGrunnbeløp(BigDecimal.valueOf(GrunnbeløpTestKonstanter.GRUNNBELØP_2018))
            .build();
        BeregningsgrunnlagAktivitetStatus.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medHjemmel(Hjemmel.F_14_7)
            .build(beregningsgrunnlag);

        BeregningsgrunnlagPeriode beregningsgrunnlagPeriode1 = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT_OPPTJENING, graderingStart.minusDays(1))
            .build(beregningsgrunnlag);

        leggTilAndel(beregningsgrunnlagPeriode1, AktivitetStatus.ARBEIDSTAKER, orgnr, 0);
        leggTilAndel(beregningsgrunnlagPeriode1, AktivitetStatus.ARBEIDSTAKER, orgnr2, refusjonskravAndel2 * 12);

        BeregningsgrunnlagPeriode beregningsgrunnlagPeriode2 = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(graderingStart, null)
            .leggTilPeriodeÅrsak(PeriodeÅrsak.GRADERING)
            .build(beregningsgrunnlag);
        leggTilAndel(beregningsgrunnlagPeriode2, AktivitetStatus.ARBEIDSTAKER, orgnr, 0);
        leggTilAndel(beregningsgrunnlagPeriode2, AktivitetStatus.ARBEIDSTAKER, orgnr2, refusjonskravAndel2 * 12);

        Gradering gradering = new Gradering(graderingStart, null, BigDecimal.valueOf(50));
        inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, orgnr, arbId, SKJÆRINGSTIDSPUNKT_OPPTJENING, singletonList(gradering),
            LocalDateTime.of(SKJÆRINGSTIDSPUNKT_OPPTJENING, LocalTime.MIDNIGHT));
        inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, orgnr2, arbId2, SKJÆRINGSTIDSPUNKT_OPPTJENING, refusjonskravAndel2,
            LocalDateTime.of(SKJÆRINGSTIDSPUNKT_OPPTJENING, LocalTime.MIDNIGHT).plusSeconds(1));
        leggTilAktivitet(arbId, orgnr, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(10));
        leggTilAktivitet(arbId2, orgnr2, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(1));

        // Act
        BeregningsgrunnlagGrunnlagEntitet grunnlag = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .medRegisterAktiviteter(beregningAktivitetBuilder.build())
            .build(behandlingReferanse.getId(), BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
        var input = lagInput(behandlingReferanse);
        List<BeregningAksjonspunktResultat> resultat = aksjonspunktUtlederFaktaOmBeregning.utledAksjonspunkterFor(input, grunnlag, false);

        // Assert
        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).getBeregningAksjonspunktDefinisjon()).isEqualTo(BeregningAksjonspunktDefinisjon.VURDER_FAKTA_FOR_ATFL_SN);
        List<FaktaOmBeregningTilfelle> tilfeller = beregningsgrunnlag.getFaktaOmBeregningTilfeller();
        assertThat(tilfeller).containsExactlyInAnyOrder(
            FaktaOmBeregningTilfelle.VURDER_TIDSBEGRENSET_ARBEIDSFORHOLD);
    }

    private void leggTilAndel(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode, AktivitetStatus aktivitetStatus, String orgnr, int refusjonskravPrÅr) {
        BeregningsgrunnlagPrStatusOgAndel.Builder andelBuilder = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(aktivitetStatus);
        if (orgnr != null) {
            BGAndelArbeidsforhold.Builder bgAndelArbeidsforholdBuilder = BGAndelArbeidsforhold.builder()
                .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr))
                .medRefusjonskravPrÅr(BigDecimal.valueOf(refusjonskravPrÅr));
            andelBuilder.medBGAndelArbeidsforhold(bgAndelArbeidsforholdBuilder);
        }
        andelBuilder.build(beregningsgrunnlagPeriode);
    }

    private void leggTilAktivitet(InternArbeidsforholdRef ref, String orgnr, LocalDate fom, LocalDate tom) {
        beregningAktivitetBuilder.leggTilAktivitet(BeregningAktivitetEntitet.builder()
            .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr))
            .medArbeidsforholdRef(ref).build());
    }

    @Test
    public void skalUtledeAksjonspunktForFellesTilfeller() {
        // Act
        BehandlingReferanse behandlingReferanse = lagBehandling();
        iayTestUtil.lagOppgittOpptjeningForSN(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, true);
        HashMap<String, Periode> opptjeningMap = new HashMap<>();
        Periode periode = Periode.månederFør(SKJÆRINGSTIDSPUNKT_OPPTJENING, 12);
        opptjeningMap.put(orgnr, periode);
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(3), arbId, Arbeidsgiver.virksomhet(orgnr));
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING,
            AktivitetStatus.KOMBINERT_AT_SN, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);

        // Act
        BeregningsgrunnlagGrunnlagBuilder grunnlagBuilder = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medRegisterAktiviteter(BeregningAktivitetAggregatEntitet.builder()
                .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING)
                .leggTilAktivitet(lagBeregningAktivitetArbeid(periode, arbeidsgiver, arbId))
                .leggTilAktivitet(lagBeregningAktivitetSN(periode))
                .build())
            .medBeregningsgrunnlag(beregningsgrunnlag);
        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), grunnlagBuilder, BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = grunnlagBuilder.build(behandlingReferanse.getId(), BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
        var input = lagInput(behandlingReferanse);
        List<BeregningAksjonspunktResultat> resultater = aksjonspunktUtlederFaktaOmBeregning.utledAksjonspunkterFor(input, grunnlag, false);

        // Assert
        assertThat(resultater).hasSize(1);
        assertThat(resultater)
            .anySatisfy(resultat -> assertThat(resultat.getBeregningAksjonspunktDefinisjon()).isEqualTo(BeregningAksjonspunktDefinisjon.VURDER_FAKTA_FOR_ATFL_SN));
    }

    @Test
    public void skalUtledeAksjonspunktForFellesTilfellerOgReturnereOverstyring() {
        // Act
        BehandlingReferanse behandlingReferanse = lagBehandling();
        iayTestUtil.lagOppgittOpptjeningForSN(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, true);
        HashMap<String, Periode> opptjeningMap = new HashMap<>();
        Periode periode = Periode.månederFør(SKJÆRINGSTIDSPUNKT_OPPTJENING, 12);
        opptjeningMap.put(orgnr, periode);
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(3), arbId, Arbeidsgiver.virksomhet(orgnr));
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING,
            AktivitetStatus.KOMBINERT_AT_SN, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);

        // Act
        BeregningsgrunnlagGrunnlagBuilder grunnlagBuilder = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medRegisterAktiviteter(BeregningAktivitetAggregatEntitet.builder()
                .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING)
                .leggTilAktivitet(lagBeregningAktivitetArbeid(periode, arbeidsgiver, arbId))
                .leggTilAktivitet(lagBeregningAktivitetSN(periode))
                .build())
            .medBeregningsgrunnlag(beregningsgrunnlag);
        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), grunnlagBuilder, BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = grunnlagBuilder.build(behandlingReferanse.getId(), BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
        var input = lagInput(behandlingReferanse);
        List<BeregningAksjonspunktResultat> resultater = aksjonspunktUtlederFaktaOmBeregning.utledAksjonspunkterFor(input, grunnlag, true);

        // Assert
        assertThat(resultater).hasSize(1);
        assertThat(resultater)
            .anySatisfy(resultat -> assertThat(resultat.getBeregningAksjonspunktDefinisjon()).isEqualTo(BeregningAksjonspunktDefinisjon.OVERSTYRING_AV_BEREGNINGSGRUNNLAG));
        assertThat(grunnlag.getBeregningsgrunnlag().get()
            .getFaktaOmBeregningTilfeller()).containsExactly(FaktaOmBeregningTilfelle.VURDER_SN_NY_I_ARBEIDSLIVET);

    }

    private BeregningAktivitetEntitet lagBeregningAktivitetArbeid(Periode periode, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbId) {
        return BeregningAktivitetEntitet.builder().medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom()))
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdRef(arbId)
            .build();
    }

    private BeregningAktivitetEntitet lagBeregningAktivitetSN(Periode periode) {
        return BeregningAktivitetEntitet.builder().medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom()))
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.NÆRING)
            .build();
    }

    private BeregningAktivitetEntitet lagBeregningAktivitetFL(Periode periode) {
        return BeregningAktivitetEntitet.builder().medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom()))
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.FRILANS)
            .build();
    }

    @Test
    public void skalReturnereIngenAksjonspunkter() {
        // Arrange
        BehandlingReferanse behandlingReferanse = lagBehandling();
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING,
            AktivitetStatus.ARBEIDSTAKER);

        // Act
        BeregningsgrunnlagGrunnlagEntitet grunnlag = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty()).medBeregningsgrunnlag(beregningsgrunnlag)
            .medRegisterAktiviteter(BeregningAktivitetAggregatEntitet.builder().medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING).build())
            .build(behandlingReferanse.getId(), BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);

        var input = lagInput(behandlingReferanse);
        List<BeregningAksjonspunktResultat> resultater = aksjonspunktUtlederFaktaOmBeregning .utledAksjonspunkterFor(input, grunnlag, false);

        // Assert
        assertThat(resultater).isEmpty();
    }
}
