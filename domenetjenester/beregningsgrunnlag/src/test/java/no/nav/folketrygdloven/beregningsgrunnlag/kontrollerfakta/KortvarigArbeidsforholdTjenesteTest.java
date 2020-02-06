package no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.util.BeregningsgrunnlagTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningIAYTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class KortvarigArbeidsforholdTjenesteTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, 9, 30);
    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private RepositoryProvider repositoryProvider = new RepositoryProvider(repoRule.getEntityManager());

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository = repositoryProvider.getBeregningsgrunnlagRepository();
    @Inject
    private BeregningIAYTestUtil iayTestUtil;
    @Inject
    private BeregningsgrunnlagTestUtil beregningTestUtil;

    private BehandlingReferanse behandlingReferanse;

    private InntektArbeidYtelseTjeneste iayTjeneste;

    @Before
    public void setup() {
        iayTjeneste = iayTestUtil.getIayTjeneste();
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = scenario.lagre(repositoryProvider);
    }

    @Test
    public void skalIkkjeGiKortvarigForArbeidsforholdPå6Mnd() {
        // Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        String orgnr = "123456780";
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, LocalDate.of(2018, 8, 5),
            LocalDate.of(2019, 2, 4), arbId, Arbeidsgiver.virksomhet(orgnr));
        beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING);

        // Act
        Map<BeregningsgrunnlagPrStatusOgAndel, Yrkesaktivitet> kortvarige = KortvarigArbeidsforholdTjeneste.hentAndelerForKortvarigeArbeidsforhold(
            behandlingReferanse.getAktørId(), beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId()),
            iayTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId())
        );

        // Assert
        assertThat(kortvarige).isEmpty();
    }

    @Test
    public void skalIkkjeGiKortvarigForArbeidsforholdPå6MndIMånederMedUlikDato1() {
        // Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        String orgnr = "123456780";
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, LocalDate.of(2018, 8, 29),
            LocalDate.of(2019, 2, 28), arbId, Arbeidsgiver.virksomhet(orgnr));
        beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING);

        // Act
        Map<BeregningsgrunnlagPrStatusOgAndel, Yrkesaktivitet> kortvarige = KortvarigArbeidsforholdTjeneste.hentAndelerForKortvarigeArbeidsforhold(
            behandlingReferanse.getAktørId(),
            beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId()),
            iayTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId())
        );

        // Assert
        assertThat(kortvarige).isEmpty();
    }

    @Test
    public void skalIkkjeGiKortvarigForArbeidsforholdPå6MndIMånederMedUlikDato2() {
        // Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        String orgnr = "123456780";
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, LocalDate.of(2018, 8, 31),
            LocalDate.of(2019, 2, 28), arbId, Arbeidsgiver.virksomhet(orgnr));
        beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING);

        // Act
        Map<BeregningsgrunnlagPrStatusOgAndel, Yrkesaktivitet> kortvarige = KortvarigArbeidsforholdTjeneste.hentAndelerForKortvarigeArbeidsforhold(
            behandlingReferanse.getAktørId(),
            beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId()),
            iayTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId())
        );

        // Assert
        assertThat(kortvarige).isEmpty();
    }

    @Test
    public void skalIkkjeGiKortvarigForArbeidsforholdPå6MndIMånederMedUlikDato3() {
        // Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        String orgnr = "123456780";
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, LocalDate.of(2018, 9, 1),
            LocalDate.of(2019, 2, 28), arbId, Arbeidsgiver.virksomhet(orgnr));
        beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING);

        // Act
        Map<BeregningsgrunnlagPrStatusOgAndel, Yrkesaktivitet> kortvarige = KortvarigArbeidsforholdTjeneste.hentAndelerForKortvarigeArbeidsforhold(
            behandlingReferanse.getAktørId(),
            beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId()),
            iayTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId())
        );

        // Assert
        assertThat(kortvarige).isEmpty();
    }

    @Test
    public void skalIkkjeGiKortvarigForArbeidsforholdPå6MndIMånederMedUlikDato4() {
        // Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        String orgnr = "123456780";
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, LocalDate.of(2018, 9, 30), LocalDate.of(2018, 8, 30),
            LocalDate.of(2019, 2, 28), arbId, Arbeidsgiver.virksomhet(orgnr));
        beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING);

        // Act
        Map<BeregningsgrunnlagPrStatusOgAndel, Yrkesaktivitet> kortvarige = KortvarigArbeidsforholdTjeneste.hentAndelerForKortvarigeArbeidsforhold(
            behandlingReferanse.getAktørId(),
            beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId()),
            iayTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId())
        );

        // Assert
        assertThat(kortvarige).isEmpty();
    }

    @Test
    public void skalIkkjeGiKortvarigForArbeidsforholdSomStarterPåSkjæringstidspunktet() {
        // Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        String orgnr = "123456780";
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(5).minusDays(1), arbId, Arbeidsgiver.virksomhet(orgnr));
        beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING);

        // Act
        Map<BeregningsgrunnlagPrStatusOgAndel, Yrkesaktivitet> kortvarige = KortvarigArbeidsforholdTjeneste.hentAndelerForKortvarigeArbeidsforhold(
            behandlingReferanse.getAktørId(),
            beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId()),
            iayTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId())
        );

        // Assert
        assertThat(kortvarige).isEmpty();
    }

    @Test
    public void skalIkkjeGiKortvarigForArbeidsforholdSomStarterEtterSkjæringstidspunktet() {
        // Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        String orgnr = "123456780";
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.plusDays(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(5).minusDays(1), arbId, Arbeidsgiver.virksomhet(orgnr));
        beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING);

        // Act
        Map<BeregningsgrunnlagPrStatusOgAndel, Yrkesaktivitet> kortvarige = KortvarigArbeidsforholdTjeneste.hentAndelerForKortvarigeArbeidsforhold(
            behandlingReferanse.getAktørId(),
            beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId()),
            iayTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId())
        );

        // Assert
        assertThat(kortvarige).isEmpty();
    }


    @Test
    public void skalGiKortvarigForArbeidsforholdPå6MndMinusEinDag() {
        // Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        String orgnr = "123456780";
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(5).minusDays(2), arbId, Arbeidsgiver.virksomhet(orgnr));
        beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING);

        // Act
        Map<BeregningsgrunnlagPrStatusOgAndel, Yrkesaktivitet> kortvarige = KortvarigArbeidsforholdTjeneste.hentAndelerForKortvarigeArbeidsforhold(
            behandlingReferanse.getAktørId(),
            beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId()),
            iayTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId())
        );

        // Assert
        List<Yrkesaktivitet> kortvarigeYrkesaktiviteter = new ArrayList<>(kortvarige.values());
        assertThat(kortvarigeYrkesaktiviteter).hasSize(1);
    }

    @Test
    public void skalGiKortvarigForArbeidsforholdSomStarterDagenFørSkjæringstidspunktet() {
        // Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        String orgnr = "123456780";
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(5).minusDays(2), arbId, Arbeidsgiver.virksomhet(orgnr));
        beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING);
        // Act
        Map<BeregningsgrunnlagPrStatusOgAndel, Yrkesaktivitet> kortvarige = KortvarigArbeidsforholdTjeneste.hentAndelerForKortvarigeArbeidsforhold(
            behandlingReferanse.getAktørId(),
            beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId()),
            iayTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId())
        );

        // Assert
        List<Yrkesaktivitet> kortvarigeYrkesaktiviteter = new ArrayList<>(kortvarige.values());
        assertThat(kortvarigeYrkesaktiviteter).hasSize(1);
    }

    @Test
    public void skalGiKortvarigVedKombinasjonMedDagpenger() {
        // Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        String orgnr = "123456780";
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(5).minusDays(2), arbId, Arbeidsgiver.virksomhet(orgnr));
        beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, AktivitetStatus.DAGPENGER);

        // Act
        Map<BeregningsgrunnlagPrStatusOgAndel, Yrkesaktivitet> kortvarige = KortvarigArbeidsforholdTjeneste.hentAndelerForKortvarigeArbeidsforhold(
            behandlingReferanse.getAktørId(),
            beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId()),
            iayTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId())
        );

        // Assert
        assertThat(kortvarige.values()).hasSize(1);
    }


    @Test
    public void skalGiToKortvarigeArbeidsforhold() {
        // Arrange
        var arbId1 = InternArbeidsforholdRef.nyRef();
        var arbId2 = InternArbeidsforholdRef.nyRef();
        String orgnr1 = "123456780";
        String orgnr2 = "123456644";
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(5).minusDays(10), arbId1, Arbeidsgiver.virksomhet(orgnr1));
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(2).minusDays(2), arbId2, Arbeidsgiver.virksomhet(orgnr2));
        beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING);

        // Act
        Map<BeregningsgrunnlagPrStatusOgAndel, Yrkesaktivitet> kortvarige = KortvarigArbeidsforholdTjeneste.hentAndelerForKortvarigeArbeidsforhold(
            behandlingReferanse.getAktørId(),
            beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId()),
            iayTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId())
        );

        // Assert
        List<Yrkesaktivitet> kortvarigeYrkesaktiviteter = new ArrayList<>(kortvarige.values());
        assertThat(kortvarigeYrkesaktiviteter).hasSize(2);
    }

    @Test
    public void skalIkkeGiKortvarigArbeidsforholdDersomBrukerErSN() {
        // Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        String orgnr = "123456780";
        // Lager et kortvarig arbeidsforhold
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(5).minusDays(2), arbId, Arbeidsgiver.virksomhet(orgnr));
        iayTestUtil.lagOppgittOpptjeningForSN(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, false);
        beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, AktivitetStatus.KOMBINERT_AT_SN);

        // Act
        boolean resultat = KortvarigArbeidsforholdTjeneste.harKortvarigeArbeidsforholdOgErIkkeSN(behandlingReferanse.getAktørId(),
            beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId()),
            iayTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId())
        );

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skalGiKortvarigArbeidsforholdDersomBrukerIkkeErSN() {
        // Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        String orgnr = "123456780";
        // Lager et kortvarig arbeidsforhold
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(5).minusDays(2), arbId, Arbeidsgiver.virksomhet(orgnr));
        beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING);

        // Act
        boolean resultat = KortvarigArbeidsforholdTjeneste.harKortvarigeArbeidsforholdOgErIkkeSN(
            behandlingReferanse.getAktørId(),
            beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId()),
            iayTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId())
        );

        // Assert
        assertThat(resultat).isTrue();
    }

}
