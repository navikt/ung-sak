package no.nav.k9.sak.behandling.prosessering;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskStatus;
import no.nav.k9.prosesstask.api.TaskType;

class BehandlingProsesseringTjenesteImplTest {

    private BehandlingProsesseringTjenesteImpl tjeneste = new BehandlingProsesseringTjenesteImpl();

    @Test
    void skal_ikke_med_hvis_blokkert_av_kjørende_task() {
        var taskId = 1L;
        var data =  ProsessTaskData.forTaskType(new TaskType("1234"));
        data.setBlokkertAvProsessTaskId(taskId);
        data.setStatus(ProsessTaskStatus.VETO);

        assertThat(tjeneste.taskerSomKanBlokkere(taskId, data)).isFalse();
    }

    @Test
    void skal_med_hvis_ikke_blokkert_av_kjørende_task() {
        var taskId = 1L;
        var data =  ProsessTaskData.forTaskType(new TaskType("1234"));
        data.setStatus(ProsessTaskStatus.KLAR);

        assertThat(tjeneste.taskerSomKanBlokkere(taskId, data)).isTrue();
    }

    @Test
    void skal_med_hvis_ikke_blokkert_av_kjørende_task_1() {
        var taskId = 1L;
        var data =  ProsessTaskData.forTaskType(new TaskType("1234"));
        data.setBlokkertAvProsessTaskId(taskId);
        data.setStatus(ProsessTaskStatus.KLAR);

        assertThat(tjeneste.taskerSomKanBlokkere(taskId, data)).isTrue();
    }
}
