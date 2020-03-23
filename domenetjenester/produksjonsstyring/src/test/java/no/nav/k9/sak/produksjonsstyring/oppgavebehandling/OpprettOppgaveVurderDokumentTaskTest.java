package no.nav.k9.sak.produksjonsstyring.oppgavebehandling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakLås;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderDokumentTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

public class OpprettOppgaveVurderDokumentTaskTest {

    private static final long FAGSAK_ID = 2L;

    private OppgaveTjeneste oppgaveTjeneste;
    private BehandlingRepositoryProvider repositoryProvider;
    private OpprettOppgaveVurderDokumentTask opprettOppgaveVurderDokumentTask;
    private FagsakLåsRepository låsRepository;

    @Before
    public void before() {
        oppgaveTjeneste = mock(OppgaveTjeneste.class);
        repositoryProvider = mock(BehandlingRepositoryProvider.class);
        låsRepository = mock(FagsakLåsRepository.class);

        when(repositoryProvider.getFagsakLåsRepository()).thenReturn(låsRepository);
        when(låsRepository.taLås(anyLong())).thenReturn(mock(FagsakLås.class));

        opprettOppgaveVurderDokumentTask = new OpprettOppgaveVurderDokumentTask(oppgaveTjeneste, repositoryProvider);
    }

    @Test
    public void skal_opprette_oppgave_for_å_vurdere_dokument_basert_på_fagsakId() {
        DokumentTypeId dokumentTypeId = DokumentTypeId.INNTEKTSMELDING;

        // Arrange
        ProsessTaskData prosessTaskData = new ProsessTaskData(OpprettOppgaveVurderDokumentTask.TASKTYPE);
        prosessTaskData.setFagsakId(FAGSAK_ID);
        prosessTaskData.setProperty(OpprettOppgaveVurderDokumentTask.KEY_DOKUMENT_TYPE, dokumentTypeId.getKode());
        ArgumentCaptor<Long> fagsakIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<OppgaveÅrsak> årsakCaptor = ArgumentCaptor.forClass(OppgaveÅrsak.class);
        ArgumentCaptor<String> fordelingsoppgaveEnhetsIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> beskrivelseCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Boolean> priCaptor = ArgumentCaptor.forClass(Boolean.class);

        // Act
        opprettOppgaveVurderDokumentTask.doTask(prosessTaskData);

        // Assert
        verify(oppgaveTjeneste).opprettMedPrioritetOgBeskrivelseBasertPåFagsakId(fagsakIdCaptor.capture(), årsakCaptor.capture(),
            fordelingsoppgaveEnhetsIdCaptor.capture(), beskrivelseCaptor.capture(), priCaptor.capture());
        assertThat(fagsakIdCaptor.getValue()).isEqualTo(FAGSAK_ID);
        assertThat(årsakCaptor.getValue()).isEqualTo(OppgaveÅrsak.VURDER_DOKUMENT);
        assertThat(beskrivelseCaptor.getValue()).isEqualTo("VL: " + dokumentTypeId.getNavn()); // Antar testhelper, ellers bruk finn+navn
    }
}
