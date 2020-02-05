package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.ArbeidstakerandelUtenIMMottarYtelseDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FaktaBeregningLagreDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.MottarYtelseDto;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningArbeidsgiverTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;

public class MottarYtelseOppdatererTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, 9, 30);
    private static final String ORGNR = "3482934982384";
    private static final InternArbeidsforholdRef ARB_ID = InternArbeidsforholdRef.namedRef("TEST-REF");

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private RepositoryProvider repositoryProvider = new RepositoryProvider(repoRule.getEntityManager());

    private BehandlingReferanse behandlingReferanse;
    private BeregningsgrunnlagEntitet beregningsgrunnlag;
    private BeregningsgrunnlagPeriode periode;
    private BeregningArbeidsgiverTestUtil arbeidsgiverTestUtil;
    private MottarYtelseOppdaterer oppdaterer;

    @Before
    public void setUp() {
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = scenario.lagre(repositoryProvider);
        beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING)
            .medGrunnbeløp(BigDecimal.valueOf(91425L))
            .leggTilFaktaOmBeregningTilfeller(singletonList(FaktaOmBeregningTilfelle.VURDER_MOTTAR_YTELSE))
            .build();
        periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT_OPPTJENING, null)
            .build(beregningsgrunnlag);
        arbeidsgiverTestUtil = new BeregningArbeidsgiverTestUtil(repositoryProvider.getVirksomhetRepository());
        this.oppdaterer = new MottarYtelseOppdaterer();
    }

    @Test
    public void skal_sette_mottar_ytelse_kun_for_frilans() {
        // Arrange
        FaktaBeregningLagreDto dto = new FaktaBeregningLagreDto(singletonList(FaktaOmBeregningTilfelle.VURDER_MOTTAR_YTELSE));
        dto.setMottarYtelse(new MottarYtelseDto(true, emptyList()));
        BeregningsgrunnlagPrStatusOgAndel frilansAndel = byggFrilansAndel(null);
        BeregningsgrunnlagPrStatusOgAndel arbeidsforholdAndel = byggArbeidsforholdMedBgAndel(null);

        // Act
        oppdaterer.oppdater(dto, behandlingReferanse, beregningsgrunnlag, Optional.empty());

        // Assert
        assertThat(frilansAndel.mottarYtelse()).isPresent();
        assertThat(frilansAndel.mottarYtelse().get()).isTrue();
        assertThat(arbeidsforholdAndel.mottarYtelse()).isNotPresent();
    }

    @Test
    public void skal_sette_mottar_ytelse_kun_for_frilans_og_arbeidstakerandel() {
        // Arrange
        FaktaBeregningLagreDto dto = new FaktaBeregningLagreDto(singletonList(FaktaOmBeregningTilfelle.VURDER_MOTTAR_YTELSE));
        BeregningsgrunnlagPrStatusOgAndel frilansAndel = byggFrilansAndel(null);
        BeregningsgrunnlagPrStatusOgAndel arbeidsforholdAndel = byggArbeidsforholdMedBgAndel(null);
        dto.setMottarYtelse(new MottarYtelseDto(false,
            singletonList(new ArbeidstakerandelUtenIMMottarYtelseDto(arbeidsforholdAndel.getAndelsnr(), true))));

        // Act
        oppdaterer.oppdater(dto, behandlingReferanse, beregningsgrunnlag, Optional.empty());

        // Assert
        assertThat(frilansAndel.mottarYtelse()).isPresent();
        assertThat(frilansAndel.mottarYtelse().get()).isFalse();
        assertThat(arbeidsforholdAndel.mottarYtelse()).isPresent();
        assertThat(arbeidsforholdAndel.mottarYtelse().get()).isTrue();
    }

    private BeregningsgrunnlagPrStatusOgAndel byggFrilansAndel(Boolean mottarYtelse) {
        return BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.FRILANSER)
            .medInntektskategori(Inntektskategori.FRILANSER)
            .medMottarYtelse(mottarYtelse, AktivitetStatus.FRILANSER)
            .build(periode);
    }

    private BeregningsgrunnlagPrStatusOgAndel byggArbeidsforholdMedBgAndel(Boolean mottarYtelse) {
        Arbeidsgiver arbeidsgiver = arbeidsgiverTestUtil.forArbeidsgiverVirksomhet(ORGNR);
        return BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medMottarYtelse(mottarYtelse, AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsforholdRef(ARB_ID).medArbeidsgiver(arbeidsgiver))
            .build(periode);
    }


}
