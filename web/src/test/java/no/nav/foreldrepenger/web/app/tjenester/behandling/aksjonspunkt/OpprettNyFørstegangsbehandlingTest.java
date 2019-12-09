package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.SaksbehandlingDokumentmottakTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.HåndterMottattDokumentTaskProperties;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskEventPubliserer;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskRepositoryImpl;
import no.nav.vedtak.felles.testutilities.Whitebox;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@SuppressWarnings("deprecation")
@RunWith(CdiRunner.class)
public class OpprettNyFørstegangsbehandlingTest {

    private  static final long MOTTATT_DOKUMENT_ID = 5L;
    private  static final long MOTTATT_DOKUMENT_PAPIR_SØKNAD_ID = 3L;
    private  static final long MOTTATT_DOKUMENT_EL_SØKNAD_ID = 1L;

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private Behandling behandling;

    private BehandlingRepositoryProvider repositoryProvider;

    private ProsessTaskRepository prosessTaskRepository;
    private SaksbehandlingDokumentmottakTjeneste saksbehandlingDokumentmottakTjeneste;
    private BehandlingsoppretterApplikasjonTjeneste behandlingsoppretterApplikasjonTjeneste;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    MottattDokument.Builder md1 = new MottattDokument.Builder()
        .medJournalPostId(new JournalpostId("123"))
        .medDokumentType(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_ADOPSJON)
        .medMottattDato(LocalDate.now())
        .medElektroniskRegistrert(true)
        .medXmlPayload("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>") // Skal bare være en string slik at XmlPayLoad ikke er null
        .medId(MOTTATT_DOKUMENT_EL_SØKNAD_ID);
    MottattDokument.Builder md2 = new MottattDokument.Builder() //Annet dokument som ikke er søknad
        .medJournalPostId(new JournalpostId("456"))
        .medDokumentType(DokumentTypeId.UDEFINERT)
        .medMottattDato(LocalDate.now())
        .medElektroniskRegistrert(false)
        .medId(2L);
    MottattDokument.Builder md3 = new MottattDokument.Builder()
        .medJournalPostId(new JournalpostId("789"))
        .medDokumentType(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL)
        .medMottattDato(LocalDate.now())
        .medElektroniskRegistrert(false)
        .medId(MOTTATT_DOKUMENT_PAPIR_SØKNAD_ID);
    MottattDokument.Builder md4 = new MottattDokument.Builder()
        .medJournalPostId(new JournalpostId("789"))
        .medDokumentType(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL)
        .medMottattDato(LocalDate.now())
        .medElektroniskRegistrert(false)
        .medXmlPayload("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>") // Skal bare være en string slik at XmlPayLoad ikke er null
        .medId(4L);

    private Behandling opprettOgLagreBehandling() {
        return TestScenarioBuilder.builderMedSøknad().lagre(repositoryProvider);
    }

    @Before
    public void setup() {
        ProsessTaskEventPubliserer prosessTaskEventPubliserer = Mockito.mock(ProsessTaskEventPubliserer.class);
        Mockito.doNothing().when(prosessTaskEventPubliserer).fireEvent(Mockito.any(ProsessTaskData.class), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        prosessTaskRepository = Mockito.spy(new ProsessTaskRepositoryImpl(repoRule.getEntityManager(), prosessTaskEventPubliserer));
        mottatteDokumentTjeneste = mock(MottatteDokumentTjeneste.class);
        when(mottatteDokumentTjeneste.lagreMottattDokumentPåFagsak(any(MottattDokument.class))).thenReturn(MOTTATT_DOKUMENT_ID);

        repositoryProvider = Mockito.spy(new BehandlingRepositoryProvider(repoRule.getEntityManager()));
        behandling = opprettOgLagreBehandling();
    }

    private void mockResterende() {
        saksbehandlingDokumentmottakTjeneste = new SaksbehandlingDokumentmottakTjeneste(prosessTaskRepository, mottatteDokumentTjeneste);

        behandlingsoppretterApplikasjonTjeneste = new BehandlingsoppretterApplikasjonTjeneste(
            repositoryProvider,
            saksbehandlingDokumentmottakTjeneste,
            null);
    }

    private void mockMottatteDokumentRepository(BehandlingRepositoryProvider repositoryProvider) {
        MottatteDokumentRepository mottatteDokumentRepository = mock(MottatteDokumentRepository.class);
        when(mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(behandling.getFagsakId())).thenAnswer(invocation -> {
            List<MottattDokument> mottatteDokumentList = new ArrayList<>();
            MottattDokument md1d = md1.medFagsakId(behandling.getFagsakId()).medBehandlingId(behandling.getId()).build();
            Whitebox.setInternalState(md1d, "opprettetTidspunkt", LocalDateTime.now().minusSeconds(1L));
            mottatteDokumentList.add(md1d);
            MottattDokument md2d = md2.medFagsakId(behandling.getFagsakId()).medBehandlingId(behandling.getId()).build();
            Whitebox.setInternalState(md2d, "opprettetTidspunkt", LocalDateTime.now().minusSeconds(1L));
            mottatteDokumentList.add(md2d);
            MottattDokument md3d = md3.medFagsakId(behandling.getFagsakId()).medBehandlingId(behandling.getId()).build();
            Whitebox.setInternalState(md3d, "opprettetTidspunkt", LocalDateTime.now());
            mottatteDokumentList.add(md3d);
            MottattDokument md4d = md4.medFagsakId(behandling.getFagsakId()).medBehandlingId(behandling.getId()).build();
            Whitebox.setInternalState(md4d, "opprettetTidspunkt", LocalDateTime.now().plusSeconds(1L));
            mottatteDokumentList.add(md4d);
            return mottatteDokumentList;
        });
        when(repositoryProvider.getMottatteDokumentRepository()).thenReturn(mottatteDokumentRepository);
        mockResterende();
    }

    private void mockMottatteDokumentRepositoryElsokMedBehandling(BehandlingRepositoryProvider repositoryProvider) {
        MottatteDokumentRepository mottatteDokumentRepository = mock(MottatteDokumentRepository.class);
        when(mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(behandling.getFagsakId())).thenAnswer(invocation -> {
            List<MottattDokument> mottatteDokumentList = new ArrayList<>();
            MottattDokument md1d = md1.medFagsakId(behandling.getFagsakId()).medBehandlingId(behandling.getId()).build();
            Whitebox.setInternalState(md1d, "opprettetTidspunkt", LocalDateTime.now().minusSeconds(1L));
            mottatteDokumentList.add(md1d);
            MottattDokument md2d = md2.medFagsakId(behandling.getFagsakId()).medBehandlingId(behandling.getId()).build();
            Whitebox.setInternalState(md2d, "opprettetTidspunkt", LocalDateTime.now().minusSeconds(1L));
            mottatteDokumentList.add(md2d);
            return mottatteDokumentList;
        });
        when(repositoryProvider.getMottatteDokumentRepository()).thenReturn(mottatteDokumentRepository);
        mockResterende();
    }

    private void mockMottatteDokumentRepositoryElsokUtenBehandling(BehandlingRepositoryProvider repositoryProvider) {
        MottatteDokumentRepository mottatteDokumentRepository = mock(MottatteDokumentRepository.class);
        when(mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(behandling.getFagsakId())).thenAnswer(invocation -> {
            List<MottattDokument> mottatteDokumentList = new ArrayList<>();
            MottattDokument md1d = md1.medFagsakId(behandling.getFagsakId()).build();
            Whitebox.setInternalState(md1d, "opprettetTidspunkt", LocalDateTime.now().minusSeconds(1L));
            mottatteDokumentList.add(md1d);
            return mottatteDokumentList;
        });
        when(repositoryProvider.getMottatteDokumentRepository()).thenReturn(mottatteDokumentRepository);
        mockResterende();
    }

    @Test(expected = FunksjonellException.class)
    public void skal_kaste_exception_når_behandling_fortsatt_er_åpen() {
        mockMottatteDokumentRepository(repositoryProvider);
        //Act and expect Exception
        behandlingsoppretterApplikasjonTjeneste.opprettNyFørstegangsbehandling(behandling.getFagsakId(), behandling.getFagsak().getSaksnummer(), false);
    }

    @Test(expected = FunksjonellException.class)
    public void skal_kaste_exception_når_behandling_ikke_eksisterer() {
        mockMottatteDokumentRepository(repositoryProvider);
        //Act and expect Exception
        behandlingsoppretterApplikasjonTjeneste.opprettNyFørstegangsbehandling(-1L, new Saksnummer("50"), false);
    }

    @Test
    public void skal_opprette_uten_behandlet() {
        //Arrange
        behandling.avsluttBehandling();
        mockMottatteDokumentRepositoryElsokUtenBehandling(repositoryProvider);

        //Act
        behandlingsoppretterApplikasjonTjeneste.opprettNyFørstegangsbehandling(behandling.getFagsakId(), behandling.getFagsak().getSaksnummer(),false);

        // Assert
        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskRepository, times(1)).lagre(captor.capture());
        ProsessTaskData prosessTaskData = captor.getValue();
        verifiserProsessTaskData(behandling, prosessTaskData, MOTTATT_DOKUMENT_EL_SØKNAD_ID, false);
    }

    @Test
    public void skal_opprette_med_behandling() {
        //Arrange
        behandling.avsluttBehandling();
        mockMottatteDokumentRepositoryElsokMedBehandling(repositoryProvider);

        //Act
        behandlingsoppretterApplikasjonTjeneste.opprettNyFørstegangsbehandling(behandling.getFagsakId(), behandling.getFagsak().getSaksnummer(),false);

        // Assert
        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskRepository, times(1)).lagre(captor.capture());
        ProsessTaskData prosessTaskData = captor.getValue();
        verifiserProsessTaskData(behandling, prosessTaskData, MOTTATT_DOKUMENT_EL_SØKNAD_ID, true);
    }

    @Test(expected = FunksjonellException.class)
    public void skal_feile_uten_tidligere_klagebehandling() {
        //Arrange
        mockMottatteDokumentRepository(repositoryProvider);
        behandling.avsluttBehandling();

        //Act
        behandlingsoppretterApplikasjonTjeneste.opprettNyFørstegangsbehandling(behandling.getFagsakId(), behandling.getFagsak().getSaksnummer(),true);
    }

    //Verifiserer at den opprettede prosesstasken stemmer overens med MottattDokument-mock
    private void verifiserProsessTaskData(Behandling behandling, ProsessTaskData prosessTaskData, Long ventetDokument, boolean skalhabehandling) {

        assertThat(prosessTaskData.getTaskType()).isEqualTo(HåndterMottattDokumentTaskProperties.TASKTYPE);
        assertThat(prosessTaskData.getFagsakId()).isEqualTo(behandling.getFagsakId());
        if (skalhabehandling) {
            assertThat(prosessTaskData.getBehandlingId()).isEqualTo(behandling.getId());
        } else {
            assertThat(prosessTaskData.getBehandlingId()).isNull();
        }
        assertThat(prosessTaskData.getPropertyValue(HåndterMottattDokumentTaskProperties.MOTTATT_DOKUMENT_ID_KEY))
            .isEqualTo(ventetDokument.toString());
    }
}
