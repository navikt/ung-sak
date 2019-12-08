package no.nav.foreldrepenger.dokumentbestiller.autopunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.MockitoAnnotations.initMocks;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.Spy;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerApplikasjonTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class SendBrevForAutopunktTest {

    @Spy
    DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste;
    @Spy
    DokumentBehandlingTjeneste dokumentBehandlingTjeneste;

    Aksjonspunkt aksjonspunkt;

    private SendBrevForAutopunkt sendBrevForAutopunkt;

    private Behandling behandling;
    private TestScenarioBuilder scenario;
    private LocalDateTime førsteJanuar2019 = LocalDateTime.of(LocalDate.of(2019, 1, 1), LocalTime.of(12, 0));
    private BehandlingRepositoryProvider repositoryProvider;

    @Before
    public void setUp() {
        initMocks(this);
        scenario = TestScenarioBuilder.builderMedSøknad();
        repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VENT_PÅ_FØDSEL, BehandlingStegType.KONTROLLER_FAKTA);
        behandling = scenario.lagMocked();
        aksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VENT_PÅ_FØDSEL).get();

        repositoryProvider.getAksjonspunktRepository().setFrist(aksjonspunkt, førsteJanuar2019, Venteårsak.AVV_FODSEL);

        sendBrevForAutopunkt = new SendBrevForAutopunkt(dokumentBestillerApplikasjonTjeneste,
            dokumentBehandlingTjeneste,
            repositoryProvider);

        doReturn(false).when(dokumentBehandlingTjeneste).erDokumentProdusert(Mockito.eq(behandling.getId()), Mockito.any());
        doNothing().when(dokumentBestillerApplikasjonTjeneste).bestillDokument(Mockito.any(), Mockito.eq(HistorikkAktør.VEDTAKSLØSNINGEN), Mockito.anyBoolean());
    }

    @Test
    public void sendBrevForSøknadIkkeMottattFørsteGang() {
        sendBrevForAutopunkt.sendBrevForSøknadIkkeMottatt(behandling);
        Mockito.verify(dokumentBestillerApplikasjonTjeneste, times(1)).bestillDokument(Mockito.any(), Mockito.eq(HistorikkAktør.VEDTAKSLØSNINGEN), Mockito.anyBoolean());
    }

    @Test
    public void sendBrevForTidligSøknadFørsteGang() {
        sendBrevForAutopunkt.sendBrevForTidligSøknad(behandling, aksjonspunkt);
        Mockito.verify(dokumentBestillerApplikasjonTjeneste, times(1)).bestillDokument(Mockito.any(), Mockito.eq(HistorikkAktør.VEDTAKSLØSNINGEN), Mockito.anyBoolean());
        assertThat(behandling.getBehandlingstidFrist()).isEqualTo(LocalDate.from(førsteJanuar2019.plusWeeks(behandling.getType().getBehandlingstidFristUker())));
    }

    @Test
    public void sendBrevForTidligSøknadBareEnGang() {
        doReturn(true).when(dokumentBehandlingTjeneste).erDokumentProdusert(behandling.getId(), DokumentMalType.FORLENGET_TIDLIG_SOK);
        sendBrevForAutopunkt.sendBrevForTidligSøknad(behandling, aksjonspunkt);
        Mockito.verify(dokumentBestillerApplikasjonTjeneste, times(0)).bestillDokument(Mockito.any(), Mockito.eq(HistorikkAktør.VEDTAKSLØSNINGEN), Mockito.anyBoolean());
    }
}
