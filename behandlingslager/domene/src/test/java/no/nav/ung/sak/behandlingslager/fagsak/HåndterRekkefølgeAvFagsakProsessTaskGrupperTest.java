package no.nav.ung.sak.behandlingslager.fagsak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.prosesstask.api.TaskType;

class HåndterRekkefølgeAvFagsakProsessTaskGrupperTest {

    private static final Long FAGSAK_ID = 10L;
    private static final Long BLOKKERENDE_TASK_ID = 1000L;

    private FagsakProsessTaskRepository repository;
    private ProsessTaskTjeneste taskTjeneste;
    private HåndterRekkefølgeAvFagsakProsessTaskGrupper håndterer;
    private ProsessTaskData task;

    @BeforeEach
    void setUp() {
        repository = mock(FagsakProsessTaskRepository.class);
        taskTjeneste = mock(ProsessTaskTjeneste.class);
        håndterer = new HåndterRekkefølgeAvFagsakProsessTaskGrupper(repository, taskTjeneste);

        task = ProsessTaskData.forTaskType(new TaskType("behandlingskontroll.opprettRevurderingEllerDiff"));
        task.setId(2000L);
        task.setFagsakId(FAGSAK_ID);
    }

    @Test
    void skal_vetoe_når_blokkerende_task_finnes() {
        var blokkerende = new FagsakProsessTask(FAGSAK_ID, "20", BLOKKERENDE_TASK_ID, 100L, "behandlingskontroll.fortsettBehandling");
        when(repository.sjekkTillattKjøreFagsakProsessTask(task)).thenReturn(Optional.of(blokkerende));
        when(repository.prosessTaskFinnes(BLOKKERENDE_TASK_ID)).thenReturn(true);
        var blokkerendeTask = ProsessTaskData.forTaskType(new TaskType("behandlingskontroll.fortsettBehandling"));
        blokkerendeTask.setId(BLOKKERENDE_TASK_ID);
        when(taskTjeneste.finn(BLOKKERENDE_TASK_ID)).thenReturn(blokkerendeTask);

        var veto = håndterer.vetoKjøring(task);

        assertThat(veto.isVeto()).isTrue();
        assertThat(veto.getBlokkertAvProsessTaskId()).isEqualTo(BLOKKERENDE_TASK_ID);
    }

    @Test
    void skal_feile_med_tydelig_melding_når_blokkerende_task_ikke_finnes() {
        var koblingUtenProsessTask = new FagsakProsessTask(FAGSAK_ID, "20", BLOKKERENDE_TASK_ID, 100L, "behandlingskontroll.fortsettBehandling");
        when(repository.sjekkTillattKjøreFagsakProsessTask(task)).thenReturn(Optional.of(koblingUtenProsessTask));
        when(repository.prosessTaskFinnes(BLOKKERENDE_TASK_ID)).thenReturn(false);

        assertThatThrownBy(() -> håndterer.vetoKjøring(task))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("prosesstask[" + BLOKKERENDE_TASK_ID + "]")
            .hasMessageContaining("fagsak[" + FAGSAK_ID + "]")
            .hasMessageContaining("gruppe_sekvensnr[100]")
            .hasMessageContaining("POST /forvaltning/fagsak-prosess-task/fjern-koblinger-uten-prosesstask");
        verify(taskTjeneste, never()).finn(BLOKKERENDE_TASK_ID);
    }

    @Test
    void skal_ikke_vetoe_når_ingen_task_blokkerer() {
        when(repository.sjekkTillattKjøreFagsakProsessTask(task)).thenReturn(Optional.empty());

        var veto = håndterer.vetoKjøring(task);

        assertThat(veto.isVeto()).isFalse();
    }

    @Test
    void skal_ikke_vetoe_task_uten_fagsak() {
        var taskUtenFagsak = ProsessTaskData.forTaskType(new TaskType("noe.annet"));
        taskUtenFagsak.setId(2L);

        var veto = håndterer.vetoKjøring(taskUtenFagsak);

        assertThat(veto.isVeto()).isFalse();
        verify(repository, never()).sjekkTillattKjøreFagsakProsessTask(taskUtenFagsak);
    }
}
