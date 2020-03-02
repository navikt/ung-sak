package no.nav.foreldrepenger.domene.vedtak.intern;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerApplikasjonTjeneste;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.dokument.DokumentMalType;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class SendVedtaksbrevTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Mock
    private DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste;

    @Mock
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;

    private SendVedtaksbrev sendVedtaksbrev;
    private Behandling behandling;

    private TestScenarioBuilder scenario;

    public SendVedtaksbrevTest() {
        scenario = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
    }

    @Before
    public void oppsett() {
        sendVedtaksbrev = new SendVedtaksbrev(behandlingRepository,
            repositoryProvider.getBehandlingVedtakRepository(),
            dokumentBestillerApplikasjonTjeneste,
            dokumentBehandlingTjeneste);

    }

    @Test
    public void testSendVedtaksbrevVedtakInnvilget() {
        scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET);
        behandling = scenario.lagre(repositoryProvider);
        
        // Act
        sendVedtaksbrev.sendVedtaksbrev(BehandlingReferanse.fra(behandling));

        // Assert
        verify(dokumentBestillerApplikasjonTjeneste).produserVedtaksbrev(any(BehandlingVedtak.class));
    }

    @Test
    public void testSendVedtaksbrevVedtakAvslag() {
        // Arrange
        scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.AVSLAG);
        behandling = scenario.lagre(repositoryProvider);
        
        // Act
        sendVedtaksbrev.sendVedtaksbrev(BehandlingReferanse.fra(behandling));

        // Assert
        verify(dokumentBestillerApplikasjonTjeneste).produserVedtaksbrev(any(BehandlingVedtak.class));
    }

    @Test
    public void senderBrevOmUendretUtfallVedRevurdering() {
        scenario.medBehandlingVedtak().medBeslutning(true).medVedtakResultatType(VedtakResultatType.INNVILGET);
        behandling = scenario.lagre(repositoryProvider);
        
        when(dokumentBehandlingTjeneste.erDokumentProdusert(behandling.getId(), DokumentMalType.REVURDERING_DOK))
            .thenReturn(true);

        sendVedtaksbrev.sendVedtaksbrev(BehandlingReferanse.fra(behandling));

        verify(dokumentBestillerApplikasjonTjeneste).produserVedtaksbrev(any(BehandlingVedtak.class));
    }

    @Test
    public void senderIkkeBrevOmUendretUtfallHvisIkkeSendtVarselbrevOmRevurdering() {
        behandling = scenario.lagre(repositoryProvider);
        
        when(dokumentBehandlingTjeneste.erDokumentProdusert(behandling.getId(), DokumentMalType.REVURDERING_DOK))
            .thenReturn(false);

        sendVedtaksbrev.sendVedtaksbrev(BehandlingReferanse.fra(behandling));

        verify(dokumentBestillerApplikasjonTjeneste, never()).produserVedtaksbrev(any(BehandlingVedtak.class));
    }

    @Test
    public void sender_ikke_brev_dersom_førstegangsøknad_som_er_migrert_fra_infotrygd() {
        behandling = scenario.lagre(repositoryProvider);
        behandling.setMigrertKilde(Fagsystem.INFOTRYGD);
        
        sendVedtaksbrev.sendVedtaksbrev(BehandlingReferanse.fra(behandling));

        verify(dokumentBestillerApplikasjonTjeneste, never()).produserVedtaksbrev(any(BehandlingVedtak.class));
    }

}
