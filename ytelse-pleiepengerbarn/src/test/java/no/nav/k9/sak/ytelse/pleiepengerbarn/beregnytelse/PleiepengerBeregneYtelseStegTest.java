package no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusInMemoryTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.domene.uttak.UttakInMemoryTjeneste;
import no.nav.k9.sak.domene.uttak.uttaksplan.InnvilgetUttaksplanperiode;
import no.nav.k9.sak.domene.uttak.uttaksplan.Uttaksplan;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.test.util.behandling.AbstractTestScenario;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.ytelse.beregning.BeregnFeriepengerTjeneste;
import no.nav.k9.sak.ytelse.beregning.FastsettBeregningsresultatTjeneste;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPeriode;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import no.nav.vedtak.util.Tuple;

@RunWith(CdiRunner.class)
public class PleiepengerBeregneYtelseStegTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private final BeregningsresultatRepository beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    @Inject
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    @Inject
    private UttakInMemoryTjeneste uttakTjeneste;
    @Inject
    private KalkulusInMemoryTjeneste kalkulusTjeneste;
    @Mock
    private FastsettBeregningsresultatTjeneste fastsettBeregningsresultatTjeneste = mock(FastsettBeregningsresultatTjeneste.class);
    private BeregnFeriepengerTjeneste beregnFeriepengerTjeneste = mock(BeregnFeriepengerTjeneste.class);
    private BeregningPerioderGrunnlagRepository bgGrunnlagRepository = new BeregningPerioderGrunnlagRepository(repoRule.getEntityManager(), repositoryProvider.getVilkårResultatRepository());
    private BeregningTjeneste beregningTjeneste;

    private PleiepengerBeregneYtelseSteg steg;
    private BeregningsresultatEntitet beregningsresultat;

    @Before
    public void setup() {
        beregningTjeneste = new BeregningsgrunnlagTjeneste(new UnitTestLookupInstanceImpl<>(kalkulusTjeneste), behandlingRepository, repositoryProvider.getVilkårResultatRepository(), bgGrunnlagRepository);
        beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput("regelInput")
            .medRegelSporing("regelSporing")
            .build();
        steg = new PleiepengerBeregneYtelseSteg(repositoryProvider, beregningTjeneste,
            uttakTjeneste,
            fastsettBeregningsresultatTjeneste,
            new UnitTestLookupInstanceImpl<>(beregnFeriepengerTjeneste));
    }

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void skalUtførStegForFørstegangsbehandling() {
        // Arrange

        when(fastsettBeregningsresultatTjeneste.fastsettBeregningsresultat(any(List.class), Mockito.any())).thenReturn(beregningsresultat);

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
        Assert.assertThrows("Mangler Beregningsgrunnlag for behandling", IllegalStateException.class, () -> {
            // Assert

            // Arrange
            Tuple<Behandling, BehandlingskontrollKontekst> behandlingKontekst = byggGrunnlag(false, true);
            BehandlingskontrollKontekst kontekst = behandlingKontekst.getElement2();

            // Act
            steg.utførSteg(kontekst);
        });
    }

    private Tuple<Behandling, BehandlingskontrollKontekst> byggGrunnlag(boolean medBeregningsgrunnlag, boolean medUttaksPlanResultat) {
        var scenario = TestScenarioBuilder.builderMedSøknad();

        var behandling = lagre(scenario);
        var stp = LocalDate.now().minusDays(3);

        if (medBeregningsgrunnlag) {
            medBeregningsgrunnlag(behandling, stp);
        }

        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        var vilkårResultatBuilder = new VilkårResultatBuilder();
        vilkårResultatBuilder.leggTil(vilkårResultatBuilder
            .hentBuilderFor(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .leggTil(new VilkårPeriodeBuilder()
                .medUtfall(Utfall.OPPFYLT)
                .medPeriode(stp, stp.plusDays(2))));

        repositoryProvider.getVilkårResultatRepository().lagre(behandling.getId(), vilkårResultatBuilder.build());
        if (medUttaksPlanResultat) {
            byggUttakPlanResultat(behandling, stp);
        }
        return new Tuple<>(behandling, kontekst);
    }

    private void medBeregningsgrunnlag(Behandling behandling, LocalDate stp) {
        var beregningsgrunnlagBuilder = Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(stp)
            .medGrunnbeløp(BigDecimal.valueOf(90000));
        Beregningsgrunnlag beregningsgrunnlag = beregningsgrunnlagBuilder.build();
        kalkulusTjeneste.lagreBeregningsgrunnlag(BehandlingReferanse.fra(behandling), beregningsgrunnlag, BeregningsgrunnlagTilstand.OPPRETTET);
        bgGrunnlagRepository.lagre(behandling.getId(), new BeregningsgrunnlagPeriode(behandling.getUuid(), stp));
    }

    private void byggUttakPlanResultat(Behandling behandling, LocalDate stp) {
        var periode = new Periode(stp, stp.plusDays(2));
        var uttaksplan = new Uttaksplan(Map.of(periode, new InnvilgetUttaksplanperiode(100, List.of())));

        uttakTjeneste.lagreUttakResultatPerioder(behandling.getFagsak().getSaksnummer(), behandling.getUuid(), uttaksplan);
    }
}
