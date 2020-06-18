package no.nav.k9.sak.ytelse.frisinn.brev;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import javax.inject.Inject;

import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.k9.kodeverk.vedtak.Vedtaksbrev;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarsel;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.dokument.bestill.DokumentBestillerApplikasjonTjeneste;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(CdiRunner.class)
public class SendVedtaksbrevFrisinnTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private VedtakVarselRepository vedtakVarselRepository;

    @Mock
    private DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste;

    private SendVedtaksbrevFrisinn sendVedtaksbrev;
    private Behandling behandling;

    private TestScenarioBuilder scenario;

    public SendVedtaksbrevFrisinnTest() {
        scenario = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingsresultat(BehandlingResultatType.INNVILGET);
    }

    @Before
    public void oppsett() {
        sendVedtaksbrev = new SendVedtaksbrevFrisinn(behandlingRepository,
            repositoryProvider.getBehandlingVedtakRepository(),
            vedtakVarselRepository,
            dokumentBestillerApplikasjonTjeneste);
    }

    @Test
    public void testSendVedtaksbrevVedtakInnvilget() {
        scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET);
        behandling = scenario.lagre(repositoryProvider);

        // Act
        sendVedtaksbrev.sendVedtaksbrev(behandling.getId().toString());

        // Assert
        verify(dokumentBestillerApplikasjonTjeneste).produserVedtaksbrev(any(), any());
    }

    @Test
    public void testSendVedtaksbrevVedtakAvslag() {
        // Arrange
        scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.AVSLAG);
        behandling = scenario.lagre(repositoryProvider);

        // Act
        sendVedtaksbrev.sendVedtaksbrev(behandling.getId().toString());

        // Assert
        verify(dokumentBestillerApplikasjonTjeneste).produserVedtaksbrev(any(), any());
    }

    @Test
    public void senderBrevOmUendretUtfallVedRevurdering() {
        scenario.medBehandlingVedtak().medBeslutning(true).medVedtakResultatType(VedtakResultatType.INNVILGET);
        behandling = scenario.lagre(repositoryProvider);

        sendVedtaksbrev.sendVedtaksbrev(behandling.getId().toString());

        verify(dokumentBestillerApplikasjonTjeneste).produserVedtaksbrev(any(), any());
    }

    @Test
    public void senderIkkeBrevOmUendretUtfallHvisIkkeSendtVarselbrevOmRevurdering() {
        behandling = scenario.lagre(repositoryProvider);

        sendVedtaksbrev.sendVedtaksbrev(behandling.getId().toString());

        verify(dokumentBestillerApplikasjonTjeneste, never()).produserVedtaksbrev(any(), any());
    }

    @Test
    public void sender_ikke_brev_dersom_førstegangsøknad_som_er_migrert_fra_infotrygd() {
        behandling = scenario.lagre(repositoryProvider);
        behandling.setMigrertKilde(Fagsystem.INFOTRYGD);

        sendVedtaksbrev.sendVedtaksbrev(behandling.getId().toString());

        verify(dokumentBestillerApplikasjonTjeneste, never()).produserVedtaksbrev(any(), any());
    }

    @Test
    public void senderIkkeBrevForRefusjonTilArbeidsgiver() {
        TestScenarioBuilder scenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.OMSORGSPENGER)
            .medBehandlingsresultat(BehandlingResultatType.INNVILGET);
        scenario.medBehandlingVedtak().medBeslutning(true).medVedtakResultatType(VedtakResultatType.INNVILGET);
        behandling = scenario.lagre(repositoryProvider);

        sendVedtaksbrev.sendVedtaksbrev(behandling.getId().toString());

        verify(dokumentBestillerApplikasjonTjeneste, never()).produserVedtaksbrev(any(), any());
    }

    @Test
    public void senderIkkeBrevForFrisinnVedOverstyrt() {
        TestScenarioBuilder scenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.FRISINN)
            .medBehandlingsresultat(BehandlingResultatType.INNVILGET);
        scenario.medBehandlingVedtak().medBeslutning(true).medVedtakResultatType(VedtakResultatType.INNVILGET);
        behandling = scenario.lagre(repositoryProvider);

        VedtakVarsel varsel = new VedtakVarsel();
        varsel.setVedtaksbrev(Vedtaksbrev.INGEN);
        vedtakVarselRepository.lagre(behandling.getId(), varsel);

        sendVedtaksbrev.sendVedtaksbrev(behandling.getId().toString());

        verify(dokumentBestillerApplikasjonTjeneste, never()).produserVedtaksbrev(any(), any());
    }

    @Test
    public void senderFritekstbrevVedOverstyrt() {
        TestScenarioBuilder scenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.PLEIEPENGER_SYKT_BARN)
            .medBehandlingsresultat(BehandlingResultatType.INNVILGET);
        scenario.medBehandlingVedtak().medBeslutning(true).medVedtakResultatType(VedtakResultatType.INNVILGET);
        behandling = scenario.lagre(repositoryProvider);

        VedtakVarsel varsel = new VedtakVarsel();
        varsel.setVedtaksbrev(Vedtaksbrev.FRITEKST);
        vedtakVarselRepository.lagre(behandling.getId(), varsel);

        sendVedtaksbrev.sendVedtaksbrev(behandling.getId().toString());

        verify(dokumentBestillerApplikasjonTjeneste).produserVedtaksbrev(any(), any());
    }

    @Test
    public void senderIkkeBrevEtterKlagebehandling() {
        behandling = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.FRISINN).lagre(repositoryProvider);

        // Skal ikke sende brev etter revurdering som opprettes etter klagebehandling
        var etterKlagebehandling = BehandlingÅrsakType.ETTER_KLAGE;

        var revurderingsscenario = TestScenarioBuilder.builderMedSøknad()
            .medOriginalBehandling(behandling, etterKlagebehandling)
            .medBehandlingType(BehandlingType.REVURDERING);
        revurderingsscenario.medBehandlingVedtak().medBeslutning(true).medVedtakResultatType(VedtakResultatType.INNVILGET);
        var revurdering = revurderingsscenario.lagre(repositoryProvider);

        sendVedtaksbrev.sendVedtaksbrev(revurdering.getId().toString());

        verify(dokumentBestillerApplikasjonTjeneste, never()).produserVedtaksbrev(any(), any());
    }
}
