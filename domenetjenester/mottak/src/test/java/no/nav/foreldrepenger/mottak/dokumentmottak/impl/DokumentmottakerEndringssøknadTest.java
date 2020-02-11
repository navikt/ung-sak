package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static java.time.LocalDate.now;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class DokumentmottakerEndringssøknadTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Mock
    private Behandlingsoppretter behandlingsoppretter;
    @Mock
    private ProsessTaskRepository prosessTaskRepository;
    @Mock
    private BehandlendeEnhetTjeneste enhetsTjeneste;

    @Mock
    private Kompletthetskontroller kompletthetskontroller;
    @Mock
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    @Mock
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;

    private DokumentmottakerEndringssøknad dokumentmottaker;
    private DokumentmottakerFelles dokumentmottakerFelles;

    @Before
    public void oppsett() {
        MockitoAnnotations.initMocks(this);

        OrganisasjonsEnhet enhet = new OrganisasjonsEnhet("0312", "enhetNavn");
        when(enhetsTjeneste.finnBehandlendeEnhetFraSøker(any(Fagsak.class))).thenReturn(enhet);
        when(enhetsTjeneste.finnBehandlendeEnhetFraSøker(any(Behandling.class))).thenReturn(enhet);

        dokumentmottakerFelles = new DokumentmottakerFelles(repositoryProvider, prosessTaskRepository,
            enhetsTjeneste, historikkinnslagTjeneste, mottatteDokumentTjeneste, behandlingsoppretter);
        dokumentmottakerFelles = Mockito.spy(dokumentmottakerFelles);

        dokumentmottaker = new DokumentmottakerEndringssøknad(repositoryProvider, dokumentmottakerFelles,
            mottatteDokumentTjeneste, behandlingsoppretter, kompletthetskontroller);
        dokumentmottaker = Mockito.spy(dokumentmottaker);
    }

    @Test
    public void skal_opprette_task_for_manuell_vurdering_av_endringssøknad_dersom_ingen_behandling_finnes_fra_før() {
        //Arrange
        Fagsak fagsak = nyFagsak();
        Long fagsakId = fagsak.getId();
        DokumentTypeId dokumentTypeEndringssøknad = DokumentTypeId.FORELDREPENGER_ENDRING_SØKNAD;

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeEndringssøknad, fagsakId, "", now(), true, null);

        //Act
        dokumentmottaker.mottaDokument(mottattDokument, fagsak, dokumentTypeEndringssøknad, BehandlingÅrsakType.UDEFINERT);

        //Assert
        verify(dokumentmottakerFelles).opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument);
    }

    @Test
    public void skal_opprette_revurdering_for_endringssøknad_dersom_siste_behandling_er_avsluttet() {
        //Arrange
        Behandling behandling = TestScenarioBuilder
            .builderUtenSøknad()
            .lagre(repositoryProvider);
        BehandlingVedtak vedtak = DokumentmottakTestUtil.oppdaterVedtaksresultat(behandling, VedtakResultatType.INNVILGET);
        repoRule.getRepository().lagre(vedtak.getBehandlingsresultat());

        Behandling revurdering = TestScenarioBuilder
            .builderUtenSøknad()
            .lagre(repositoryProvider);
        when(behandlingsoppretter.opprettRevurdering(behandling.getFagsak(), BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)).thenReturn(revurdering);

        // simulere at den tidligere behandlingen er avsluttet
        behandling.avsluttBehandling();
        Long fagsakId = behandling.getFagsakId();
        DokumentTypeId dokumentTypeEndringssøknad = DokumentTypeId.FORELDREPENGER_ENDRING_SØKNAD;

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeEndringssøknad, fagsakId, "", now(), true, null);

        //Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), dokumentTypeEndringssøknad, BehandlingÅrsakType.UDEFINERT);//Behandlingårsaktype blir aldri satt for mottatt dokument, så input er i udefinert.

        //Assert
        verify(behandlingsoppretter).opprettRevurdering(behandling.getFagsak(), BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER); //Skal ved opprettelse av revurdering fra endringssøknad sette behandlingårsaktype til 'endring fra bruker'.
        verify(dokumentmottakerFelles).opprettHistorikk(any(Behandling.class), eq(mottattDokument.getJournalpostId()));
    }

    @Test
    public void skal_oppdatere_behandling_med_endringssøknad_dersom_siste_behandling_er_åpen() {
        //Arrange
        Behandling behandling = TestScenarioBuilder
            .builderUtenSøknad()
            .lagre(repositoryProvider);
        BehandlingVedtak vedtak = DokumentmottakTestUtil.oppdaterVedtaksresultat(behandling, VedtakResultatType.INNVILGET);
        repoRule.getRepository().lagre(vedtak.getBehandlingsresultat());

        Behandling revurdering = TestScenarioBuilder
            .builderUtenSøknad()
            .medFagsakId(behandling.getFagsakId())
            .medOriginalBehandling(behandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagre(repositoryProvider);

        // simulere at den tidligere behandlingen er avsluttet
        behandling.avsluttBehandling();

        Long fagsakId = behandling.getFagsakId();
        DokumentTypeId dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);

        //Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), dokumentTypeId, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);

        //Assert
        verify(dokumentmottaker).oppdaterÅpenBehandlingMedDokument(revurdering, mottattDokument, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        verify(kompletthetskontroller).persisterDokumentOgVurderKompletthet(revurdering, mottattDokument);
        verify(dokumentmottakerFelles).opprettHistorikk(revurdering, mottattDokument.getJournalpostId());
    }

    @Test
    public void skal_oppdatere_mottatt_dokument_med_behandling_hvis_behandlig_er_på_vent() {
        //Arrange
        Behandling behandling = TestScenarioBuilder
            .builderUtenSøknad()
            .lagre(repositoryProvider);
        BehandlingVedtak vedtak = DokumentmottakTestUtil.oppdaterVedtaksresultat(behandling, VedtakResultatType.INNVILGET);
        repoRule.getRepository().lagre(vedtak.getBehandlingsresultat());

        Behandling revurdering = TestScenarioBuilder
            .builderUtenSøknad()
            .medFagsakId(behandling.getFagsakId())
            .medOriginalBehandling(behandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagre(repositoryProvider);

        // simulere at den tidligere behandlingen er avsluttet
        behandling.avsluttBehandling();

        Long fagsakId = behandling.getFagsakId();
        DokumentTypeId dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);

        //Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), dokumentTypeId, BehandlingÅrsakType.UDEFINERT);

        //Assert
        verify(mottatteDokumentTjeneste).oppdaterMottattDokumentMedBehandling(mottattDokument, revurdering.getId());
    }

    @Test
    public void skal_opprette_vurder_dokument_oppgave_i_gosys_dersom_det_er_åpen_førstegangsbehandling() {
        //Arrange
        Behandling behandling = TestScenarioBuilder
            .builderUtenSøknad()
            .lagre(repositoryProvider);

        Long fagsakId = behandling.getFagsakId();
        DokumentTypeId dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);

        //Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), dokumentTypeId, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);

        //Assert
        verify(dokumentmottaker).oppdaterÅpenBehandlingMedDokument(behandling, mottattDokument, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        verify(dokumentmottakerFelles).opprettTaskForÅVurdereDokument(behandling.getFagsak(), behandling, mottattDokument);
    }

    private Fagsak nyFagsak() {
        return TestScenarioBuilder.builderUtenSøknad().lagreFagsak(repositoryProvider);
    }
}
