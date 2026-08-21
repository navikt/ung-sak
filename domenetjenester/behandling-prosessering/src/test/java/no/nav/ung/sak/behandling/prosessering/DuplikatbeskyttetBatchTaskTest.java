package no.nav.ung.sak.behandling.prosessering;

import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskStatus;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.prosesstask.api.TaskType;
import no.nav.k9.prosesstask.impl.cron.CronExpression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DuplikatbeskyttetBatchTaskTest {

    private static final String CHILD_TASK_NAME = "test.childTask";

    private ProsessTaskTjeneste prosessTaskTjeneste;
    private TestBatchHandler handler;

    @BeforeEach
    void setUp() {
        prosessTaskTjeneste = mock(ProsessTaskTjeneste.class);
        handler = new TestBatchHandler(prosessTaskTjeneste);

        // Default: ingen eksisterende tasks
        when(prosessTaskTjeneste.finnAlle(eq(CHILD_TASK_NAME), any(ProsessTaskStatus.class)))
            .thenReturn(List.of());
    }

    @Test
    void skal_opprette_child_task_når_ingen_eksisterende() {
        handler.doTask(new ProsessTaskData("batch.test"));

        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste).lagre(captor.capture());
        assertThat(captor.getValue().getTaskType()).isEqualTo(CHILD_TASK_NAME);
    }

    @Test
    void skal_ikke_opprette_child_task_når_feilet_finnes() {
        var feiletTask = lagTaskUtenSaksnummer();
        when(prosessTaskTjeneste.finnAlle(CHILD_TASK_NAME, ProsessTaskStatus.FEILET))
            .thenReturn(List.of(feiletTask));

        handler.doTask(new ProsessTaskData("batch.test"));

        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskData.class));
    }

    @Test
    void skal_ikke_opprette_child_task_når_klar_finnes() {
        var klarTask = lagTaskUtenSaksnummer();
        when(prosessTaskTjeneste.finnAlle(CHILD_TASK_NAME, ProsessTaskStatus.KLAR))
            .thenReturn(List.of(klarTask));

        handler.doTask(new ProsessTaskData("batch.test"));

        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskData.class));
    }

    @Test
    void skal_ikke_opprette_child_task_når_veto_finnes() {
        var vetoTask = lagTaskUtenSaksnummer();
        when(prosessTaskTjeneste.finnAlle(CHILD_TASK_NAME, ProsessTaskStatus.VETO))
            .thenReturn(List.of(vetoTask));

        handler.doTask(new ProsessTaskData("batch.test"));

        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskData.class));
    }

    @Test
    void skal_ikke_opprette_child_task_når_disabled() {
        handler.setEnabled(false);

        handler.doTask(new ProsessTaskData("batch.test"));

        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskData.class));
        verify(prosessTaskTjeneste, never()).finnAlle(any(), any(ProsessTaskStatus.class));
    }

    @Test
    void skal_ignorere_tasks_som_ikke_matcher_duplikatfilter() {
        // Task med saksnummer settes — default filter ekskluderer disse
        var taskMedSaksnummer = lagTaskMedSaksnummer();
        when(prosessTaskTjeneste.finnAlle(CHILD_TASK_NAME, ProsessTaskStatus.FEILET))
            .thenReturn(List.of(taskMedSaksnummer));

        handler.doTask(new ProsessTaskData("batch.test"));

        verify(prosessTaskTjeneste).lagre(any(ProsessTaskData.class));
    }

    @Test
    void skal_bruke_custom_duplikatfilter() {
        handler = new TestBatchHandlerMedCustomFilter(prosessTaskTjeneste, "matchVerdi");

        var matchendeTask = lagTaskMedProperty("key", "matchVerdi");
        when(prosessTaskTjeneste.finnAlle(CHILD_TASK_NAME, ProsessTaskStatus.FEILET))
            .thenReturn(List.of(matchendeTask));

        handler.doTask(new ProsessTaskData("batch.test"));

        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskData.class));
    }

    private ProsessTaskData lagTaskUtenSaksnummer() {
        ProsessTaskData task = new ProsessTaskData(CHILD_TASK_NAME);
        return task;
    }

    private ProsessTaskData lagTaskMedSaksnummer() {
        ProsessTaskData task = new ProsessTaskData(CHILD_TASK_NAME);
        task.setSaksnummer("12345");
        return task;
    }

    private ProsessTaskData lagTaskMedProperty(String key, String value) {
        ProsessTaskData task = new ProsessTaskData(CHILD_TASK_NAME);
        task.setProperty(key, value);
        return task;
    }

    // --- Test-implementasjoner ---

    static class TestBatchHandler extends DuplikatbeskyttetBatchTask {
        private boolean enabled = true;

        TestBatchHandler(ProsessTaskTjeneste prosessTaskTjeneste) {
            super(prosessTaskTjeneste);
        }

        void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        protected TaskType getTaskType() {
            return new TaskType(CHILD_TASK_NAME);
        }

        @Override
        protected boolean isEnabled() {
            return enabled;
        }

        @Override
        public CronExpression getCron() {
            return CronExpression.create("0 0 7 * * *");
        }
    }

    static class TestBatchHandlerMedCustomFilter extends TestBatchHandler {
        private final String expectedValue;

        TestBatchHandlerMedCustomFilter(ProsessTaskTjeneste prosessTaskTjeneste, String expectedValue) {
            super(prosessTaskTjeneste);
            this.expectedValue = expectedValue;
        }

        @Override
        protected boolean erDuplikat(ProsessTaskData data) {
            return expectedValue.equals(data.getPropertyValue("key"));
        }
    }
}

