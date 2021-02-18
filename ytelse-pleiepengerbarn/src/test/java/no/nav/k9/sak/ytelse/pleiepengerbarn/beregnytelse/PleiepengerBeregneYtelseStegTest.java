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
import java.util.Set;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import no.nav.k9.sak.behandlingslager.behandling.beregning.RegelData;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.k9.sak.test.util.behandling.AbstractTestScenario;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.ytelse.beregning.BeregnFeriepengerTjeneste;
import no.nav.k9.sak.ytelse.beregning.FastsettBeregningsresultatTjeneste;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakInMemoryTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.AnnenPart;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.vedtak.util.Tuple;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class PleiepengerBeregneYtelseStegTest {

    @Inject
    private EntityManager entityManager;

    private BehandlingRepositoryProvider repositoryProvider;
    private BeregningsresultatRepository beregningsresultatRepository;
    private BehandlingRepository behandlingRepository;

    @Inject
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    @Inject
    private UttakInMemoryTjeneste uttakTjeneste;
    @Inject
    private KalkulusInMemoryTjeneste kalkulusTjeneste;
    @Mock
    private FastsettBeregningsresultatTjeneste fastsettBeregningsresultatTjeneste = mock(FastsettBeregningsresultatTjeneste.class);
    private BeregnFeriepengerTjeneste beregnFeriepengerTjeneste = mock(BeregnFeriepengerTjeneste.class);
    private BeregningPerioderGrunnlagRepository bgGrunnlagRepository;
    private BeregningTjeneste beregningTjeneste;

    private PleiepengerBeregneYtelseSteg steg;
    private BeregningsresultatEntitet beregningsresultat;

    @BeforeEach
    public void setup() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        bgGrunnlagRepository = new BeregningPerioderGrunnlagRepository(entityManager, repositoryProvider.getVilkårResultatRepository());
        beregningTjeneste = new BeregningsgrunnlagTjeneste(new UnitTestLookupInstanceImpl<>(kalkulusTjeneste), repositoryProvider.getVilkårResultatRepository(), bgGrunnlagRepository);
        beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput("regelInput")
            .medRegelSporing("regelSporing")
            .build();
        steg = new PleiepengerBeregneYtelseSteg(repositoryProvider, beregningTjeneste,
            fastsettBeregningsresultatTjeneste,
            uttakTjeneste,
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

        Optional<BeregningsresultatEntitet> beregningsresultat = beregningsresultatRepository.hentBgBeregningsresultat(behandling.getId());
        assertThat(beregningsresultat).hasValueSatisfying(resultat -> {
            assertThat(resultat).isNotNull();
            assertThat(resultat.getRegelInput()).as("regelInput").isEqualTo(new RegelData("regelInput"));
            assertThat(resultat.getRegelSporing()).as("regelSporing").isEqualTo(new RegelData("regelSporing"));
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
        Optional<BeregningsresultatEntitet> resultat = beregningsresultatRepository.hentBgBeregningsresultat(behandling.getId());
        assertThat(resultat).isNotPresent();
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
        var periode = new LukketPeriode(stp, stp.plusDays(2));
        var uttaksplan = new Uttaksplan(Map.of(periode, new UttaksperiodeInfo(no.nav.pleiepengerbarn.uttak.kontrakter.Utfall.OPPFYLT,
            BigDecimal.valueOf(100), List.of(), Set.of(), Map.of(), null, Set.of(), behandling.getUuid().toString(), AnnenPart.ALENE)));

        uttakTjeneste.lagreUttakResultatPerioder(behandling.getFagsak().getSaksnummer(), behandling.getUuid(), uttaksplan);
    }
}
