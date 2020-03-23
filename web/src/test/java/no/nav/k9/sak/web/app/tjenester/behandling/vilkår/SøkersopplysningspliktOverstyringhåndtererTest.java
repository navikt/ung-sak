package no.nav.k9.sak.web.app.tjenester.behandling.vilkår;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
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
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.søker.OverstyringSokersOpplysingspliktDto;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktApplikasjonTjeneste;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class SøkersopplysningspliktOverstyringhåndtererTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    @Inject
    private AksjonspunktApplikasjonTjeneste aksjonspunktApplikasjonTjeneste;

    @Test
    public void skal_generere_historikkinnslag_ved_avklaring_av_søkers_opplysningsplikt_overstyrt() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_MARKERING_AV_UTLAND_SAKSTYPE,
            BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR);
        scenario.lagre(repositoryProvider);

        Behandling behandling = scenario.getBehandling();
        final var vilkårene = Vilkårene.builder()
            .leggTilIkkeVurderteVilkår(List.of(DatoIntervallEntitet.fraOgMed(LocalDate.now())), VilkårType.SØKERSOPPLYSNINGSPLIKT)
            .build();
        repositoryProvider.getVilkårResultatRepository().lagre(behandling.getId(), vilkårene);
        // Dto
        OverstyringSokersOpplysingspliktDto overstyringspunktDto = new OverstyringSokersOpplysingspliktDto(false,
            "test av overstyring");
        assertThat(behandling.getAksjonspunkter()).hasSize(1);

        // Act
        aksjonspunktApplikasjonTjeneste.overstyrAksjonspunkter(Set.of(overstyringspunktDto), behandling.getId());

        // Assert
        List<Historikkinnslag> historikkinnslagene = repositoryProvider.getHistorikkRepository().hentHistorikk(behandling.getId());
        assertThat(historikkinnslagene.get(0).getHistorikkinnslagDeler()).hasSize(1);
        List<HistorikkinnslagFelt> feltList = historikkinnslagene.get(0).getHistorikkinnslagDeler().get(0).getEndredeFelt();
        assertThat(feltList).hasSize(1);
        HistorikkinnslagFelt felt = feltList.get(0);
        assertThat(felt.getNavn()).as("navn").isEqualTo(HistorikkEndretFeltType.SOKERSOPPLYSNINGSPLIKT.getKode());
        assertThat(felt.getFraVerdi()).as("fraVerdi").isNull();
        assertThat(felt.getTilVerdi()).as("tilVerdi").isEqualTo(HistorikkEndretFeltVerdiType.IKKE_OPPFYLT.getKode());

        Set<Aksjonspunkt> aksjonspunktSet = behandling.getAksjonspunkter();

        assertThat(aksjonspunktSet).extracting("aksjonspunktDefinisjon").contains(AksjonspunktDefinisjon.SØKERS_OPPLYSNINGSPLIKT_OVST);

        assertThat(aksjonspunktSet.stream()
            .filter(ap -> ap.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.SØKERS_OPPLYSNINGSPLIKT_OVST)))
            .anySatisfy(ap -> assertThat(ap.getStatus()).isEqualTo(AksjonspunktStatus.UTFØRT));

        assertThat(aksjonspunktSet.stream()
            .filter(ap -> ap.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL)))
            .anySatisfy(ap -> assertThat(ap.getStatus()).isEqualTo(AksjonspunktStatus.OPPRETTET));

        assertThat(aksjonspunktSet).hasSize(3);
    }

}
