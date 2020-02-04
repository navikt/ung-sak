package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.input.K9BeregningsgrunnlagInput;
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
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.AndelMedBeløpDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.KunYtelseDto;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.beregningsgrunnlag.BeregningAktivitetTestUtil;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.tid.ÅpenDatoIntervallEntitet;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.kodeverk.iay.AktivitetStatus;
import no.nav.k9.kodeverk.iay.Inntektskategori;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;

public class KunYtelseDtoTjenesteTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, Month.MAY, 10);
    private static final int BRUTTO_PR_ÅR = 10000;

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private RepositoryProvider repositoryProvider = new RepositoryProvider(repoRule.getEntityManager());
    private final BeregningsgrunnlagRepository beregningsgrunnlagRepository = new BeregningsgrunnlagRepository(repoRule.getEntityManager());

    private BehandlingReferanse behandlingReferanse;
    private KunYtelseDtoTjeneste kunYtelseDtoTjeneste;

    @Before
    public void setUp() {
        BeregningsgrunnlagDtoUtil beregningsgrunnlagDtoUtil = new BeregningsgrunnlagDtoUtil(beregningsgrunnlagRepository, null);
        this.kunYtelseDtoTjeneste = new KunYtelseDtoTjeneste(beregningsgrunnlagRepository, beregningsgrunnlagDtoUtil);
    }

    @Test
    public void fødende_kvinne_uten_dagpenger() {
        // Arrange
        ÅpenDatoIntervallEntitet periode = ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(
            SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(8),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1));
        BeregningAktivitetAggregatEntitet beregningAktivitetAggregat = BeregningAktivitetTestUtil.opprettBeregningAktiviteter(SKJÆRINGSTIDSPUNKT_OPPTJENING, periode, OpptjeningAktivitetType.SYKEPENGER);
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = scenario.lagre(repositoryProvider);
        var beregningsgrunnlagGrunnlag = lagBeregningsgrunnlag(scenario, beregningAktivitetAggregat);
        K9BeregningsgrunnlagInput medBesteberegning = new K9BeregningsgrunnlagInput();

        // Act
        var input = new BeregningsgrunnlagInput(behandlingReferanse, InntektArbeidYtelseGrunnlagBuilder.nytt().build(), null, null, medBesteberegning)
                .medBeregningsgrunnlagGrunnlag(beregningsgrunnlagGrunnlag);

        KunYtelseDto kunytelse = kunYtelseDtoTjeneste.lagKunYtelseDto(input);

        // Assert
        assertAndel(kunytelse);
    }

    @Test
    public void fødende_kvinne_med_dagpenger() {
        // Arrange
        BeregningAktivitetAggregatEntitet beregningAktivitetAggregat = beregningAktivitetSykepengerOgDagpenger();
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = scenario.lagre(repositoryProvider);
        var beregningsgrunnlagGrunnlag = lagBeregningsgrunnlag(scenario, beregningAktivitetAggregat);
        K9BeregningsgrunnlagInput medBesteberegning = new K9BeregningsgrunnlagInput();

        // Act
        var input = new BeregningsgrunnlagInput(behandlingReferanse, InntektArbeidYtelseGrunnlagBuilder.nytt().build(), null, null, medBesteberegning)
                .medBeregningsgrunnlagGrunnlag(beregningsgrunnlagGrunnlag);

        KunYtelseDto kunytelse = kunYtelseDtoTjeneste.lagKunYtelseDto(input);

        // Assert
        assertAndel(kunytelse);
    }

    @Test
    public void adopsjon_kvinne_med_dagpenger() {
        // Arrange
        BeregningAktivitetAggregatEntitet beregningAktivitetAggregat = beregningAktivitetSykepengerOgDagpenger();
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = scenario.lagre(repositoryProvider);

        var beregningsgrunnlagGrunnlag = lagBeregningsgrunnlag(scenario, beregningAktivitetAggregat);
        K9BeregningsgrunnlagInput utenBesteberegning = new K9BeregningsgrunnlagInput();

        // Act
        var input = new BeregningsgrunnlagInput(behandlingReferanse, InntektArbeidYtelseGrunnlagBuilder.nytt().build(), null, null, utenBesteberegning)
                .medBeregningsgrunnlagGrunnlag(beregningsgrunnlagGrunnlag);

        KunYtelseDto kunytelse = kunYtelseDtoTjeneste.lagKunYtelseDto(input);

        // Assert
        assertAndel(kunytelse);
    }

    private BeregningAktivitetAggregatEntitet beregningAktivitetSykepengerOgDagpenger() {
        BeregningAktivitetAggregatEntitet.Builder builder = BeregningAktivitetAggregatEntitet.builder()
            .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING);
        builder.leggTilAktivitet(BeregningAktivitetEntitet.builder()
            .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(8), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1)))
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.SYKEPENGER)
            .build());
        builder.leggTilAktivitet(BeregningAktivitetEntitet.builder()
            .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(12), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(8).minusDays(1)))
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.DAGPENGER)
            .build());
        return builder.build();
    }

    @Test
    public void mann_med_dagpenger() {
        // Arrange
        BeregningAktivitetAggregatEntitet beregningAktivitetAggregat = beregningAktivitetSykepengerOgDagpenger();
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenarioFar();
        behandlingReferanse = scenario.lagre(repositoryProvider);
        var beregningsgrunnlagGrunnlag = lagBeregningsgrunnlag(scenario, beregningAktivitetAggregat);
        K9BeregningsgrunnlagInput utenBesteberegning = new K9BeregningsgrunnlagInput();

        // Act
        var input = new BeregningsgrunnlagInput(behandlingReferanse, InntektArbeidYtelseGrunnlagBuilder.nytt().build(), null, null, utenBesteberegning)
                .medBeregningsgrunnlagGrunnlag(beregningsgrunnlagGrunnlag);

        KunYtelseDto kunytelse = kunYtelseDtoTjeneste.lagKunYtelseDto(input);

        // Assert
        assertAndel(kunytelse);
    }

    @Test
    public void skal_sette_verdier_om_forrige_grunnlag_var_kun_ytelse() {
        // Arrange
        BeregningAktivitetAggregatEntitet beregningAktivitetAggregat = beregningAktivitetSykepengerOgDagpenger();
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = scenario.lagre(repositoryProvider);
        var beregningsgrunnlagGrunnlag = lagForrigeBeregningsgrunnlagMedLagtTilAndel(scenario, beregningAktivitetAggregat);
        K9BeregningsgrunnlagInput utenBesteberegning = new K9BeregningsgrunnlagInput();
        // Act
        var input = new BeregningsgrunnlagInput(behandlingReferanse, InntektArbeidYtelseGrunnlagBuilder.nytt().build(), null, null, utenBesteberegning)
                .medBeregningsgrunnlagGrunnlag(beregningsgrunnlagGrunnlag);

        KunYtelseDto kunytelse = kunYtelseDtoTjeneste.lagKunYtelseDto(input);

        // Assert
        List<AndelMedBeløpDto> andeler = kunytelse.getAndeler();
        assertThat(andeler).hasSize(2);
        assertThat(andeler.get(0).getAktivitetStatus()).isEqualTo(AktivitetStatus.BRUKERS_ANDEL);
        assertThat(andeler.get(0).getAndelsnr()).isEqualTo(1L);
        assertThat(andeler.get(0).getInntektskategori()).isEqualTo(Inntektskategori.ARBEIDSTAKER);
        assertThat(andeler.get(1).getAktivitetStatus()).isEqualTo(AktivitetStatus.BRUKERS_ANDEL);
        assertThat(andeler.get(1).getAndelsnr()).isEqualTo(2L);
        assertThat(andeler.get(1).getInntektskategori()).isEqualTo(Inntektskategori.FRILANSER);
    }

    @Test
    public void skal_ikkje_sette_verdier_om_forrige_grunnlag_ikkje_var_kun_ytelse() {
        // Arrange
        BeregningAktivitetAggregatEntitet beregningAktivitetAggregat = beregningAktivitetSykepengerOgDagpenger();
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = scenario.lagre(repositoryProvider);
        var beregningsgrunnlagGrunnlag  = lagForrigeBeregningsgrunnlag(true, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet forrigeBg = lagForrigeBeregningsgrunnlagMUtenKunYtelse(scenario);
        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), forrigeBg, BeregningsgrunnlagTilstand.KOFAKBER_UT);
        K9BeregningsgrunnlagInput medBesteberegning = new K9BeregningsgrunnlagInput();

        // Act
        var input = new BeregningsgrunnlagInput(behandlingReferanse, InntektArbeidYtelseGrunnlagBuilder.nytt().build(), null, null, medBesteberegning)
                .medBeregningsgrunnlagGrunnlag(beregningsgrunnlagGrunnlag);

        KunYtelseDto kunytelse = kunYtelseDtoTjeneste.lagKunYtelseDto(input);

        // Assert
        List<AndelMedBeløpDto> andeler = kunytelse.getAndeler();
        assertThat(andeler).hasSize(1);
        assertThat(andeler.get(0).getAktivitetStatus()).isEqualTo(AktivitetStatus.BRUKERS_ANDEL);
        assertThat(andeler.get(0).getAndelsnr()).isEqualTo(1L);
        assertThat(andeler.get(0).getInntektskategori()).isEqualTo(Inntektskategori.UDEFINERT);
    }

    @Test
    public void skal_sette_verdier_fra_forrige_med_besteberegning() {
        // Arrange
        ÅpenDatoIntervallEntitet periode = ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(
            SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(8),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1));
        BeregningAktivitetAggregatEntitet beregningAktivitetAggregat = BeregningAktivitetTestUtil.opprettBeregningAktiviteter(SKJÆRINGSTIDSPUNKT_OPPTJENING, periode, OpptjeningAktivitetType.SYKEPENGER);
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = scenario.lagre(repositoryProvider);
        var beregningsgrunnlagGrunnlag  = lagForrigeBeregningsgrunnlag(true, beregningAktivitetAggregat);
        K9BeregningsgrunnlagInput medBesteberegning = new K9BeregningsgrunnlagInput();

        // Act
        var input = new BeregningsgrunnlagInput(behandlingReferanse, InntektArbeidYtelseGrunnlagBuilder.nytt().build(), null, null, medBesteberegning)
                .medBeregningsgrunnlagGrunnlag(beregningsgrunnlagGrunnlag);

        KunYtelseDto kunytelse = kunYtelseDtoTjeneste.lagKunYtelseDto(input);


        // Assert
        List<AndelMedBeløpDto> andeler = kunytelse.getAndeler();
        assertThat(andeler).hasSize(1);
        assertThat(andeler.get(0).getFastsattBelopPrMnd()).isNotEqualByComparingTo(BigDecimal.valueOf(BRUTTO_PR_ÅR));
        assertThat(andeler.get(0).getAktivitetStatus()).isEqualTo(AktivitetStatus.BRUKERS_ANDEL);
        assertThat(andeler.get(0).getAndelsnr()).isEqualTo(1L);
        assertThat(andeler.get(0).getInntektskategori()).isEqualTo(Inntektskategori.UDEFINERT);
    }

    @Test
    public void skal_sette_verdier_fra_forrige_uten_besteberegning() {
        // Arrange
        ÅpenDatoIntervallEntitet periode = ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(
            SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(8),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1));
        BeregningAktivitetAggregatEntitet beregningAktivitetAggregat = BeregningAktivitetTestUtil.opprettBeregningAktiviteter(SKJÆRINGSTIDSPUNKT_OPPTJENING, periode, OpptjeningAktivitetType.SYKEPENGER);
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = scenario.lagre(repositoryProvider);
        var beregningsgrunnlagGrunnlag  = lagForrigeBeregningsgrunnlag(false, beregningAktivitetAggregat);
        K9BeregningsgrunnlagInput utenBesteberegning = new K9BeregningsgrunnlagInput();

        // Act
        var input = new BeregningsgrunnlagInput(behandlingReferanse, InntektArbeidYtelseGrunnlagBuilder.nytt().build(), null, null, utenBesteberegning)
                .medBeregningsgrunnlagGrunnlag(beregningsgrunnlagGrunnlag);

        KunYtelseDto kunytelse = kunYtelseDtoTjeneste.lagKunYtelseDto(input);


        // Assert
        List<AndelMedBeløpDto> andeler = kunytelse.getAndeler();
        assertThat(andeler).hasSize(1);
        assertThat(andeler.get(0).getFastsattBelopPrMnd()).isNotEqualByComparingTo(BigDecimal.valueOf(BRUTTO_PR_ÅR));
        assertThat(andeler.get(0).getAktivitetStatus()).isEqualTo(AktivitetStatus.BRUKERS_ANDEL);
        assertThat(andeler.get(0).getAndelsnr()).isEqualTo(1L);
        assertThat(andeler.get(0).getInntektskategori()).isEqualTo(Inntektskategori.UDEFINERT);
    }


    private void assertAndel(KunYtelseDto kunytelse) {
        List<AndelMedBeløpDto> andeler = kunytelse.getAndeler();
        assertThat(andeler).hasSize(1);
        assertThat(andeler.get(0).getFastsattBelopPrMnd()).isNull();
        assertThat(andeler.get(0).getAktivitetStatus()).isEqualTo(AktivitetStatus.BRUKERS_ANDEL);
        assertThat(andeler.get(0).getAndelsnr()).isEqualTo(1L);
        assertThat(andeler.get(0).getInntektskategori()).isEqualTo(Inntektskategori.UDEFINERT);
    }

    private BeregningsgrunnlagGrunnlagEntitet lagForrigeBeregningsgrunnlag(boolean medBesteberegning, BeregningAktivitetAggregatEntitet beregningAktivitetAggregat) {
        BeregningsgrunnlagEntitet bg = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(LocalDate.now().minusDays(5))
            .medGrunnbeløp(BigDecimal.valueOf(90000))
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.KUN_YTELSE))
            .build();
        BeregningsgrunnlagPeriode periode1 = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT_OPPTJENING, null)
            .build(bg);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medLagtTilAvSaksbehandler(false)
            .medBesteberegningPrÅr(medBesteberegning ? BigDecimal.valueOf(BRUTTO_PR_ÅR) : null)
            .medBeregnetPrÅr(BigDecimal.valueOf(BRUTTO_PR_ÅR))
            .medAktivitetStatus(AktivitetStatus.BRUKERS_ANDEL)
            .build(periode1);

        var builder = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
                .medBeregningsgrunnlag(bg)
                .medRegisterAktiviteter(beregningAktivitetAggregat);

        return repositoryProvider.getBeregningsgrunnlagRepository().lagre(behandlingReferanse.getId(), builder, BeregningsgrunnlagTilstand.KOFAKBER_UT);
    }

    private BeregningsgrunnlagGrunnlagEntitet lagBeregningsgrunnlag(TestScenarioBuilder scenario, BeregningAktivitetAggregatEntitet beregningAktivitetAggregat) {
        BeregningsgrunnlagEntitet bg = scenario.medBeregningsgrunnlag()
            .medSkjæringstidspunkt(LocalDate.now().minusDays(5))
            .medGrunnbeløp(BigDecimal.valueOf(90000))
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.KUN_YTELSE))
            .build();
        BeregningsgrunnlagPeriode periode1 = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT_OPPTJENING, null)
            .build(bg);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medLagtTilAvSaksbehandler(false)
            .medAktivitetStatus(AktivitetStatus.BRUKERS_ANDEL)
            .build(periode1);

        var builder = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
                .medBeregningsgrunnlag(bg)
                .medRegisterAktiviteter(beregningAktivitetAggregat);

        return repositoryProvider.getBeregningsgrunnlagRepository().lagre(behandlingReferanse.getId(), builder, BeregningsgrunnlagTilstand.OPPRETTET);
    }

    private BeregningsgrunnlagGrunnlagEntitet lagForrigeBeregningsgrunnlagMedLagtTilAndel(TestScenarioBuilder scenario, BeregningAktivitetAggregatEntitet beregningAktivitetAggregat) {
        BeregningsgrunnlagEntitet bg = scenario.medBeregningsgrunnlag()
            .medSkjæringstidspunkt(LocalDate.now().minusDays(5))
            .medGrunnbeløp(BigDecimal.valueOf(90000))
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.KUN_YTELSE))
            .build();
        BeregningsgrunnlagPeriode periode1 = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT_OPPTJENING, null)
            .build(bg);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medLagtTilAvSaksbehandler(false)
            .medAktivitetStatus(AktivitetStatus.BRUKERS_ANDEL)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .build(periode1);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medLagtTilAvSaksbehandler(true)
            .medAktivitetStatus(AktivitetStatus.BRUKERS_ANDEL)
            .medInntektskategori(Inntektskategori.FRILANSER)
            .build(periode1);

        return BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
                .medBeregningsgrunnlag(bg)
                .medRegisterAktiviteter(beregningAktivitetAggregat)
                .build(1L, BeregningsgrunnlagTilstand.OPPRETTET);
    }

    private BeregningsgrunnlagEntitet lagForrigeBeregningsgrunnlagMUtenKunYtelse(TestScenarioBuilder scenario) {
        BeregningsgrunnlagEntitet bg = scenario.medBeregningsgrunnlag()
            .medSkjæringstidspunkt(LocalDate.now().minusDays(5))
            .medGrunnbeløp(BigDecimal.valueOf(90000))
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .build();
        BeregningsgrunnlagPeriode periode1 = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT_OPPTJENING, null)
            .build(bg);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medLagtTilAvSaksbehandler(false)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(Arbeidsgiver.fra(AktørId.dummy())))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .build(periode1);
        return bg;
    }
}
