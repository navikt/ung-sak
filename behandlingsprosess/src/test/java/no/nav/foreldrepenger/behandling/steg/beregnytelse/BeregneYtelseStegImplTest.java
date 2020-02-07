package no.nav.foreldrepenger.behandling.steg.beregnytelse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import no.nav.folketrygdloven.beregningsgrunnlag.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.HentBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.steg.beregnytelse.BeregneYtelseStegImpl;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.ytelse.beregning.BeregnFeriepengerTjeneste;
import no.nav.foreldrepenger.ytelse.beregning.FastsettBeregningsresultatTjeneste;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.kodeverk.uttak.PeriodeResultatType;
import no.nav.k9.kodeverk.uttak.PeriodeResultatÅrsak;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import no.nav.vedtak.util.Tuple;

@RunWith(CdiRunner.class)
public class BeregneYtelseStegImplTest {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    @Inject
    private UttakInputTjeneste uttakInputTjeneste;

    @Inject
    private HentBeregningsgrunnlagTjeneste hentBeregningsgrunnlagTjeneste;

    @Inject
    private BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    
    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private final BeregningsresultatRepository beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
    private final UttakRepository uttakRepository = repositoryProvider.getUttakRepository();
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();

    @Mock
    private FastsettBeregningsresultatTjeneste fastsettBeregningsresultatTjeneste = mock(FastsettBeregningsresultatTjeneste.class);
    private BeregnFeriepengerTjeneste beregnFeriepengerTjeneste = mock(BeregnFeriepengerTjeneste.class);

    private BeregneYtelseStegImpl steg;
    private BeregningsresultatEntitet beregningsresultat;

    @Before
    public void setup() {
        beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput("regelInput")
            .medRegelSporing("regelSporing")
            .build();
        steg = new BeregneYtelseStegImpl(repositoryProvider, hentBeregningsgrunnlagTjeneste, 
            uttakInputTjeneste, 
            fastsettBeregningsresultatTjeneste,
            new UnitTestLookupInstanceImpl<>(beregnFeriepengerTjeneste));
    }

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @Test
    public void skalUtførStegForFørstegangsbehandling() {
        // Arrange

        when(fastsettBeregningsresultatTjeneste.fastsettBeregningsresultat(Mockito.any(), Mockito.any())).thenReturn(beregningsresultat);

        Tuple<Behandling, BehandlingskontrollKontekst> behandlingKontekst = byggGrunnlag(true, true);
        Behandling behandling = behandlingKontekst.getElement1();
        BehandlingskontrollKontekst kontekst = behandlingKontekst.getElement2();

        // Act
        BehandleStegResultat stegResultat = steg.utførSteg(kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);

        Optional<BeregningsresultatEntitet> beregningsresultat = beregningsresultatRepository.hentBeregningsresultat(behandling.getId());
        assertThat(beregningsresultat).hasValueSatisfying(resultat -> {
            assertThat(resultat).isNotNull();
            assertThat(resultat.getRegelInput()).as("regelInput").isEqualTo("regelInput");
            assertThat(resultat.getRegelSporing()).as("regelSporing").isEqualTo("regelSporing");
        });
    }

    @Test
    public void skalSletteBeregningsresultatFPVedTilbakehopp() {
        // Arrange
        Tuple<Behandling, BehandlingskontrollKontekst> behandlingKontekst = byggGrunnlag(true, true);
        Behandling behandling = behandlingKontekst.getElement1();
        BehandlingskontrollKontekst kontekst = behandlingKontekst.getElement2();
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Act
        steg.vedHoppOverBakover(kontekst, null, null, null);

        // Assert
        Optional<BeregningsresultatEntitet> resultat = beregningsresultatRepository.hentBeregningsresultat(behandling.getId());
        assertThat(resultat).isNotPresent();
    }

    @Test
    public void skalKasteFeilNårBeregningsgrunnlagMangler() {
        // Assert
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Mangler Beregningsgrunnlag for behandling");

        // Arrange
        Tuple<Behandling, BehandlingskontrollKontekst> behandlingKontekst = byggGrunnlag(false, true);
        BehandlingskontrollKontekst kontekst = behandlingKontekst.getElement2();

        // Act
        steg.utførSteg(kontekst);
    }

    private Tuple<Behandling, BehandlingskontrollKontekst> byggGrunnlag(boolean medBeregningsgrunnlag, boolean medUttaksPlanResultat) {
        var scenario = TestScenarioBuilder.builderMedSøknad();

        var behandling = lagre(scenario);

        if (medBeregningsgrunnlag) {
            medBeregningsgrunnlag(behandling);
        }

        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        if (medUttaksPlanResultat) {
            byggUttakPlanResultat(behandling);
        }
        return new Tuple<>(behandling, kontekst);
    }

    private void medBeregningsgrunnlag(Behandling behandling) {
        var beregningsgrunnlagBuilder = BeregningsgrunnlagEntitet.builder()
                .medSkjæringstidspunkt(LocalDate.now())
                .medGrunnbeløp(BigDecimal.valueOf(90000));
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningsgrunnlagBuilder.build();
        beregningsgrunnlagTjeneste.lagreBeregningsgrunnlag(behandling.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.OPPRETTET);
    }

    private void byggUttakPlanResultat(Behandling behandling) {
        var periode = new UttakResultatPeriodeEntitet.Builder(LocalDate.now().minusDays(3), LocalDate.now().minusDays(1))
            .medPeriodeResultat(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();

        var perioder = new UttakResultatPerioderEntitet();
        perioder.leggTilPeriode(periode);

        uttakRepository.lagreOpprinneligUttakResultatPerioder(behandling.getId(), perioder);
    }
}
