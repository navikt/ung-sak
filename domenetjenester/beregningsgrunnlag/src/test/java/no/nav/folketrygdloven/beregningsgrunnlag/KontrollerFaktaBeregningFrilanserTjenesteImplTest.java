package no.nav.folketrygdloven.beregningsgrunnlag;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.folketrygdloven.beregningsgrunnlag.adapter.util.BeregningsgrunnlagTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.KontrollerFaktaBeregningFrilanserTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningIAYTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.k9.kodeverk.iay.AktivitetStatus;
import no.nav.k9.kodeverk.iay.ArbeidType;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class KontrollerFaktaBeregningFrilanserTjenesteImplTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, 9, 30);
    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private RepositoryProvider repositoryProvider = new RepositoryProvider(repoRule.getEntityManager());
    @Inject
    private BeregningIAYTestUtil iayTestUtil;
    @Inject
    private BeregningsgrunnlagTestUtil beregningTestUtil;

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository = repositoryProvider.getBeregningsgrunnlagRepository();

    private BehandlingReferanse behandlingReferanse;

    @Before
    public void setup() {
        inntektArbeidYtelseTjeneste = iayTestUtil.getIayTjeneste();
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = scenario.lagre(repositoryProvider);
    }

    @Test
    public void ikkeFrilansISammeArbeidsforholdHvisBareArbeidstaker() {
        //Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        String orgnr = "123456780";
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(5).minusDays(2), arbId, Arbeidsgiver.virksomhet(orgnr));
        beregningTestUtil.lagGjeldendeBeregningsgrunnlag(lagReferanseMedStp(behandlingReferanse), SKJÆRINGSTIDSPUNKT_OPPTJENING);

        //Act
        Set<Arbeidsgiver> brukerErArbeidstakerOgFrilanserISammeOrganisasjon = KontrollerFaktaBeregningFrilanserTjeneste.brukerErArbeidstakerOgFrilanserISammeOrganisasjon(
            behandlingReferanse.getAktørId(), beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId()), inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId()));

        //Assert
        assertThat(brukerErArbeidstakerOgFrilanserISammeOrganisasjon).isEmpty();
    }

    @Test
    public void ikkeFrilansISammeArbeidsforholdHvisFrilansHosAnnenOppdragsgiver() {
        //Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        String orgnr = "123456780";
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(5).minusDays(2), arbId, Arbeidsgiver.virksomhet(orgnr));
        String orgnrFrilans = "987654320";
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(5).minusDays(2),
            (InternArbeidsforholdRef) null, Arbeidsgiver.virksomhet(orgnrFrilans), ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER,
            singletonList(BigDecimal.TEN), false, Optional.empty());
        beregningTestUtil.lagGjeldendeBeregningsgrunnlag(lagReferanseMedStp(behandlingReferanse), SKJÆRINGSTIDSPUNKT_OPPTJENING, AktivitetStatus.KOMBINERT_AT_FL);

        //Act
        Set<Arbeidsgiver> brukerErArbeidstakerOgFrilanserISammeOrganisasjon = KontrollerFaktaBeregningFrilanserTjeneste.brukerErArbeidstakerOgFrilanserISammeOrganisasjon(
            behandlingReferanse.getAktørId(), beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId()), inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId()));

        //Assert
        assertThat(brukerErArbeidstakerOgFrilanserISammeOrganisasjon).isEmpty();
    }

    @Test
    public void frilansISammeArbeidsforhold() {
        //Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        String orgnr = "123456780";
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(5).minusDays(2), arbId, Arbeidsgiver.virksomhet(orgnr));
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(5).minusDays(2), (InternArbeidsforholdRef) null, Arbeidsgiver.virksomhet(orgnr),
            ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER, singletonList(BigDecimal.TEN), false, Optional.empty());
        beregningTestUtil.lagGjeldendeBeregningsgrunnlag(lagReferanseMedStp(behandlingReferanse), SKJÆRINGSTIDSPUNKT_OPPTJENING, AktivitetStatus.KOMBINERT_AT_FL);

        //Act
        Set<Arbeidsgiver> brukerErArbeidstakerOgFrilanserISammeOrganisasjon = KontrollerFaktaBeregningFrilanserTjeneste.brukerErArbeidstakerOgFrilanserISammeOrganisasjon(behandlingReferanse.getAktørId(),
            beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId()),
            inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId()));

        //Assert
        assertThat(brukerErArbeidstakerOgFrilanserISammeOrganisasjon).hasSize(1);
    }

    private static BehandlingReferanse lagReferanseMedStp(BehandlingReferanse behandlingReferanse) {
        return behandlingReferanse.medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING);
    }
}
