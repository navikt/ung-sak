package no.nav.ung.sak.domene.vedtak.fp;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskStatus;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.prosesstask.impl.ProsessTaskRepositoryImpl;
import no.nav.k9.prosesstask.impl.ProsessTaskTjenesteImpl;
import no.nav.k9.prosesstask.impl.TaskManager;
import no.nav.ung.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.iverksett.OpprettProsessTaskIverksett;
import no.nav.ung.sak.domene.iverksett.UngdomsytelseOpprettProsessTaskIverksett;
import no.nav.ung.sak.domene.vedtak.intern.AvsluttBehandlingTask;
import no.nav.ung.sak.hendelse.stønadstatistikk.StønadstatistikkService;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.task.AvsluttOppgaveTask;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.ytelse.kontroll.ManglendeKontrollperioderTjeneste;
import no.nav.ung.sak.økonomi.SendØkonomiOppdragTask;
import no.nav.ung.sak.økonomi.task.VurderOppgaveTilbakekrevingTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

@Disabled
@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class OpprettProsessTaskIverksettTest {

    @Inject
    private EntityManager entityManager;

    private ProsessTaskTjeneste prosessTaskRepository;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;

    @Mock
    private OppgaveTjeneste oppgaveTjeneste;


    @Mock
    private StønadstatistikkService stønadstatistikkService;

    @Mock
    private ManglendeKontrollperioderTjeneste manglendeKontrollperioderTjeneste;

    private Behandling behandling;
    private OpprettProsessTaskIverksett opprettProsessTaskIverksett;

    @BeforeEach
    public void setup() {
        prosessTaskRepository = new ProsessTaskTjenesteImpl(new ProsessTaskRepositoryImpl(entityManager, null, null));
        fagsakProsessTaskRepository = new FagsakProsessTaskRepository(entityManager, prosessTaskRepository, Mockito.mock(TaskManager.class));

        var scenario = TestScenarioBuilder.builderMedSøknad();
        behandling = scenario.lagMocked();
        opprettProsessTaskIverksett = new UngdomsytelseOpprettProsessTaskIverksett(fagsakProsessTaskRepository, stønadstatistikkService);
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
            SendØkonomiOppdragTask.TASKTYPE,
            VurderOppgaveTilbakekrevingTask.TASKTYPE);
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
            AvsluttOppgaveTask.TASKTYPE,
            SendØkonomiOppdragTask.TASKTYPE,
            VurderOppgaveTilbakekrevingTask.TASKTYPE);
    }

    private void mockOpprettTaskAvsluttOppgave() {
        ProsessTaskData prosessTaskData = ProsessTaskData.forProsessTask(AvsluttOppgaveTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setOppgaveId("1001");
        when(oppgaveTjeneste.opprettTaskAvsluttOppgave(any(Behandling.class), any(OppgaveÅrsak.class), anyBoolean())).thenReturn(Optional.of(prosessTaskData));
    }
}
