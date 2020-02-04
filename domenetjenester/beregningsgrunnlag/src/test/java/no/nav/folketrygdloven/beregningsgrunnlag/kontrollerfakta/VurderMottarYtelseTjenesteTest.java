package no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningIAYTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.k9.kodeverk.iay.AktivitetStatus;
import no.nav.k9.kodeverk.iay.Inntektskategori;

public class VurderMottarYtelseTjenesteTest {
    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, 9, 30);
    private static final String ORGNR = "3482934982384";
    private static final InternArbeidsforholdRef ARB_ID = InternArbeidsforholdRef.namedRef("TEST-REF");

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private RepositoryProvider repositoryProvider = new RepositoryProvider(repoRule.getEntityManager());

    private BehandlingReferanse behandlingReferanse;
    private BeregningsgrunnlagEntitet beregningsgrunnlag;
    private BeregningsgrunnlagPeriode periode;
    private BeregningIAYTestUtil iayTestUtil;

    @Before
    public void setUp() {
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = scenario.lagre(repositoryProvider);
        beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING)
            .medGrunnbeløp(BigDecimal.valueOf(91425L))
            .build();
        periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT_OPPTJENING, null)
            .build(beregningsgrunnlag);
        var iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        iayTestUtil = new BeregningIAYTestUtil(iayTjeneste);
    }

    @Test
    public void skal_gi_frilanser() {
        // Arrange
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.FRILANSER)
            .medInntektskategori(Inntektskategori.FRILANSER)
            .build(periode);

        // Act
        boolean erFrilanser = VurderMottarYtelseTjeneste.erFrilanser(beregningsgrunnlag);

        // Assert
        assertThat(erFrilanser).isTrue();
    }

    @Test
    public void skal_ikkje_gi_frilanser() {
        // Arrange
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medInntektskategori(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE)
            .build(periode);

        // Act
        boolean erFrilanser = VurderMottarYtelseTjeneste.erFrilanser(beregningsgrunnlag);

        // Assert
        assertThat(erFrilanser).isFalse();
    }

    @Test
    public void skal_vurdere_mottar_ytelse_for_frilans() {
        // Arrange
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.FRILANSER)
            .medInntektskategori(Inntektskategori.FRILANSER)
            .build(periode);

        // Act
        boolean skalVurdereMottarYtelse = VurderMottarYtelseTjeneste.skalVurdereMottattYtelse(beregningsgrunnlag,
            iayTestUtil.getIayTjeneste().finnGrunnlag(behandlingReferanse.getBehandlingId()).orElse(InntektArbeidYtelseGrunnlagBuilder.nytt().build()));

        // Assert
        assertThat(skalVurdereMottarYtelse).isTrue();
    }

    @Test
    public void skal_vurdere_mottary_ytelse_for_frilans_og_arbeidstaker_uten_inntektsmelding() {
        // Arrange
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(10), ARB_ID, arbeidsgiver, Optional.of(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2L)));
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsforholdRef(ARB_ID).medArbeidsgiver(arbeidsgiver))
            .build(periode);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.FRILANSER)
            .medInntektskategori(Inntektskategori.FRILANSER)
            .build(periode);

        // Act
        boolean skalVurdereMottarYtelse = VurderMottarYtelseTjeneste.skalVurdereMottattYtelse(beregningsgrunnlag, iayTestUtil.getIayTjeneste().hentGrunnlag(behandlingReferanse.getBehandlingId()));

        // Assert
        assertThat(skalVurdereMottarYtelse).isTrue();
    }
}
