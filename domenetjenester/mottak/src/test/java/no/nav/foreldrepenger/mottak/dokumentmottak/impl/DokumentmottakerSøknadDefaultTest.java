package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static java.time.LocalDate.now;
import static no.nav.vedtak.felles.testutilities.Whitebox.setInternalState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.StartBehandlingTask;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class DokumentmottakerSøknadDefaultTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private AksjonspunktRepository aksjonspunktRepository;

    @Mock
    private ProsessTaskRepository prosessTaskRepository;
    @Mock
    private Behandlingsoppretter behandlingsoppretter;
    @Mock
    private Kompletthetskontroller kompletthetskontroller;
    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    @Mock
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;

    private DokumentmottakerSøknad dokumentmottaker;
    private DokumentmottakerFelles dokumentmottakerFelles;

    @Before
    public void oppsett() {
        MockitoAnnotations.initMocks(this);

        BehandlendeEnhetTjeneste enhetsTjeneste = mock(BehandlendeEnhetTjeneste.class);
        OrganisasjonsEnhet enhet = new OrganisasjonsEnhet("0312", "enhetNavn");
        when(enhetsTjeneste.finnBehandlendeEnhetFraSøker(any(Fagsak.class))).thenReturn(enhet);
        when(enhetsTjeneste.finnBehandlendeEnhetFraSøker(any(Behandling.class))).thenReturn(enhet);

        dokumentmottakerFelles = new DokumentmottakerFelles(repositoryProvider, prosessTaskRepository, enhetsTjeneste,
            historikkinnslagTjeneste, mottatteDokumentTjeneste, behandlingsoppretter);
        dokumentmottakerFelles = Mockito.spy(dokumentmottakerFelles);

        dokumentmottaker = new DokumentmottakerSøknadDefault(repositoryProvider, dokumentmottakerFelles, mottatteDokumentTjeneste,
            behandlingsoppretter, kompletthetskontroller);
        dokumentmottaker = Mockito.spy(dokumentmottaker);
    }

    @Test
    public void skal_starte_behandling_av_søknad() {
        //Arrange
        Fagsak fagsak = nyMorFødselFagsak();
        Long fagsakId = fagsak.getId();
        DokumentTypeId dokumentTypeId = DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL;

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);

        Behandling behandlingMock = mock(Behandling.class);
        when(behandlingMock.getAktørId()).thenReturn(fagsak.getAktørId());
        when(behandlingsoppretter.opprettFørstegangsbehandling(eq(fagsak), any(), any())).thenReturn(behandlingMock);

        //Act
        dokumentmottaker.mottaDokument(mottattDokument, fagsak, dokumentTypeId, null);

        //Assert
        verify(dokumentmottaker).håndterIngenTidligereBehandling(fagsak, mottattDokument, null);
        verify(behandlingsoppretter).opprettFørstegangsbehandling(eq(fagsak), any(), any());
        verify(dokumentmottakerFelles).opprettHistorikk(any(Behandling.class), eq(mottattDokument.getJournalpostId()));
    }

    @Test
    public void skal_tilbake_til_steg_registrer_søknad_dersom_åpen_behandling() {
        //Arrange
        Behandling behandling = TestScenarioBuilder
            .builderUtenSøknad()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .lagre(repositoryProvider);

        Long fagsakId = behandling.getFagsakId();
        DokumentTypeId dokumentTypeId = DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL;

        String xml = null;
        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, xml, now(), false, null);

        //Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), dokumentTypeId, null);

        //Assert
        verify(dokumentmottaker).oppdaterÅpenBehandlingMedDokument(behandling, mottattDokument, null);
    }

    @Test
    public void skal_lage_revurdering_når_det_finnes_en_avsluttet_behandling_på_saken_fra_før() {
        //Arrange
        Behandling behandling = TestScenarioBuilder
            .builderUtenSøknad()
            .lagre(repositoryProvider);

        BehandlingVedtak vedtak = DokumentmottakTestUtil.oppdaterVedtaksresultat(behandling, VedtakResultatType.AVSLAG);
        repoRule.getRepository().lagre(vedtak.getBehandlingsresultat());

        //simulere at den tidliggere behandligen er avsluttet
        behandling.avsluttBehandling();
        Long fagsakId = behandling.getFagsakId();
        DokumentTypeId dokumentTypeId = DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL;

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        Behandling revurdering = mock(Behandling.class);
        when(revurdering.getId()).thenReturn(10L);
        when(revurdering.getFagsakId()).thenReturn(behandling.getFagsakId());
        when(revurdering.getFagsak()).thenReturn(behandling.getFagsak());
        when(revurdering.getAktørId()).thenReturn(behandling.getAktørId());
        doReturn(revurdering).when(behandlingsoppretter).opprettRevurdering(behandling.getFagsak(), BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);

        //Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), dokumentTypeId, null);

        //Assert
        verify(dokumentmottaker).håndterAvsluttetTidligereBehandling(mottattDokument, behandling.getFagsak(), null);

        //Verifiser at korrekt prosesstask for vurder dokument blir opprettet
        verify(behandlingsoppretter).opprettRevurdering(behandling.getFagsak(), BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        verify(dokumentmottakerFelles).opprettHistorikk(revurdering, mottattDokument.getJournalpostId());
    }

    @Test
    public void skal_lage_manuell_revurdering_fra_opphørt_førstegangsbehandling_på_saken_fra_før() {
        //Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.OPPHØR));
        Behandling behandling = scenario.lagre(repositoryProvider);

        BehandlingVedtak vedtak = DokumentmottakTestUtil.oppdaterVedtaksresultat(behandling, VedtakResultatType.OPPHØR);
        repoRule.getRepository().lagre(vedtak.getBehandlingsresultat());

        //simulere at den tidliggere behandligen er avsluttet
        behandling.avsluttBehandling();
        Long fagsakId = behandling.getFagsakId();
        DokumentTypeId dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        doReturn(false).when(behandlingsoppretter).erBehandlingOgFørstegangsbehandlingHenlagt(behandling.getFagsak());
        doReturn(behandling).when(behandlingsoppretter).opprettManuellRevurdering(behandling.getFagsak(), BehandlingÅrsakType.ETTER_KLAGE);

        //Act
        dokumentmottaker.opprettFraTidligereAvsluttetBehandling(behandling.getFagsak(), behandling.getId(), mottattDokument, BehandlingÅrsakType.ETTER_KLAGE);

        //Assert
        verify(dokumentmottaker).opprettFraTidligereAvsluttetBehandling(behandling.getFagsak(), behandling.getId(), mottattDokument, BehandlingÅrsakType.ETTER_KLAGE);
        verify(behandlingsoppretter).opprettManuellRevurdering(behandling.getFagsak(), BehandlingÅrsakType.ETTER_KLAGE);
    }

    @Test
    public void skal_finne_at_søknad_fra_tidligere_behandling_er_mottatt_og_henlegge_åpen_behandling() {
        // Arrange
        Behandling behandling1 = TestScenarioBuilder
            .builderMedSøknad()
            .lagre(repositoryProvider);
        behandling1.avsluttBehandling();
        Fagsak fagsak = behandling1.getFagsak();

        Behandling behandling2 = Behandling.fraTidligereBehandling(behandling1, BehandlingType.FØRSTEGANGSSØKNAD).build();
        lagreBehandling(behandling2);

        DokumentTypeId dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;

        when(mottatteDokumentTjeneste.harMottattDokumentSet(eq(behandling2.getId()), anySet())).thenReturn(true);

        Behandling behandling3 = Behandling.fraTidligereBehandling(behandling2, BehandlingType.FØRSTEGANGSSØKNAD).build();
        // Hack, men det blir feil å lagre Behandlingen før Act da det påvirker scenarioet, og mock(Behandling) er heller ikke pent...
        setInternalState(behandling3, "id", 9999L);
        when(behandlingsoppretter.oppdaterBehandlingViaHenleggelse(behandling2, null)).thenReturn(behandling3);

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsak.getId(), "<søknad>", now(), true, null);
        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling2.getFagsak(), dokumentTypeId, null);

        // Assert
        verify(behandlingsoppretter).oppdaterBehandlingViaHenleggelse(behandling2, null);
        verify(dokumentmottakerFelles).opprettHistorikk(behandling2, mottattDokument.getJournalpostId());

        verify(prosessTaskRepository).lagre(captor.capture());
        ProsessTaskData prosessTaskData = captor.getValue();
        assertThat(prosessTaskData.getTaskType()).isEqualTo(StartBehandlingTask.TASKTYPE);
    }


    @Test
    public void skal_finne_at_søknad_ikke_er_mottatt_tidligere_og_knytte_søknaden_til_behandlingen() {
        // Arrange
        Behandling behandling1 = TestScenarioBuilder
            .builderMedSøknad()
            .lagre(repositoryProvider);
        behandling1.avsluttBehandling();
        Fagsak fagsak = behandling1.getFagsak();

        Behandling behandling2 = Behandling.fraTidligereBehandling(behandling1, BehandlingType.FØRSTEGANGSSØKNAD).build();
        lagreBehandling(behandling2);

        DokumentTypeId dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;

        when(mottatteDokumentTjeneste.harMottattDokumentSet(eq(behandling1.getId()), anySet())).thenReturn(true);
        when(mottatteDokumentTjeneste.harMottattDokumentSet(eq(behandling2.getId()), anySet())).thenReturn(false);

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsak.getId(), "<søknad>", now(), true, null);

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling2.getFagsak(), dokumentTypeId, null);

        // Assert
        verify(dokumentmottakerFelles).opprettHistorikk(behandling2, mottattDokument.getJournalpostId());
        verify(kompletthetskontroller).persisterDokumentOgVurderKompletthet(behandling2, mottattDokument);
    }

    private void lagreBehandling(Behandling behandling) {
        BehandlingLås lås = repositoryProvider.getBehandlingRepository().taSkriveLås(behandling);
        repositoryProvider.getBehandlingRepository().lagre(behandling, lås);
    }

    private Fagsak nyMorFødselFagsak() {
        return TestScenarioBuilder.builderUtenSøknad().lagreFagsak(repositoryProvider);
    }

}
