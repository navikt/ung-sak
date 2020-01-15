package no.nav.folketrygdloven.beregningsgrunnlag;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.folketrygdloven.beregningsgrunnlag.adapter.util.BeregningsgrunnlagTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningArbeidsgiverTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningIAYTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningInntektsmeldingTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.AbstractTestScenario;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class BeregningsperiodeTjenesteTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.of(2019, Month.JANUARY, 1);

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private RepositoryProvider repositoryProvider = new RepositoryProvider(repoRule.getEntityManager());
    @Inject
    private BeregningsperiodeTjeneste beregningsperiodeTjeneste;
    @Inject
    private BeregningIAYTestUtil iayTestUtil;
    @Inject
    private BeregningInntektsmeldingTestUtil inntektsmeldingTestUtil;
    @Inject
    private BeregningsgrunnlagTestUtil beregningTestUtil;
    @Inject
    private BeregningArbeidsgiverTestUtil arbeidsgiverTestUtil;

    private final InternArbeidsforholdRef arbRef1 = InternArbeidsforholdRef.nyRef();
    private BehandlingReferanse behandlingReferanse;
    private Arbeidsgiver arbeidsgiverA;
    private Arbeidsgiver arbeidsgiverB;

    @Before
    public void setup() {
        opprettArbeidsforhold();
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        scenario.medDefaultInntektArbeidYtelse();
        behandlingReferanse = lagre(scenario);
    }

    private BehandlingReferanse lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider, iayTestUtil.getIayTjeneste()::lagreIayAggregat, iayTestUtil.getIayTjeneste()::lagreOppgittOpptjening);
    }

    private void opprettArbeidsforhold() {
        arbeidsgiverA = Arbeidsgiver.virksomhet("123456789");
        arbeidsgiverB = Arbeidsgiver.virksomhet("987654321");
    }

    @Test
    public void skalTesteAtBeregningsperiodeBlirSattRiktig() {
        // Arrange
        LocalDate skjæringstidspunkt = LocalDate.of(2019, 5, 15);

        // Act
        DatoIntervallEntitet periode = BeregningsperiodeTjeneste.fastsettBeregningsperiodeForATFLAndeler(skjæringstidspunkt);

        // Assert
        assertThat(periode.getFomDato()).isEqualTo(LocalDate.of(2019, 2, 1));
        assertThat(periode.getTomDato()).isEqualTo(LocalDate.of(2019, 4, 30));
    }

    @Test
    public void skalIkkeSettesPåVentNårIkkeErATFL() {
        // Arrange
        LocalDate dagensdato = SKJÆRINGSTIDSPUNKT;
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingReferanse, SKJÆRINGSTIDSPUNKT,
            AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, AktivitetStatus.DAGPENGER);

        // Act
        boolean resultat = beregningsperiodeTjeneste.skalVentePåInnrapporteringAvInntekt(beregningsgrunnlag, List.of(), dagensdato);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skalIkkeSettesPåVentNårNåtidErEtterFrist() {
        // Arrange
        LocalDate dagensdato = SKJÆRINGSTIDSPUNKT.plusDays(7); // 8. januar
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT, LocalDate.of(2017, 1, 1), LocalDate.of(2030, 1, 1), arbRef1, arbeidsgiverA);
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingReferanse, SKJÆRINGSTIDSPUNKT);

        // Act
        boolean resultat = beregningsperiodeTjeneste.skalVentePåInnrapporteringAvInntekt(beregningsgrunnlag, List.of(), dagensdato);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skalIkkeSettesPåVentNårNåtidErLengeEtterFrist() {
        // Arrange
        LocalDate dagensdato = SKJÆRINGSTIDSPUNKT.plusDays(45);
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT, LocalDate.of(2017, 1, 1), LocalDate.of(2030, 1, 1), arbRef1, arbeidsgiverA);
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingReferanse, SKJÆRINGSTIDSPUNKT);

        // Act
        boolean resultat = beregningsperiodeTjeneste.skalVentePåInnrapporteringAvInntekt(beregningsgrunnlag, List.of(), dagensdato);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skalAlltidSettesPåVentNårBrukerErFrilanserFørFrist() {
        // Arrange
        LocalDate dagensdato = SKJÆRINGSTIDSPUNKT.plusDays(4);
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingReferanse, SKJÆRINGSTIDSPUNKT,
            AktivitetStatus.FRILANSER);

        // Act
        boolean resultat = beregningsperiodeTjeneste.skalVentePåInnrapporteringAvInntekt(beregningsgrunnlag, List.of(), dagensdato);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void skalIkkeSettesPåVentNårHarInntektsmeldingFørFrist() {
        // Arrange
        LocalDate dagensdato = SKJÆRINGSTIDSPUNKT.plusDays(3);
        InternArbeidsforholdRef arbId = arbRef1;
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT, LocalDate.of(2017, 1, 1), LocalDate.of(2030, 1, 1), arbId, arbeidsgiverA);
        inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, arbeidsgiverA.getIdentifikator(), arbId, SKJÆRINGSTIDSPUNKT, LocalDateTime.now());
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingReferanse, SKJÆRINGSTIDSPUNKT);
        // Act
        boolean resultat = beregningsperiodeTjeneste.skalVentePåInnrapporteringAvInntekt(beregningsgrunnlag, List.of(arbeidsgiverA), dagensdato);
        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skalSettesPåVentNårFørFristUtenInntektsmelding() {
        // Arrange
        LocalDate dagensdato = SKJÆRINGSTIDSPUNKT.plusDays(4);
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT, LocalDate.of(2017, 1, 1), LocalDate.of(2030, 1, 1), arbRef1, arbeidsgiverA);
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingReferanse, SKJÆRINGSTIDSPUNKT);
        // Act
        boolean resultat = beregningsperiodeTjeneste.skalVentePåInnrapporteringAvInntekt(beregningsgrunnlag, List.of(), dagensdato);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void skalSettesPåVentNårUtenInntektsmeldingFørFristFlereArbeidsforhold() {
        // Arrange
        LocalDate dagensdato = SKJÆRINGSTIDSPUNKT.plusDays(4);
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT, LocalDate.of(2017, 1, 1), LocalDate.of(2030, 1, 1), arbRef1, arbeidsgiverA);
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT, LocalDate.of(2017, 1, 1), LocalDate.of(2030, 1, 1), arbRef1, arbeidsgiverB);

        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingReferanse, SKJÆRINGSTIDSPUNKT);

        // Act
        boolean resultat = beregningsperiodeTjeneste.skalVentePåInnrapporteringAvInntekt(beregningsgrunnlag, List.of(), dagensdato);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void skalSettesPåVentNårHarInntektsmeldingFørFristForBareEttAvFlereArbeidsforhold() {
        // Arrange
        LocalDate dagensdato = SKJÆRINGSTIDSPUNKT.plusDays(2);
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT, LocalDate.of(2017, 1, 1), LocalDate.of(2030, 1, 1), arbRef1, arbeidsgiverA);
        InternArbeidsforholdRef arbIdB = InternArbeidsforholdRef.nyRef();
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT, LocalDate.of(2017, 1, 1), LocalDate.of(2030, 1, 1), arbIdB, arbeidsgiverB);
        inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, arbeidsgiverB.getIdentifikator(), arbIdB, SKJÆRINGSTIDSPUNKT, LocalDateTime.now());

        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingReferanse, SKJÆRINGSTIDSPUNKT);
        // Act
        boolean resultat = beregningsperiodeTjeneste.skalVentePåInnrapporteringAvInntekt(beregningsgrunnlag, List.of(), dagensdato);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void skalIkkeSettesPåVentNårAlleHarInntektsmeldingFørFristFlereArbeidsforhold() {
        // Arrange
        LocalDate dagensdato = SKJÆRINGSTIDSPUNKT.plusDays(2);
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT, LocalDate.of(2017, 1, 1), LocalDate.of(2030, 1, 1), arbRef1, arbeidsgiverA);
        InternArbeidsforholdRef arbIdB = InternArbeidsforholdRef.nyRef();
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT, LocalDate.of(2017, 1, 1), LocalDate.of(2030, 1, 1), arbIdB, arbeidsgiverB);
        inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, arbeidsgiverA.getIdentifikator(), null, SKJÆRINGSTIDSPUNKT, LocalDateTime.now());
        inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, arbeidsgiverB.getIdentifikator(), arbIdB, SKJÆRINGSTIDSPUNKT,
            LocalDateTime.now().plusSeconds(1));

        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingReferanse, SKJÆRINGSTIDSPUNKT);

        // Act
        boolean resultat = beregningsperiodeTjeneste.skalVentePåInnrapporteringAvInntekt(beregningsgrunnlag, List.of(arbeidsgiverA, arbeidsgiverB), dagensdato);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skalIkkeSettesPåVentNårArbeidsforholdUtenInntektsmeldingErLagtTilAvSaksbehandler() {
        // Arrange
        LocalDate dagensdato = SKJÆRINGSTIDSPUNKT.plusDays(2);
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT, LocalDate.of(2017, 1, 1), LocalDate.of(2030, 1, 1), arbRef1, arbeidsgiverA);
        InternArbeidsforholdRef arbIdB = InternArbeidsforholdRef.nyRef();
        Arbeidsgiver lagtTilArbeidsforhold = arbeidsgiverTestUtil.forKunstigArbeidsforhold();
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT, LocalDate.of(2017, 1, 1), LocalDate.of(2030, 1, 1), arbIdB, lagtTilArbeidsforhold);
        inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, arbeidsgiverA.getIdentifikator(), null, SKJÆRINGSTIDSPUNKT, LocalDateTime.now());

        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingReferanse, SKJÆRINGSTIDSPUNKT);
        // Act
        boolean resultat = beregningsperiodeTjeneste.skalVentePåInnrapporteringAvInntekt(beregningsgrunnlag, List.of(arbeidsgiverA), dagensdato);
        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skalUtledeRiktigFrist() {
        // Arrange
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT, LocalDate.of(2017, 1, 1), LocalDate.of(2030, 1, 1), arbRef1, arbeidsgiverA);
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingReferanse, SKJÆRINGSTIDSPUNKT);

        // Act
        LocalDate frist = beregningsperiodeTjeneste.utledBehandlingPåVentFrist(beregningsgrunnlag);

        // Assert
        assertThat(frist).isEqualTo(SKJÆRINGSTIDSPUNKT.plusDays(7));
    }

}
