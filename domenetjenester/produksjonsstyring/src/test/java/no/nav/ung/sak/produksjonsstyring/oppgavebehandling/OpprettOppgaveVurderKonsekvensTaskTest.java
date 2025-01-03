package no.nav.ung.sak.produksjonsstyring.oppgavebehandling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import no.nav.ung.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakLås;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderKonsekvensTask;

public class OpprettOppgaveVurderKonsekvensTaskTest {

    private static final long FAGSAK_ID = 2L;
    private OppgaveTjeneste oppgaveTjeneste;
    private OpprettOppgaveVurderKonsekvensTask opprettOppgaveVurderKonsekvensTask;
    private BehandlingRepositoryProvider repositoryProvider;
    private FagsakLåsRepository låsRepository;

    @BeforeEach
    public void before() {
        oppgaveTjeneste = mock(OppgaveTjeneste.class);
        repositoryProvider = mock(BehandlingRepositoryProvider.class);
        låsRepository = mock(FagsakLåsRepository.class);

        when(repositoryProvider.getFagsakLåsRepository()).thenReturn(låsRepository);
        when(låsRepository.taLås(anyLong())).thenReturn(mock(FagsakLås.class));

        opprettOppgaveVurderKonsekvensTask = new OpprettOppgaveVurderKonsekvensTask(oppgaveTjeneste, repositoryProvider);
    }

    @Test
    public void skal_opprette_oppgave_for_å_vurdere_konsekvens_basert_på_fagsakId() {
        // Arrange
        ProsessTaskData prosessTaskData =  ProsessTaskData.forProsessTask(OpprettOppgaveVurderKonsekvensTask.class);
        prosessTaskData.setFagsakId(FAGSAK_ID);
        prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_BESKRIVELSE, OpprettOppgaveVurderKonsekvensTask.STANDARD_BESKRIVELSE);
        prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_PRIORITET, OpprettOppgaveVurderKonsekvensTask.PRIORITET_NORM);
        ArgumentCaptor<Long> fagsakIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<OppgaveÅrsak> årsakCaptor = ArgumentCaptor.forClass(OppgaveÅrsak.class);
        ArgumentCaptor<String> beskrivelseCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        opprettOppgaveVurderKonsekvensTask.doTask(prosessTaskData);

        // Assert
        verify(oppgaveTjeneste).opprettMedPrioritetOgBeskrivelseBasertPåFagsakId(fagsakIdCaptor.capture(), årsakCaptor.capture(), any(),
                beskrivelseCaptor.capture(), Mockito.eq(false));
        assertThat(fagsakIdCaptor.getValue()).isEqualTo(FAGSAK_ID);
        assertThat(årsakCaptor.getValue()).isEqualTo(OppgaveÅrsak.VURDER_KONSEKVENS_YTELSE);
        assertThat(beskrivelseCaptor.getValue()).isEqualTo(OpprettOppgaveVurderKonsekvensTask.STANDARD_BESKRIVELSE);
    }
}
