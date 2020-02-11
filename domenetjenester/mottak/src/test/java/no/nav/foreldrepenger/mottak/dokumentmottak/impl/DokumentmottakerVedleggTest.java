package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static java.time.LocalDate.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.kompletthet.KompletthetResultat;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderDokumentTask;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class DokumentmottakerVedleggTest {

    private static final KompletthetResultat OPPFYLT = KompletthetResultat.oppfylt();

    private static final KompletthetResultat IKKE_OPPFYLT = KompletthetResultat.ikkeOppfylt(LocalDateTime.now(), Venteårsak.AVV_DOK);

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private Behandlingsoppretter behandlingsoppretter;
    
    @Mock
    private ProsessTaskRepository prosessTaskRepository;
    @Mock
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    @Mock
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;
    @Mock
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    private DokumentmottakerVedlegg dokumentmottaker;
    private DokumentmottakerFelles dokumentmottakerFelles;
    private Kompletthetskontroller kompletthetskontroller;

    @Before
    public void oppsett() {
        MockitoAnnotations.initMocks(this);

        OrganisasjonsEnhet enhet = new OrganisasjonsEnhet("0312", "enhetNavn");
        when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFraSøker(any(Fagsak.class))).thenReturn(enhet);
        when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFraSøker(any(Behandling.class))).thenReturn(enhet);

        dokumentmottakerFelles = new DokumentmottakerFelles(repositoryProvider, prosessTaskRepository, behandlendeEnhetTjeneste,
            historikkinnslagTjeneste, mottatteDokumentTjeneste, behandlingsoppretter);
        dokumentmottakerFelles = Mockito.spy(dokumentmottakerFelles);

        kompletthetskontroller = mock(Kompletthetskontroller.class);
        dokumentmottaker = new DokumentmottakerVedlegg(repositoryProvider, dokumentmottakerFelles, behandlingsoppretter, kompletthetskontroller);
        dokumentmottaker = Mockito.spy(dokumentmottaker);
        when(behandlendeEnhetTjeneste.getKlageInstans()).thenReturn(new OrganisasjonsEnhet("4292", "NAV Klageinstans Midt-Norge"));
    }

    @Test
    public void skal_vurdere_kompletthet_når_ustrukturert_dokument_på_åpen_behandling() {
        when(kompletthetskontroller.vurderBehandlingKomplett(any())).thenReturn(IKKE_OPPFYLT);
        
        //Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingStegStart(BehandlingStegType.INNHENT_SØKNADOPP);
        Behandling behandling = scenario.lagre(repositoryProvider);

        DokumentTypeId dokumentTypeId = DokumentTypeId.INNTEKTSMELDING;

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, behandling.getFagsakId(), "", now(), true, null);

        //Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), dokumentTypeId, null);

        //Assert
        verify(kompletthetskontroller).persisterDokumentOgVurderKompletthet(behandling, mottattDokument);
        verify(dokumentmottakerFelles).opprettHistorikkinnslagForVedlegg(behandling.getFagsakId(), mottattDokument.getJournalpostId(), dokumentTypeId);
    }

    @Test
    public void skal_opprette_task_for_å_vurdere_dokument_når_det_ikke_er_en_søknad_men_har_behandling_på_saken_og_komplett() {
        //Arrange
        DokumentTypeId dokumentTypeId = DokumentTypeId.INNTEKTSMELDING;
        when(kompletthetskontroller.vurderBehandlingKomplett(any())).thenReturn(OPPFYLT);
        
        var scenario = TestScenarioBuilder.builderMedSøknad()
            .medBehandlendeEnhet("0450")
            .medBehandlingStegStart(BehandlingStegType.FORESLÅ_VEDTAK);
        Behandling behandling = scenario.lagre(repositoryProvider);

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, behandling.getFagsakId(), "", now(), true, null);

        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);

        //Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), dokumentTypeId, null);

        //Assert
        verify(dokumentmottakerFelles).opprettTaskForÅVurdereDokument(behandling.getFagsak(), behandling, mottattDokument);

        //Verifiser at korrekt prosesstask for vurder dokument blir opprettet
        verify(kompletthetskontroller, times(0)).persisterDokumentOgVurderKompletthet(behandling, mottattDokument);
        verify(prosessTaskRepository).lagre(captor.capture());
        ProsessTaskData prosessTaskData = captor.getValue();
        assertThat(prosessTaskData.getTaskType()).isEqualTo(OpprettOppgaveVurderDokumentTask.TASKTYPE);
        assertThat(prosessTaskData.getPropertyValue(OpprettOppgaveVurderDokumentTask.KEY_BEHANDLENDE_ENHET)).isEqualTo("0450"); //Lik enheten som ble satt på behandlingen
    }

}
