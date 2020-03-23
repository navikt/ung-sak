package no.nav.k9.sak.web.app.tjenester.behandling.vilkår;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltVerdiType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.kontrakt.opptjening.OverstyringOpptjeningsvilkåretDto;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktApplikasjonTjeneste;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class OpptjeningsvilkåretOverstyringshåndtererTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    @Inject
    private AksjonspunktApplikasjonTjeneste aksjonspunktApplikasjonTjeneste;

    @Test
    public void skal_opprette_aksjonspunkt_for_overstyring() {
        // Arrange
        // Behandling
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING,
            BehandlingStegType.VURDER_OPPTJENINGSVILKÅR);
        scenario.leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, Utfall.OPPFYLT);
        scenario.lagre(repositoryProvider);

        Behandling behandling = scenario.getBehandling();
        // Dto
        OverstyringOpptjeningsvilkåretDto overstyringspunktDto = new OverstyringOpptjeningsvilkåretDto(false,
            "test av overstyring opptjeningsvilkåret", "1035");
        assertThat(behandling.getAksjonspunkter()).hasSize(1);

        // Act
        aksjonspunktApplikasjonTjeneste.overstyrAksjonspunkter(Set.of(overstyringspunktDto), behandling.getId());

        // Assert
        Set<Aksjonspunkt> aksjonspunktSet = behandling.getAksjonspunkter();
        assertThat(aksjonspunktSet).hasSize(2);
        assertThat(aksjonspunktSet).extracting("aksjonspunktDefinisjon")
            .contains(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
        assertThat(aksjonspunktSet.stream()
            .filter(ap -> ap.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.OVERSTYRING_AV_OPPTJENINGSVILKÅRET)))
            .anySatisfy(ap -> assertThat(ap.getStatus()).isEqualTo(AksjonspunktStatus.UTFØRT));
    }

    @Test
    public void skal_få_historikkinnslag_når_opptjening_er_overstyrt() {
        // Arrange
        // Behandling
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING,
            BehandlingStegType.VURDER_OPPTJENINGSVILKÅR);
        scenario.leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, Utfall.OPPFYLT);
        scenario.lagre(repositoryProvider);

        Behandling behandling = scenario.getBehandling();
        // Dto
        OverstyringOpptjeningsvilkåretDto overstyringspunktDto = new OverstyringOpptjeningsvilkåretDto(false,
            "test av overstyring opptjeningsvilkåret", "1035");

        // Act
        aksjonspunktApplikasjonTjeneste.overstyrAksjonspunkter(Set.of(overstyringspunktDto), behandling.getId());

        // Assert
        List<Historikkinnslag> historikkinnslagene = repositoryProvider.getHistorikkRepository().hentHistorikk(behandling.getId());
        assertThat(historikkinnslagene.get(0).getHistorikkinnslagDeler()).hasSize(1);
        List<HistorikkinnslagFelt> feltList = historikkinnslagene.get(0).getHistorikkinnslagDeler().get(0).getEndredeFelt();
        assertThat(feltList).hasSize(1);
        HistorikkinnslagFelt felt = feltList.get(0);
        assertThat(felt.getNavn()).as("navn").isEqualTo(HistorikkEndretFeltType.OVERSTYRT_VURDERING.getKode());
        assertThat(felt.getFraVerdi()).as("fraVerdi").isEqualTo(HistorikkEndretFeltVerdiType.VILKAR_OPPFYLT.getKode());
        assertThat(felt.getTilVerdi()).as("tilVerdi").isEqualTo(HistorikkEndretFeltVerdiType.VILKAR_IKKE_OPPFYLT.getKode());
    }

    @Test
    public void skal_feile_hvis_det_forsøkes_å_overstyre_uten_aktiviteter_i_opptjening() {
        // Arrange
        // Behandling
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING,
            BehandlingStegType.VURDER_OPPTJENINGSVILKÅR);
        scenario.leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, Utfall.OPPFYLT);
        scenario.lagre(repositoryProvider);

        Behandling behandling = scenario.getBehandling();
        // Dto
        OverstyringOpptjeningsvilkåretDto overstyringspunktDto = new OverstyringOpptjeningsvilkåretDto(true,
            "test av overstyring opptjeningsvilkåret", "1035");

        // Act
        try {
            aksjonspunktApplikasjonTjeneste.overstyrAksjonspunkter(Set.of(overstyringspunktDto), behandling.getId());
            fail("Skal kaste exception");
        } catch (FunksjonellException e) {
            assertThat(e).hasMessage("FP-093923:Kan ikke overstyre vilkår. Det må være minst en aktivitet for at opptjeningsvilkåret skal kunne overstyres.");
        }
    }
}
