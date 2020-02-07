package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import no.nav.folketrygdloven.beregningsgrunnlag.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.HentBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.input.OpptjeningForBeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.AndelGraderingTjeneste;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.KontrollerFaktaBeregningSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

@RunWith(CdiRunner.class)
public class KontrollerFaktaBeregningStegTest {


    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();
   
    @Inject
    private BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    
    private HentBeregningsgrunnlagTjeneste hentBeregningsgrunnlagTjeneste = new HentBeregningsgrunnlagTjeneste(repoRule.getEntityManager());
    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    private BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private KontrollerFaktaBeregningSteg steg;
    private Behandling behandling;

    @Before
    public void setUp() {
        var scenario = TestScenarioBuilder.builderMedSøknad();
        var andelGraderingTjeneste = Mockito.mock(AndelGraderingTjeneste.class);
        var iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        var skjæringstidspunktTjeneste = Mockito.mock(SkjæringstidspunktTjeneste.class);
        var opptjeningForBeregningTjeneste = Mockito.mock(OpptjeningForBeregningTjeneste.class);
        var inputProvider = Mockito.mock(BeregningsgrunnlagInputProvider.class);
        behandling = scenario.lagre(repositoryProvider);

        var inputTjeneste = new BeregningsgrunnlagInputTjeneste(repositoryProvider, iayTjeneste, skjæringstidspunktTjeneste, andelGraderingTjeneste, opptjeningForBeregningTjeneste);
        when(inputProvider.getTjeneste(FagsakYtelseType.FORELDREPENGER)).thenReturn(inputTjeneste);
        steg = new KontrollerFaktaBeregningSteg(beregningsgrunnlagTjeneste, behandlingRepository, hentBeregningsgrunnlagTjeneste, inputProvider);
        lagreBeregningsgrunnlag(false, BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
    }

    @Test
    public void skal_ikke_reaktivere_grunnlag_ved_hopp_bakover_og_overstyring() {
        // Arrange
        BeregningsgrunnlagTilstand overstyrtTilstand = BeregningsgrunnlagTilstand.KOFAKBER_UT;
        lagreBeregningsgrunnlag(true, overstyrtTilstand);
        BehandlingskontrollKontekst kontekst = lagBehandlingskontrollkontekst();
        BehandlingStegType tilSteg = BehandlingStegType.KONTROLLER_FAKTA_BEREGNING;
        BehandlingStegType fraSteg = BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG;
        // Act
        steg.vedHoppOverBakover(kontekst, null, tilSteg, fraSteg);
        // Assert
        Optional<BeregningsgrunnlagGrunnlagEntitet> aktivtGrunnlag = hentBeregningsgrunnlagTjeneste.hentBeregningsgrunnlagGrunnlagEntitet(behandling.getId());
        assertThat(aktivtGrunnlag.get().getBeregningsgrunnlagTilstand()).isEqualTo(overstyrtTilstand);
    }

    @Test
    public void skal_reaktivere_grunnlag_ved_hopp_bakover_uten_overstyring() {
        // Arrange
        BeregningsgrunnlagTilstand overstyrtTilstand = BeregningsgrunnlagTilstand.KOFAKBER_UT;
        lagreBeregningsgrunnlag(false, overstyrtTilstand);
        BehandlingskontrollKontekst kontekst = lagBehandlingskontrollkontekst();
        BehandlingStegType tilSteg = BehandlingStegType.KONTROLLER_FAKTA_BEREGNING;
        BehandlingStegType fraSteg = BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG;
        // Act
        steg.vedHoppOverBakover(kontekst, null, tilSteg, fraSteg);
        // Assert
        Optional<BeregningsgrunnlagGrunnlagEntitet> aktivtGrunnlag = hentBeregningsgrunnlagTjeneste.hentBeregningsgrunnlagGrunnlagEntitet(behandling.getId());
        assertThat(aktivtGrunnlag.get().getBeregningsgrunnlagTilstand()).isEqualTo(BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
    }


    private void lagreBeregningsgrunnlag(boolean overstyrt, BeregningsgrunnlagTilstand tilstand) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .medOverstyring(overstyrt).build();
        beregningsgrunnlagTjeneste.lagreBeregningsgrunnlag(behandling.getId(), beregningsgrunnlag, tilstand);
    }

    private BehandlingskontrollKontekst lagBehandlingskontrollkontekst() {
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling.getId());
        return new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(),
            behandlingLås);
    }
}
