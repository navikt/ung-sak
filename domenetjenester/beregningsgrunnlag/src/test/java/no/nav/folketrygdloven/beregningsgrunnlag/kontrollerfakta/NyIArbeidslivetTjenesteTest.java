package no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.util.BeregningsgrunnlagTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningIAYTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.k9.kodeverk.iay.AktivitetStatus;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class NyIArbeidslivetTjenesteTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, 9, 30);
    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private RepositoryProvider repositoryProvider = new RepositoryProvider(repoRule.getEntityManager());
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository = repositoryProvider.getBeregningsgrunnlagRepository();

    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;

    @Inject
    private BeregningIAYTestUtil iayTestUtil;
    @Inject
    private BeregningsgrunnlagTestUtil beregningTestUtil;

    private BehandlingReferanse behandlingReferanse;

    @Before
    public void setup() {
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = scenario.lagre(repositoryProvider);
    }

    @Test
    public void skalGiTilfelleDeromBrukerErSNOgNyIArbeidslivet() {
        // Arrange
        // Lager et kortvarig arbeidsforhold
        iayTestUtil.lagOppgittOpptjeningForSN(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, true);
        beregningTestUtil.lagGjeldendeBeregningsgrunnlag(lagReferanseMedSkjæringstidspunkt(behandlingReferanse), SKJÆRINGSTIDSPUNKT_OPPTJENING, AktivitetStatus.KOMBINERT_AT_SN);

        // Act
        boolean resultat = NyIArbeidslivetTjeneste.erNyIArbeidslivetMedAktivitetStatusSN(beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId()), iayTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId()));

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void skalIkkeGiTilfelleDeromBrukerIkkeErNyIArbeidslivet() {
        // Arrange
        iayTestUtil.lagOppgittOpptjeningForSN(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, false);
        beregningTestUtil.lagGjeldendeBeregningsgrunnlag(lagReferanseMedSkjæringstidspunkt(behandlingReferanse), SKJÆRINGSTIDSPUNKT_OPPTJENING, AktivitetStatus.KOMBINERT_AT_SN);

        // Act
        Long behandlingId = behandlingReferanse.getId();
        boolean resultat = NyIArbeidslivetTjeneste.erNyIArbeidslivetMedAktivitetStatusSN(beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingId), iayTjeneste.hentGrunnlag(behandlingId));
            iayTjeneste.finnGrunnlag(behandlingId);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skalIkkeGiTilfelleForNyIArbeidslivetSNDeromBrukerIkkeErSN() {
        // Arrange
        // Lager et kortvarig arbeidsforhold
        var arbId = InternArbeidsforholdRef.nyRef();
        String orgnr = "123456780";
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(5).minusDays(2), arbId, Arbeidsgiver.virksomhet(orgnr));
        beregningTestUtil.lagGjeldendeBeregningsgrunnlag(lagReferanseMedSkjæringstidspunkt(behandlingReferanse), SKJÆRINGSTIDSPUNKT_OPPTJENING, AktivitetStatus.ARBEIDSTAKER);

        // Act
        Long behandlingId = behandlingReferanse.getId();
        boolean resultat = NyIArbeidslivetTjeneste.erNyIArbeidslivetMedAktivitetStatusSN(beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingId), iayTjeneste.hentGrunnlag(behandlingId));
            iayTjeneste.finnGrunnlag(behandlingId);

        // Assert
        assertThat(resultat).isFalse();
    }

    private static BehandlingReferanse lagReferanseMedSkjæringstidspunkt(BehandlingReferanse behandlingReferanse) {
        return behandlingReferanse.medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING);
    }
}
