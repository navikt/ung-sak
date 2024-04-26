package no.nav.k9.sak.behandling.revurdering.etterkontroll.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.Etterkontroll;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.KontrollType;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.task.AutomatiskEtterkontrollTask;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class AutomatiskEtterkontrollBatchTaskTest {

    @Inject
    private EntityManager entityManager;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private EtterkontrollRepository etterkontrollRepository;

    private final ProsessTaskTjeneste prosessTaskTjeneste = mock();

    private AutomatiskEtterkontrollBatchTask task;

    @BeforeEach
    public void before() {
        BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        task = new AutomatiskEtterkontrollBatchTask(repositoryProvider, etterkontrollRepository, prosessTaskTjeneste);
    }


    @Test
    public void skal_ikke_velge_kontroller_frem_i_tid() {
        Behandling behandling = opprettBehandling();

        Etterkontroll etterkontroll = new Etterkontroll.Builder(behandling)
            .medErBehandlet(false)
            .medKontrollTidspunkt(LocalDateTime.now().plusDays(2))
            .medKontrollType(KontrollType.FORSINKET_SAKSBEHANDLINGSTID)
            .build();
        etterkontrollRepository.lagre(etterkontroll);

        task.doTask(null);

        verifyNoInteractions(prosessTaskTjeneste);

    }

    @Test
    public void skal_velge_kontroller_for_behandling() {
        Behandling behandling = opprettBehandling();

        Etterkontroll etterkontroll = new Etterkontroll.Builder(behandling)
            .medErBehandlet(false)
            .medKontrollTidspunkt(LocalDateTime.now().minusDays(1))
            .medKontrollType(KontrollType.FORSINKET_SAKSBEHANDLINGSTID).build();
        etterkontrollRepository.lagre(etterkontroll);

        task.doTask(null);

        List<ProsessTaskData> nyeProsesstasker = hentProsesstasker();

        assertThat(nyeProsesstasker).hasSize(1);

        ProsessTaskData taskData = nyeProsesstasker.get(0);
        assertThat(taskData.getTaskType()).isEqualTo(AutomatiskEtterkontrollTask.TASKTYPE);
        assertThat(taskData.getBehandlingId()).isEqualTo(behandling.getId().toString());

    }

    private List<ProsessTaskData> hentProsesstasker() {
        ArgumentCaptor<ProsessTaskData> prosessTaskDataCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste, times(1)).lagre(prosessTaskDataCaptor.capture());
        return prosessTaskDataCaptor.getAllValues();
    }

    private Behandling opprettBehandling() {
        var scenario = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingsresultat(BehandlingResultatType.INNVILGET);
        scenario.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now().minusDays(1))
            .medAnsvarligSaksbehandler("asdf");
        var behandling = scenario.lagre(entityManager);

        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        return behandling;
    }

}
