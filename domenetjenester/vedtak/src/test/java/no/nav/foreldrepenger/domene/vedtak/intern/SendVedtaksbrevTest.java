package no.nav.foreldrepenger.domene.vedtak.intern;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerApplikasjonTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;

public class SendVedtaksbrevTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste;
    @Mock
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    @Mock
    private Behandlingsresultat behandlingsresultat;
    @Mock
    private Behandling behandlingMock;
    @Mock
    private Fagsak fagsakMock;

    private SendVedtaksbrev sendVedtaksbrev;

    private Behandling behandling;
    private BehandlingVedtak behandlingVedtak;

    private BehandlingRepository behandlingRepository;

    private BehandlingRepositoryProvider repositoryProvider;

    @Before
    public void oppsett() {
        var scenario = TestScenarioBuilder.builderMedSøknad();
        behandling = scenario.lagMocked();
        behandlingRepository = scenario.mockBehandlingRepository();
        repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        behandlingVedtak = scenario.mockBehandlingVedtak();
        sendVedtaksbrev = new SendVedtaksbrev(behandlingRepository, repositoryProvider.getBehandlingVedtakRepository(), dokumentBestillerApplikasjonTjeneste, dokumentBehandlingTjeneste);
        when(behandlingsresultat.getVedtaksbrev()).thenReturn(Vedtaksbrev.AUTOMATISK);
        when(behandlingVedtak.getBehandlingsresultat()).thenReturn(behandlingsresultat);
        when(behandlingVedtak.getVedtakResultatType()).thenReturn(VedtakResultatType.INNVILGET);

    }

    @Test
    public void testSendVedtaksbrevVedtakInnvilget() {
        // Act
        sendVedtaksbrev.sendVedtaksbrev(behandling.getId());

        // Assert
        verify(dokumentBestillerApplikasjonTjeneste).produserVedtaksbrev(behandlingVedtak);
    }

    @Test
    public void testSendVedtaksbrevVedtakAvslag() {
        // Arrange
        when(behandlingVedtak.getVedtakResultatType()).thenReturn(VedtakResultatType.AVSLAG);

        // Act
        sendVedtaksbrev.sendVedtaksbrev(behandling.getId());

        // Assert
        verify(dokumentBestillerApplikasjonTjeneste).produserVedtaksbrev(behandlingVedtak);
    }

    @Test
    public void senderBrevOmUendretUtfallVedRevurdering() {
        when(behandlingVedtak.isBeslutningsvedtak()).thenReturn(true);
        when(dokumentBehandlingTjeneste.erDokumentProdusert(behandling.getId(), DokumentMalType.REVURDERING_DOK))
            .thenReturn(true);

        sendVedtaksbrev.sendVedtaksbrev(behandling.getId());

        verify(dokumentBestillerApplikasjonTjeneste).produserVedtaksbrev(behandlingVedtak);
    }

    @Test
    public void senderIkkeBrevOmUendretUtfallHvisIkkeSendtVarselbrevOmRevurdering() {
        when(behandlingVedtak.isBeslutningsvedtak()).thenReturn(true);
        when(dokumentBehandlingTjeneste.erDokumentProdusert(behandling.getId(), DokumentMalType.REVURDERING_DOK))
            .thenReturn(false);

        sendVedtaksbrev.sendVedtaksbrev(behandling.getId());

        verify(dokumentBestillerApplikasjonTjeneste, never()).produserVedtaksbrev(behandlingVedtak);
    }

    @Test
    public void sender_ikke_brev_dersom_førstegangsøknad_som_er_migrert_fra_infotrygd() {
        behandling.setMigrertKilde(Fagsystem.INFOTRYGD);

        sendVedtaksbrev.sendVedtaksbrev(behandling.getId());

        verify(dokumentBestillerApplikasjonTjeneste, never()).produserVedtaksbrev(behandlingVedtak);
    }

    @Test
    public void sender_brev_dersom_førstegangsøknad_som_er_migrert_fra_infotrygd_men_overstyrt_til_fritekstbrev() {
        behandling.setMigrertKilde(Fagsystem.INFOTRYGD);
        when(behandlingsresultat.getVedtaksbrev()).thenReturn(Vedtaksbrev.FRITEKST);

        sendVedtaksbrev.sendVedtaksbrev(behandling.getId());

        verify(dokumentBestillerApplikasjonTjeneste, times(1)).produserVedtaksbrev(behandlingVedtak);
    }


}
