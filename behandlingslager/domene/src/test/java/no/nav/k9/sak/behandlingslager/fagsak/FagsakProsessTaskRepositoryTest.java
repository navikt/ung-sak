package no.nav.k9.sak.behandlingslager.fagsak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskStatus;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.prosesstask.api.TaskType;
import no.nav.k9.prosesstask.impl.ProsessTaskRepositoryImpl;
import no.nav.k9.prosesstask.impl.ProsessTaskTjenesteImpl;
import no.nav.k9.prosesstask.impl.TaskManager;
import no.nav.k9.sak.db.util.CdiDbAwareTest;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

@CdiDbAwareTest
class FagsakProsessTaskRepositoryTest {

    @Inject
    private EntityManager entityManager;
    @Inject
    private FagsakRepository fagsakRepository;

    private FagsakProsessTaskRepository fagsakProsessTaskRepository;
    private TaskManager taskManager;

    private Long fagsakId;
    private Long behandlingId;
    private final TaskType fortsettBehandlingTaskType = new TaskType("behandlingskontroll.fortsettBehandling");

    @BeforeEach
    public void setup() {
        ProsessTaskTjeneste prosessTaskTjeneste = new ProsessTaskTjenesteImpl(new ProsessTaskRepositoryImpl(entityManager, null, null));
        taskManager = Mockito.mock(TaskManager.class);
        fagsakProsessTaskRepository = new FagsakProsessTaskRepository(entityManager, prosessTaskTjeneste, taskManager);

        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, AktørId.dummy(), mock(Saksnummer.class), LocalDate.now(), null);
        fagsakId = fagsakRepository.opprettNy(fagsak);
        behandlingId = fagsakId + 1;
    }

    @Test
    void skalLagreNyTaskNårViIkkeHarAndreTaskerPåBehandling() {
        ProsessTaskData nyTask = ProsessTaskData.forTaskType(fortsettBehandlingTaskType);
        nyTask.setBehandling(fagsakId, behandlingId);
        nyTask.setStatus(ProsessTaskStatus.KLAR);
        ProsessTaskGruppe nyGruppe = new ProsessTaskGruppe(nyTask);

        fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId, nyGruppe);
        assertThat(nyTask.getId()).isNotNull();
    }

    @Test
    void skalLagreNyTaskNårViIkkeHarAndreTaskerAvSammeTypePåBehandling() {
        ProsessTaskData eksisterendeTask = ProsessTaskData.forTaskType(fortsettBehandlingTaskType);
        eksisterendeTask.setBehandling(fagsakId, behandlingId);
        eksisterendeTask.setStatus(ProsessTaskStatus.KLAR);
        ProsessTaskGruppe eksisterendeGruppe = new ProsessTaskGruppe(eksisterendeTask);
        fagsakProsessTaskRepository.lagreNyGruppe(eksisterendeGruppe);
        FagsakProsessTask fagsakProsessTask1 = new FagsakProsessTask(fagsakId, behandlingId.toString(), eksisterendeTask.getId(), 444L, fortsettBehandlingTaskType.value());
        fagsakProsessTaskRepository.lagre(fagsakProsessTask1);

        ProsessTaskData nyTask = ProsessTaskData.forTaskType(new TaskType("en.annen.task.type"));
        nyTask.setBehandling(fagsakId, behandlingId);
        nyTask.setStatus(ProsessTaskStatus.KLAR);
        ProsessTaskGruppe nyGruppe = new ProsessTaskGruppe(nyTask);

        fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId, nyGruppe);
        assertThat(nyTask.getId()).isNotNull();
    }

    @Test
    void skalFeileNårViHarAnnenTaskAvSammeTypePåBehandling() {
        ProsessTaskData feiletTask = ProsessTaskData.forTaskType(fortsettBehandlingTaskType);
        feiletTask.setBehandling(fagsakId, behandlingId);
        feiletTask.setStatus(ProsessTaskStatus.KLAR);
        fagsakProsessTaskRepository.lagreNyGruppe(feiletTask);
        FagsakProsessTask fagsakProsessTask1 = new FagsakProsessTask(fagsakId, behandlingId.toString(), feiletTask.getId(), 333L, fortsettBehandlingTaskType.value());
        fagsakProsessTaskRepository.lagre(fagsakProsessTask1);

        ProsessTaskData nyTask = ProsessTaskData.forTaskType(fortsettBehandlingTaskType);
        nyTask.setBehandling(fagsakId, behandlingId);
        nyTask.setStatus(ProsessTaskStatus.KLAR);
        ProsessTaskGruppe nyGruppe = new ProsessTaskGruppe(nyTask);

        assertThrows(IllegalStateException.class, () ->
            fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId, nyGruppe));
    }

    @Test
    void skalFeileNårViHarFeiletTaskPåBehandling() {
        ProsessTaskData feiletTask = ProsessTaskData.forTaskType(fortsettBehandlingTaskType);
        feiletTask.setBehandling(fagsakId, behandlingId);
        feiletTask.setStatus(ProsessTaskStatus.FEILET);
        fagsakProsessTaskRepository.lagreNyGruppe(feiletTask);
        FagsakProsessTask fagsakProsessTask1 = new FagsakProsessTask(fagsakId, behandlingId.toString(), feiletTask.getId(), 333L, fortsettBehandlingTaskType.value());
        fagsakProsessTaskRepository.lagre(fagsakProsessTask1);

        ProsessTaskData nyTask = ProsessTaskData.forTaskType(new TaskType("en.annen.task.type"));
        nyTask.setBehandling(fagsakId, behandlingId);
        nyTask.setStatus(ProsessTaskStatus.KLAR);
        ProsessTaskGruppe nyGruppe = new ProsessTaskGruppe(nyTask);
        assertThrows(IllegalStateException.class, () ->
            fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId, nyGruppe));
    }

    @Test
    void skalIkkeLagreNyTaskNårTaskAvSammeTypeErVetoetAvKjørendeTask() {
        ProsessTaskData kjørendeTask = ProsessTaskData.forTaskType(fortsettBehandlingTaskType);
        kjørendeTask.setBehandling(fagsakId, behandlingId);
        kjørendeTask.setStatus(ProsessTaskStatus.KLAR);
        fagsakProsessTaskRepository.lagreNyGruppe(kjørendeTask);
        FagsakProsessTask fagsakProsessTask1 = new FagsakProsessTask(fagsakId, behandlingId.toString(), kjørendeTask.getId(), 111L, fortsettBehandlingTaskType.value());
        fagsakProsessTaskRepository.lagre(fagsakProsessTask1);
        when(taskManager.getCurrentTask()).thenReturn(kjørendeTask);

        ProsessTaskData vetoetTask = ProsessTaskData.forTaskType(fortsettBehandlingTaskType);
        vetoetTask.setBehandling(fagsakId, behandlingId);
        vetoetTask.setStatus(ProsessTaskStatus.VETO);
        vetoetTask.setBlokkertAvProsessTaskId(kjørendeTask.getId());
        ProsessTaskGruppe eksisterendeGruppe = new ProsessTaskGruppe(vetoetTask);
        fagsakProsessTaskRepository.lagreNyGruppe(eksisterendeGruppe);
        FagsakProsessTask fagsakProsessTask2 = new FagsakProsessTask(fagsakId, behandlingId.toString(), vetoetTask.getId(), 222L, fortsettBehandlingTaskType.value());
        fagsakProsessTaskRepository.lagre(fagsakProsessTask2);

        ProsessTaskData nyTask = ProsessTaskData.forTaskType(fortsettBehandlingTaskType);
        nyTask.setBehandling(fagsakId, behandlingId);
        nyTask.setStatus(ProsessTaskStatus.KLAR);
        ProsessTaskGruppe nyGruppe = new ProsessTaskGruppe(nyTask);
        fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId, nyGruppe);

        assertThat(nyTask.getId()).isNull();
        var alleTasker = fagsakProsessTaskRepository.sjekkStatusProsessTasks(fagsakId, behandlingId, null);
        assertThat(alleTasker).hasSize(2);
        assertThat(alleTasker.stream()
            .filter(t -> !Objects.equals(t.getId(), kjørendeTask.getId()) && !Objects.equals(t.getId(), vetoetTask.getId())))
            .isEmpty();
    }
}
