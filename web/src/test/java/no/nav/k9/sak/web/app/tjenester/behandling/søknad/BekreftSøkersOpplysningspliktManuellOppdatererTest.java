package no.nav.k9.sak.web.app.tjenester.behandling.søknad;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltVerdiType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.søknad.BekreftSokersOpplysningspliktManuDto;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.felles.util.Tuple;

public class BekreftSøkersOpplysningspliktManuellOppdatererTest {

    private final HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();

    @Test
    public void skal_generere_historikkinnslag_ved_avklaring_av_søkers_opplysningsplikt_manu() {
        // Arrange
        // Behandling
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.SØKERS_OPPLYSNINGSPLIKT_MANU, BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT);
        scenario.lagMocked();

        Behandling behandling = scenario.getBehandling();

        BekreftSøkersOpplysningspliktManuellOppdaterer oppdaterer = new BekreftSøkersOpplysningspliktManuellOppdaterer(lagMockHistory());

        // Dto
        BekreftSokersOpplysningspliktManuDto bekreftSokersOpplysningspliktManuDto = new BekreftSokersOpplysningspliktManuDto(
           "test av manu", true, Collections.emptyList());
        assertThat(behandling.getAksjonspunkter()).hasSize(1);

        // Act
        var aksjonspunkt = behandling.getAksjonspunktFor(bekreftSokersOpplysningspliktManuDto.getKode());
        var resultat = oppdaterer.oppdater(bekreftSokersOpplysningspliktManuDto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, bekreftSokersOpplysningspliktManuDto));
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        List<HistorikkinnslagDel> historikkInnslag = tekstBuilder.build(historikkinnslag);

        // Assert
        assertThat(historikkInnslag).hasSize(1);
        HistorikkinnslagDel del = historikkInnslag.get(0);
        List<HistorikkinnslagFelt> feltList = del.getEndredeFelt();
        assertThat(feltList).hasSize(1);
        HistorikkinnslagFelt felt = feltList.get(0);
        assertThat(felt.getNavn()).as("navn").isEqualTo(HistorikkEndretFeltType.SOKERSOPPLYSNINGSPLIKT.getKode());
        assertThat(felt.getFraVerdi()).as("fraVerdi").isNull();
        assertThat(felt.getTilVerdi()).as("tilVerdi").isEqualTo(HistorikkEndretFeltVerdiType.VILKAR_OPPFYLT.getKode());

        Set<AksjonspunktDefinisjon> aksjonspunktSet = resultat.getEkstraAksjonspunktResultat().stream().map(Tuple::getElement1).collect(Collectors.toSet());

        assertThat(aksjonspunktSet).isEmpty();
    }

    private HistorikkTjenesteAdapter lagMockHistory() {
        HistorikkTjenesteAdapter mockHistory = Mockito.mock(HistorikkTjenesteAdapter.class);
        Mockito.when(mockHistory.tekstBuilder()).thenReturn(tekstBuilder);
        return mockHistory;
    }
}
