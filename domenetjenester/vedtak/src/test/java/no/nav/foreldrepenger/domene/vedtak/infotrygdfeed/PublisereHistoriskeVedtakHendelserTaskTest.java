package no.nav.foreldrepenger.domene.vedtak.infotrygdfeed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.domene.vedtak.midlertidig.PublisereHistoriskeVedtakHendelserTask;
import no.nav.k9.sak.domene.vedtak.observer.PubliserVedtakHendelseTask;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PublisereHistoriskeVedtakHendelserTaskTest {

    private BehandlingVedtakRepository vedtakRepository;

    private ProsessTaskTjeneste taskTjeneste;

    private BehandlingLåsRepository behandlingLåsRepository;

    ProsessTaskData prosessTaskData =  ProsessTaskData.forProsessTask(PublisereHistoriskeVedtakHendelserTask.class);

    PublisereHistoriskeVedtakHendelserTask task;

    @BeforeEach
    public void setup() {
        vedtakRepository = mock(BehandlingVedtakRepository.class);
        taskTjeneste = mock(ProsessTaskTjeneste.class);
        behandlingLåsRepository = mock(BehandlingLåsRepository.class);

        task = new PublisereHistoriskeVedtakHendelserTask(vedtakRepository, behandlingLåsRepository, taskTjeneste);
    }

    @Test
    public void skalPublisereVedtakHendelserForVedtakIkkeTidligerePublisert() {
        BehandlingVedtak vedtak = mock(BehandlingVedtak.class);
        when(vedtak.getBehandlingId()).thenReturn(123L);
        when(vedtakRepository.hentBehandlingVedtakSomIkkeErPublisert(anyInt())).thenReturn(Collections.singletonList(vedtak));

        task.doTask(prosessTaskData);

        ArgumentCaptor<ProsessTaskData> prosessTaskDataCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste, times(2)).lagre(prosessTaskDataCaptor.capture());
        var nyeProsesstasker = prosessTaskDataCaptor.getAllValues();

        assertThat(nyeProsesstasker.stream().map(ProsessTaskData::getTaskType))
            .containsExactlyInAnyOrder(
                PubliserVedtakHendelseTask.TASKTYPE,
                PublisereHistoriskeVedtakHendelserTask.TASKTYPE
            );
    }

    @Test
    public void skalIkkeLageNyeProsesstaskerVedTomtResultatset() {
        when(vedtakRepository.hentBehandlingVedtakSomIkkeErPublisert(anyInt())).thenReturn(Collections.emptyList());

        task.doTask(prosessTaskData);

        verify(taskTjeneste, never()).lagre(any(ProsessTaskData.class));
    }
}
