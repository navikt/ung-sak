package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.ArbeidstakerUtenInntektsmeldingAndelDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.FaktaOmBeregningDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.VurderMottarYtelseDto;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningArbeidsgiverTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningIAYTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.AbstractTestScenario;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjenesteImpl;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;

public class VurderMottarYtelseDtoTjenesteTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, 9, 30);
    private static final String ORGNR = "973093681";
    private static final EksternArbeidsforholdRef EKSTERN_ARB_ID = EksternArbeidsforholdRef.ref("TEST_REF1");
    private static final BigDecimal INNTEKT1 = BigDecimal.valueOf(10000);
    private static final BigDecimal INNTEKT2 = BigDecimal.valueOf(20000);
    private static final BigDecimal INNTEKT3 = BigDecimal.valueOf(30000);
    private static final List<BigDecimal> INNTEKT_PR_MND = List.of(INNTEKT1, INNTEKT2, INNTEKT3);
    private static final BigDecimal INNTEKT_SNITT = INNTEKT1.add(INNTEKT2.add(INNTEKT3)).divide(BigDecimal.valueOf(3), 10, RoundingMode.HALF_EVEN);
    private static final String FRILANS_ORGNR = "853498598934";

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private RepositoryProvider repositoryProvider = new RepositoryProvider(repoRule.getEntityManager());
    private final InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    private BehandlingReferanse behandlingReferanse;
    private BeregningsgrunnlagEntitet beregningsgrunnlag;
    private BeregningsgrunnlagPeriode periode;
    private BeregningIAYTestUtil iayTestUtil;
    private BeregningArbeidsgiverTestUtil arbeidsgiverTestUtil;
    private VurderMottarYtelseDtoTjeneste dtoTjeneste;
    private String referanseEtterLagring;
    private BeregningsgrunnlagGrunnlagEntitet grunnlag;

    @Before
    public void setUp() {
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = lagre(scenario);
        beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING)
            .medGrunnbeløp(BigDecimal.valueOf(91425L))
            .leggTilFaktaOmBeregningTilfeller(Collections.singletonList(FaktaOmBeregningTilfelle.VURDER_MOTTAR_YTELSE))
            .build();
        periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT_OPPTJENING, null)
            .build(beregningsgrunnlag);
        grunnlag = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .build(behandlingReferanse.getId(), BeregningsgrunnlagTilstand.OPPRETTET);
        iayTestUtil = new BeregningIAYTestUtil(iayTjeneste);
        arbeidsgiverTestUtil = new BeregningArbeidsgiverTestUtil(repositoryProvider.getVirksomhetRepository());
        var virksomhetTjeneste = new VirksomhetTjeneste(null, repositoryProvider.getVirksomhetRepository());
        BeregningsgrunnlagDtoUtil dtoUtil = new BeregningsgrunnlagDtoUtil(repositoryProvider.getBeregningsgrunnlagRepository(), new ArbeidsgiverTjenesteImpl(null, virksomhetTjeneste));
        FaktaOmBeregningAndelDtoTjeneste faktaOmBeregningAndelDtoTjeneste = new FaktaOmBeregningAndelDtoTjeneste(dtoUtil);
        dtoTjeneste = new VurderMottarYtelseDtoTjeneste(dtoUtil, faktaOmBeregningAndelDtoTjeneste);
    }

    private BehandlingReferanse lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider, iayTjeneste::lagreIayAggregat, iayTjeneste::lagreOppgittOpptjening);
    }

    @Test
    public void skal_lage_dto_for_mottar_ytelse_uten_mottar_ytelse_satt() {
        // Arrange
        FaktaOmBeregningDto dto = new FaktaOmBeregningDto();
        byggFrilansAndel(null);
        BeregningsgrunnlagPrStatusOgAndel arbeidsforholdAndel = byggArbeidsforholdMedBgAndel(null);

        // Act
        var iayGrunnlag = iayTjeneste.finnGrunnlag(behandlingReferanse.getId()).orElseThrow();
        var input = new BeregningsgrunnlagInput(behandlingReferanse, iayGrunnlag, null, AktivitetGradering.INGEN_GRADERING, null)
            .medBeregningsgrunnlagGrunnlag(grunnlag);
        dtoTjeneste.lagDto(input, Optional.empty(), dto);

        // Assert
        VurderMottarYtelseDto mottarYtelseDto = dto.getVurderMottarYtelse();
        assertThat(mottarYtelseDto.getErFrilans()).isTrue();
        assertThat(mottarYtelseDto.getFrilansMottarYtelse()).isNull();
        assertThat(mottarYtelseDto.getFrilansInntektPrMnd()).isEqualByComparingTo(INNTEKT_SNITT);
        assertThat(mottarYtelseDto.getArbeidstakerAndelerUtenIM()).hasSize(1);
        ArbeidstakerUtenInntektsmeldingAndelDto andelUtenIM = mottarYtelseDto.getArbeidstakerAndelerUtenIM().get(0);
        assertThat(andelUtenIM.getMottarYtelse()).isNull();
        assertThat(andelUtenIM.getArbeidsforhold().getArbeidsgiverId()).isEqualTo(ORGNR);
        assertThat(andelUtenIM.getArbeidsforhold().getArbeidsforholdId()).isEqualTo(referanseEtterLagring);
        assertThat(andelUtenIM.getAndelsnr()).isEqualTo(arbeidsforholdAndel.getAndelsnr());
        assertThat(andelUtenIM.getInntektPrMnd()).isEqualByComparingTo(INNTEKT_SNITT);
    }

    @Test
    public void skal_lage_dto_for_mottar_ytelse_med_mottar_ytelse_satt() {
        // Arrange
        FaktaOmBeregningDto dto = new FaktaOmBeregningDto();
        byggFrilansAndel(false);
        BeregningsgrunnlagPrStatusOgAndel arbeidsforholdAndel = byggArbeidsforholdMedBgAndel(true);

        // Act
        var iayGrunnlag = iayTjeneste.finnGrunnlag(behandlingReferanse.getId()).orElseThrow();
        var input = new BeregningsgrunnlagInput(behandlingReferanse, iayGrunnlag, null, AktivitetGradering.INGEN_GRADERING, null)
            .medBeregningsgrunnlagGrunnlag(grunnlag);
        dtoTjeneste.lagDto(input, Optional.empty(), dto);

        // Assert
        VurderMottarYtelseDto mottarYtelseDto = dto.getVurderMottarYtelse();
        assertThat(mottarYtelseDto.getErFrilans()).isTrue();
        assertThat(mottarYtelseDto.getFrilansMottarYtelse()).isFalse();
        assertThat(mottarYtelseDto.getFrilansInntektPrMnd()).isEqualByComparingTo(INNTEKT_SNITT);
        assertThat(mottarYtelseDto.getArbeidstakerAndelerUtenIM()).hasSize(1);
        ArbeidstakerUtenInntektsmeldingAndelDto andelUtenIM = mottarYtelseDto.getArbeidstakerAndelerUtenIM().get(0);
        assertThat(andelUtenIM.getMottarYtelse()).isTrue();
        assertThat(andelUtenIM.getArbeidsforhold().getArbeidsgiverId()).isEqualTo(ORGNR);
        assertThat(andelUtenIM.getArbeidsforhold().getArbeidsforholdId()).isEqualTo(referanseEtterLagring);
        assertThat(andelUtenIM.getAndelsnr()).isEqualTo(arbeidsforholdAndel.getAndelsnr());
        assertThat(andelUtenIM.getInntektPrMnd()).isEqualByComparingTo(INNTEKT_SNITT);
    }

    private void byggFrilansAndel(Boolean mottarYtelse) {
        Arbeidsgiver arbeidsgiver = arbeidsgiverTestUtil.forArbeidsgiverVirksomhet(FRILANS_ORGNR);
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(10), EKSTERN_ARB_ID, arbeidsgiver, ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER, INNTEKT_PR_MND, true,
            Optional.of(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2L)));
        iayTestUtil.lagAnnenAktivitetOppgittOpptjening(behandlingReferanse, ArbeidType.FRILANSER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(2),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.plusWeeks(2));
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.FRILANSER)
            .medInntektskategori(Inntektskategori.FRILANSER)
            .medBeregningsperiode(SKJÆRINGSTIDSPUNKT_OPPTJENING.withDayOfMonth(1).minusMonths(3), SKJÆRINGSTIDSPUNKT_OPPTJENING.withDayOfMonth(1).minusDays(1))
            .medMottarYtelse(mottarYtelse, AktivitetStatus.FRILANSER)
            .build(periode);
    }

    private BeregningsgrunnlagPrStatusOgAndel byggArbeidsforholdMedBgAndel(Boolean mottarYtelse) {
        Arbeidsgiver arbeidsgiver = arbeidsgiverTestUtil.forArbeidsgiverVirksomhet(ORGNR);
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(10), EKSTERN_ARB_ID, arbeidsgiver, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, INNTEKT_PR_MND, true,
            Optional.of(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2L)));
        String referanse = finnReferanseForArbeidsgiver(arbeidsgiver);
        return BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medMottarYtelse(mottarYtelse, AktivitetStatus.ARBEIDSTAKER)
            .medBeregningsperiode(SKJÆRINGSTIDSPUNKT_OPPTJENING.withDayOfMonth(1).minusMonths(3), SKJÆRINGSTIDSPUNKT_OPPTJENING.withDayOfMonth(1).minusDays(1))
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder()
                .medArbeidsforholdRef(referanse).medArbeidsgiver(arbeidsgiver))
            .build(periode);
    }

    private String finnReferanseForArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        InntektArbeidYtelseGrunnlag grunnlag = iayTjeneste.hentGrunnlag(behandlingReferanse.getId());
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(behandlingReferanse.getAktørId()))
            .før(SKJÆRINGSTIDSPUNKT_OPPTJENING);
        referanseEtterLagring = filter.getYrkesaktiviteter().stream()
            .filter(ya -> ya.getArbeidsgiver().equals(arbeidsgiver))
            .filter(ya -> ya.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold())
            .map(ya -> ya.getArbeidsforholdRef().getReferanse())
            .findFirst().orElseThrow();
        return referanseEtterLagring;
    }
}
