package no.nav.k9.sak.domene.vedtak.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedService;
import no.nav.k9.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.iverksett.OpprettProsessTaskIverksett;
import no.nav.k9.sak.domene.iverksett.OpprettProsessTaskIverksettImpl;
import no.nav.k9.sak.domene.vedtak.ekstern.VurderOppgaveArenaTask;
import no.nav.k9.sak.domene.vedtak.intern.AvsluttBehandlingTask;
import no.nav.k9.sak.domene.vedtak.intern.SendVedtaksbrevTask;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.task.AvsluttOppgaveTaskProperties;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.økonomi.SendØkonomiOppdragTask;
import no.nav.k9.sak.økonomi.task.VurderOppgaveTilbakekrevingTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskRepositoryImpl;
import no.nav.vedtak.felles.prosesstask.impl.TaskManager;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

@Disabled
@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class OpprettProsessTaskIverksettTest {

    @Inject
    private EntityManager entityManager;

    private ProsessTaskRepository prosessTaskRepository ;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository ;

    @Mock
    private OppgaveTjeneste oppgaveTjeneste;

    @Mock
    private InfotrygdFeedService infotrygdFeedService;


    private Behandling behandling;
    private OpprettProsessTaskIverksett opprettProsessTaskIverksett;

    @BeforeEach
    public void setup() {
        prosessTaskRepository = new ProsessTaskRepositoryImpl(entityManager, null, null);
        fagsakProsessTaskRepository = new FagsakProsessTaskRepository(entityManager, prosessTaskRepository, Mockito.mock(TaskManager.class));

        var scenario = TestScenarioBuilder.builderMedSøknad();
        behandling = scenario.lagMocked();
        opprettProsessTaskIverksett = new OpprettProsessTaskIverksettImpl(fagsakProsessTaskRepository, oppgaveTjeneste, infotrygdFeedService);
    }

    @Test
    public void skalIkkeAvslutteOppgave() {
        // Arrange
        when(oppgaveTjeneste.opprettTaskAvsluttOppgave(any(Behandling.class), any(OppgaveÅrsak.class), anyBoolean())).thenReturn(Optional.empty());

        // Act
        opprettProsessTaskIverksett.opprettIverksettingstasker(behandling, Optional.empty());

        // Assert
        List<ProsessTaskData> prosessTaskDataList = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        List<String> tasktyper = prosessTaskDataList.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(AvsluttBehandlingTask.TASKTYPE,
            SendVedtaksbrevTask.TASKTYPE,
            SendØkonomiOppdragTask.TASKTYPE,
            VurderOppgaveArenaTask.TASKTYPE,
            VurderOppgaveTilbakekrevingTask.TASKTYPE);
        verify(infotrygdFeedService).publiserHendelse(behandling);
    }

    @Test
    public void testOpprettIverksettingstasker() {
        // Arrange
        mockOpprettTaskAvsluttOppgave();

        // Act
        opprettProsessTaskIverksett.opprettIverksettingstasker(behandling);

        // Assert
        List<ProsessTaskData> prosessTaskDataList = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        List<String> tasktyper = prosessTaskDataList.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(AvsluttBehandlingTask.TASKTYPE,
            SendVedtaksbrevTask.TASKTYPE,
            AvsluttOppgaveTaskProperties.TASKTYPE,
            SendØkonomiOppdragTask.TASKTYPE,
            VurderOppgaveArenaTask.TASKTYPE,
            VurderOppgaveTilbakekrevingTask.TASKTYPE);
        verify(infotrygdFeedService).publiserHendelse(behandling);
    }

    private void mockOpprettTaskAvsluttOppgave() {
        ProsessTaskData prosessTaskData = new ProsessTaskData(AvsluttOppgaveTaskProperties.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setOppgaveId("1001");
        when(oppgaveTjeneste.opprettTaskAvsluttOppgave(any(Behandling.class), any(OppgaveÅrsak.class), anyBoolean())).thenReturn(Optional.of(prosessTaskData));
    }
}
