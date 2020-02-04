package no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingSomIkkeKommer;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.k9.kodeverk.iay.AktivitetStatus;
import no.nav.k9.kodeverk.iay.Inntektskategori;

public class ArbeidstakerUtenInntektsmeldingTjenesteTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, 9, 30);
    private static final String ORGNR = "3482934982384";
    private static final InternArbeidsforholdRef ARB_ID = InternArbeidsforholdRef.namedRef("TEST-REF");
    private static final EksternArbeidsforholdRef ARB_ID_EKSTERN = EksternArbeidsforholdRef.ref("A");
    private static final AktørId AKTØR_ID_ARBEIDSGIVER = AktørId.dummy();

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private RepositoryProvider repositoryProvider = new RepositoryProvider(repoRule.getEntityManager());
    private BeregningsgrunnlagEntitet beregningsgrunnlag;
    private BeregningsgrunnlagPeriode periode;

    @Before
    public void setUp() {
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        @SuppressWarnings("unused")
        BehandlingReferanse behandlingReferanse = scenario.lagre(repositoryProvider);
        beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING)
            .medGrunnbeløp(BigDecimal.valueOf(91425L))
            .build();
        periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT_OPPTJENING, null)
            .build(beregningsgrunnlag);
    }

    @Test
    public void skal_returnere_andeler_uten_inntektsmelding() {
        // Arrange
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);
        InntektsmeldingSomIkkeKommer imSomIkkeKommer = new InntektsmeldingSomIkkeKommer(arbeidsgiver, ARB_ID, ARB_ID_EKSTERN);
        InntektArbeidYtelseGrunnlag iayGrunnlagMock = mock(InntektArbeidYtelseGrunnlag.class);
        when(iayGrunnlagMock.getInntektsmeldingerSomIkkeKommer()).thenReturn(Collections.singletonList(imSomIkkeKommer));
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsforholdRef(ARB_ID).medArbeidsgiver(arbeidsgiver))
            .build(periode);

        // Act
        Collection<BeregningsgrunnlagPrStatusOgAndel> andelerUtenInntektsmelding = ArbeidstakerUtenInntektsmeldingTjeneste
            .finnArbeidstakerAndelerUtenInntektsmelding(beregningsgrunnlag, iayGrunnlagMock);

        // Assert
        assertThat(andelerUtenInntektsmelding).hasSize(1);
    }

    @Test
    public void skal_returnere_andeler_uten_inntektsmelding_privatperson_som_arbeidsgiver() {
        // Arrange
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.person(AKTØR_ID_ARBEIDSGIVER);
        InntektsmeldingSomIkkeKommer imSomIkkeKommer = new InntektsmeldingSomIkkeKommer(arbeidsgiver, ARB_ID, ARB_ID_EKSTERN);
        InntektArbeidYtelseGrunnlag iayGrunnlagMock = mock(InntektArbeidYtelseGrunnlag.class);
        when(iayGrunnlagMock.getInntektsmeldingerSomIkkeKommer()).thenReturn(Collections.singletonList(imSomIkkeKommer));
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsforholdRef(ARB_ID).medArbeidsgiver(arbeidsgiver))
            .build(periode);

        // Act
        Collection<BeregningsgrunnlagPrStatusOgAndel> andelerUtenInntektsmelding = ArbeidstakerUtenInntektsmeldingTjeneste
            .finnArbeidstakerAndelerUtenInntektsmelding(beregningsgrunnlag, iayGrunnlagMock);

        // Assert
        assertThat(andelerUtenInntektsmelding).hasSize(1);
    }


    @Test
    public void skal_tom_liste_med_andeler_om_ingen_arbeidstakere_uten_inntektsmelding() {
        // Arrange
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);
        InntektArbeidYtelseGrunnlag iayGrunnlagMock = mock(InntektArbeidYtelseGrunnlag.class);
        when(iayGrunnlagMock.getInntektsmeldingerSomIkkeKommer()).thenReturn(Collections.emptyList());
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsforholdRef(ARB_ID).medArbeidsgiver(arbeidsgiver))
            .build(periode);

        // Act
        Collection<BeregningsgrunnlagPrStatusOgAndel> andelerUtenInntektsmelding = ArbeidstakerUtenInntektsmeldingTjeneste
            .finnArbeidstakerAndelerUtenInntektsmelding(beregningsgrunnlag, iayGrunnlagMock);

        // Assert
        assertThat(andelerUtenInntektsmelding).isEmpty();
    }

    @Test
    public void skal_returnere_tom_liste_med_andeler_arbeidstaker_uten_inntektsmelding_status_DP_på_skjæringstidspunktet() {
        // Arrange
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);
        InntektsmeldingSomIkkeKommer imSomIkkeKommer = new InntektsmeldingSomIkkeKommer(arbeidsgiver, ARB_ID, ARB_ID_EKSTERN);
        InntektArbeidYtelseGrunnlag iayGrunnlagMock = mock(InntektArbeidYtelseGrunnlag.class);
        when(iayGrunnlagMock.getInntektsmeldingerSomIkkeKommer()).thenReturn(Collections.singletonList(imSomIkkeKommer));
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.DAGPENGER)
            .medInntektskategori(Inntektskategori.DAGPENGER)
            .build(periode);

        // Act
        Collection<BeregningsgrunnlagPrStatusOgAndel> andelerUtenInntektsmelding = ArbeidstakerUtenInntektsmeldingTjeneste
            .finnArbeidstakerAndelerUtenInntektsmelding(beregningsgrunnlag, iayGrunnlagMock);

        // Assert
        assertThat(andelerUtenInntektsmelding).isEmpty();
    }
}
