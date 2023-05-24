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
    private final TaskType taskTypeA = new TaskType("A");
    private final TaskType taskTypeB = new TaskType("B");
    private final TaskType taskTypeC = new TaskType("C");

    @BeforeEach
    void setup() {
        ProsessTaskTjeneste prosessTaskTjeneste = new ProsessTaskTjenesteImpl(new ProsessTaskRepositoryImpl(entityManager, null, null));
        taskManager = Mockito.mock(TaskManager.class);
        fagsakProsessTaskRepository = new FagsakProsessTaskRepository(entityManager, prosessTaskTjeneste, taskManager);

        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, AktørId.dummy(), mock(Saksnummer.class), LocalDate.now(), null);
        fagsakId = fagsakRepository.opprettNy(fagsak);
        behandlingId = fagsakId + 1;
    }

    private ProsessTaskData setupKjørendeTask(TaskType taskType) {
        ProsessTaskData kjørendeTask = lagTask(taskType, ProsessTaskStatus.KLAR);
        fagsakProsessTaskRepository.lagreNyGruppe(kjørendeTask);
        FagsakProsessTask fagsakProsessTask = new FagsakProsessTask(fagsakId, behandlingId.toString(), kjørendeTask.getId(), 111L, taskType.value());
        fagsakProsessTaskRepository.lagre(fagsakProsessTask);
        when(taskManager.getCurrentTask()).thenReturn(kjørendeTask);
        return kjørendeTask;
    }

    private ProsessTaskData lagTask(TaskType taskType, ProsessTaskStatus status) {
        ProsessTaskData nyTask = ProsessTaskData.forTaskType(taskType);
        nyTask.setBehandling(fagsakId, behandlingId);
        nyTask.setStatus(status);
        return nyTask;
    }

    @Test
    void skalLagreNyTaskNårViIkkeHarAndreTaskerPåBehandling() {
        ProsessTaskData nyTask = lagTask(taskTypeA, ProsessTaskStatus.KLAR);
        ProsessTaskGruppe nyGruppe = new ProsessTaskGruppe(nyTask);

        fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId, nyGruppe);
        assertThat(nyTask.getId()).isNotNull();
    }

    @Test
    void skalLagreNyTaskNårViIkkeHarAndreTaskerAvSammeTypePåBehandling() {
        ProsessTaskData eksisterendeTask = lagTask(taskTypeA, ProsessTaskStatus.KLAR);
        ProsessTaskGruppe eksisterendeGruppe = new ProsessTaskGruppe(eksisterendeTask);
        fagsakProsessTaskRepository.lagreNyGruppe(eksisterendeGruppe);
        FagsakProsessTask fagsakProsessTask1 = new FagsakProsessTask(fagsakId, behandlingId.toString(), eksisterendeTask.getId(), 444L, eksisterendeTask.getTaskType());
        fagsakProsessTaskRepository.lagre(fagsakProsessTask1);

        ProsessTaskData nyTask = lagTask(taskTypeB, ProsessTaskStatus.KLAR);
        ProsessTaskGruppe nyGruppe = new ProsessTaskGruppe(nyTask);

        fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId, nyGruppe);
        assertThat(nyTask.getId()).isNotNull();
    }

    @Test
    void skalFeileNårViHarAnnenTaskAvSammeTypePåBehandling() {
        ProsessTaskData eksisterendeTask = lagTask(taskTypeA, ProsessTaskStatus.KLAR);
        fagsakProsessTaskRepository.lagreNyGruppe(eksisterendeTask);
        FagsakProsessTask fagsakProsessTask1 = new FagsakProsessTask(fagsakId, behandlingId.toString(), eksisterendeTask.getId(), 333L, eksisterendeTask.getTaskType());
        fagsakProsessTaskRepository.lagre(fagsakProsessTask1);

        ProsessTaskData nyTask = lagTask(taskTypeA, ProsessTaskStatus.KLAR);
        ProsessTaskGruppe nyGruppe = new ProsessTaskGruppe(nyTask);

        assertThrows(IllegalStateException.class, () ->
            fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId, nyGruppe));
    }

    @Test
    void skalFeileNårViHarFeiletTaskPåBehandling() {
        ProsessTaskData feiletTask = lagTask(taskTypeA, ProsessTaskStatus.FEILET);
        fagsakProsessTaskRepository.lagreNyGruppe(feiletTask);
        FagsakProsessTask fagsakProsessTask1 = new FagsakProsessTask(fagsakId, behandlingId.toString(), feiletTask.getId(), 333L, feiletTask.getTaskType());
        fagsakProsessTaskRepository.lagre(fagsakProsessTask1);

        ProsessTaskData nyTask = lagTask(taskTypeB, ProsessTaskStatus.KLAR);
        ProsessTaskGruppe nyGruppe = new ProsessTaskGruppe(nyTask);

        assertThrows(IllegalStateException.class, () ->
            fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId, nyGruppe));
    }

    @Test
    void skalLagreNyTaskSelvOmKjørendeTaskErAvSammeType() {
        setupKjørendeTask(taskTypeA);

        ProsessTaskData nyTask = lagTask(taskTypeA, ProsessTaskStatus.KLAR);
        ProsessTaskGruppe nyGruppe = new ProsessTaskGruppe(nyTask);

        fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId, nyGruppe);
        assertThat(nyTask.getId()).isNotNull();
    }

    @Test
    void skalLagreNyTaskSelvOmTaskAvAnnenTypeErVetoetAvKjørendeTask() {
        ProsessTaskData kjørendeTask = setupKjørendeTask(taskTypeA);

        ProsessTaskData vetoetTask = lagTask(taskTypeB, ProsessTaskStatus.VETO);
        vetoetTask.setBlokkertAvProsessTaskId(kjørendeTask.getId());
        fagsakProsessTaskRepository.lagreNyGruppe(vetoetTask);
        FagsakProsessTask fagsakProsessTask1 = new FagsakProsessTask(fagsakId, behandlingId.toString(), vetoetTask.getId(), 222L, vetoetTask.getTaskType());
        fagsakProsessTaskRepository.lagre(fagsakProsessTask1);

        ProsessTaskData nyTask = lagTask(taskTypeC, ProsessTaskStatus.KLAR);
        ProsessTaskGruppe nyGruppe = new ProsessTaskGruppe(nyTask);

        fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId, nyGruppe);
        assertThat(nyTask.getId()).isNotNull();
    }

    @Test
    void skalIkkeLagreNyTaskNårTaskAvSammeTypeErVetoetAvKjørendeTask() {
        ProsessTaskData kjørendeTask = setupKjørendeTask(taskTypeA);

        ProsessTaskData vetoetTask = lagTask(taskTypeB, ProsessTaskStatus.VETO);
        vetoetTask.setBlokkertAvProsessTaskId(kjørendeTask.getId());
        ProsessTaskData taskIGruppeMedVetoet = lagTask(taskTypeC, ProsessTaskStatus.KLAR);
        ProsessTaskGruppe eksisterendeGruppe = new ProsessTaskGruppe(vetoetTask);
        eksisterendeGruppe.addNesteSekvensiell(taskIGruppeMedVetoet);
        fagsakProsessTaskRepository.lagreNyGruppe(eksisterendeGruppe);
        FagsakProsessTask fagsakProsessTask1 = new FagsakProsessTask(fagsakId, behandlingId.toString(), vetoetTask.getId(), 222L, vetoetTask.getTaskType());
        fagsakProsessTaskRepository.lagre(fagsakProsessTask1);
        FagsakProsessTask fagsakProsessTask2 = new FagsakProsessTask(fagsakId, behandlingId.toString(), taskIGruppeMedVetoet.getId(), 333L, taskIGruppeMedVetoet.getTaskType());
        fagsakProsessTaskRepository.lagre(fagsakProsessTask2);

        ProsessTaskData nyTask = lagTask(taskTypeB, ProsessTaskStatus.KLAR);
        ProsessTaskGruppe nyGruppe = new ProsessTaskGruppe(nyTask);
        fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId, nyGruppe);

        assertThat(nyTask.getId()).isNull();
        var alleTasker = fagsakProsessTaskRepository.sjekkStatusProsessTasks(fagsakId, behandlingId, null);
        assertThat(alleTasker).hasSize(3);
        assertThat(alleTasker.stream()
            .filter(t -> !Objects.equals(t.getId(), kjørendeTask.getId()) && !Objects.equals(t.getId(), vetoetTask.getId()) && !Objects.equals(t.getId(), taskIGruppeMedVetoet.getId())))
            .isEmpty();
    }

    @Test
    void skalIkkeLagreNyTaskNårTaskAvSammeTypeErISammeGruppeSomTaskVetoetAvKjørendeTask() {
        ProsessTaskData kjørendeTask = setupKjørendeTask(taskTypeA);

        ProsessTaskData vetoetTask = lagTask(taskTypeB, ProsessTaskStatus.VETO);
        vetoetTask.setBlokkertAvProsessTaskId(kjørendeTask.getId());
        ProsessTaskData taskIGruppeMedVetoet = lagTask(taskTypeC, ProsessTaskStatus.KLAR);
        ProsessTaskGruppe eksisterendeGruppe = new ProsessTaskGruppe(vetoetTask);
        eksisterendeGruppe.addNesteSekvensiell(taskIGruppeMedVetoet);
        fagsakProsessTaskRepository.lagreNyGruppe(eksisterendeGruppe);
        FagsakProsessTask fagsakProsessTask1 = new FagsakProsessTask(fagsakId, behandlingId.toString(), vetoetTask.getId(), 222L, vetoetTask.getTaskType());
        fagsakProsessTaskRepository.lagre(fagsakProsessTask1);
        FagsakProsessTask fagsakProsessTask2 = new FagsakProsessTask(fagsakId, behandlingId.toString(), taskIGruppeMedVetoet.getId(), 333L, taskIGruppeMedVetoet.getTaskType());
        fagsakProsessTaskRepository.lagre(fagsakProsessTask2);

        ProsessTaskData nyTask = lagTask(taskTypeC, ProsessTaskStatus.KLAR);
        ProsessTaskGruppe nyGruppe = new ProsessTaskGruppe(nyTask);

        fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId, nyGruppe);
        assertThat(nyTask.getId()).isNull();
    }

    @Test
    void skalFeileNårViSkalOppretteToTasksOgViBareHarEnVetoetTask() {
        ProsessTaskData kjørendeTask = setupKjørendeTask(taskTypeA);

        ProsessTaskData vetoetTask = lagTask(taskTypeB, ProsessTaskStatus.VETO);
        vetoetTask.setBlokkertAvProsessTaskId(kjørendeTask.getId());
        fagsakProsessTaskRepository.lagreNyGruppe(vetoetTask);
        FagsakProsessTask fagsakProsessTask1 = new FagsakProsessTask(fagsakId, behandlingId.toString(), vetoetTask.getId(), 222L, vetoetTask.getTaskType());
        fagsakProsessTaskRepository.lagre(fagsakProsessTask1);

        ProsessTaskData nyTask1 = lagTask(taskTypeA, ProsessTaskStatus.KLAR);
        ProsessTaskData nyTask2 = lagTask(taskTypeB, ProsessTaskStatus.KLAR);
        ProsessTaskGruppe nyGruppe = new ProsessTaskGruppe(nyTask1);
        nyGruppe.addNesteSekvensiell(nyTask2);

        assertThrows(IllegalStateException.class, () ->
            fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId, nyGruppe));
    }

    @Test
    void skalIkkeLagreNyeTaskerNårViSkalOppretteToTasksOgViHarEnVetoetOgEnIGruppeMedVetoetAvSammeType() {
        ProsessTaskData kjørendeTask = setupKjørendeTask(taskTypeA);

        ProsessTaskData vetoetTask = lagTask(taskTypeA, ProsessTaskStatus.VETO);
        vetoetTask.setBlokkertAvProsessTaskId(kjørendeTask.getId());
        ProsessTaskData taskIGruppeMedVetoet = lagTask(taskTypeB, ProsessTaskStatus.KLAR);
        ProsessTaskGruppe eksisterendeGruppe = new ProsessTaskGruppe(vetoetTask);
        eksisterendeGruppe.addNesteSekvensiell(taskIGruppeMedVetoet);
        fagsakProsessTaskRepository.lagreNyGruppe(eksisterendeGruppe);
        FagsakProsessTask fagsakProsessTask1 = new FagsakProsessTask(fagsakId, behandlingId.toString(), vetoetTask.getId(), 222L, vetoetTask.getTaskType());
        fagsakProsessTaskRepository.lagre(fagsakProsessTask1);
        FagsakProsessTask fagsakProsessTask2 = new FagsakProsessTask(fagsakId, behandlingId.toString(), taskIGruppeMedVetoet.getId(), 333L, taskIGruppeMedVetoet.getTaskType());
        fagsakProsessTaskRepository.lagre(fagsakProsessTask2);

        ProsessTaskData nyTask1 = lagTask(taskTypeA, ProsessTaskStatus.KLAR);
        ProsessTaskData nyTask2 = lagTask(taskTypeB, ProsessTaskStatus.KLAR);
        ProsessTaskGruppe nyGruppe = new ProsessTaskGruppe(nyTask1);
        nyGruppe.addNesteSekvensiell(nyTask2);

        fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId, nyGruppe);
        assertThat(nyTask1.getId()).isNull();
        assertThat(nyTask2.getId()).isNull();
    }

    @Test
    void skalFeileNårViSkalOppretteToTasksOgViHarEnVetoetOgEnIGruppeMedVetoetMenSomHarEnAnnenType() {
        ProsessTaskData kjørendeTask = setupKjørendeTask(taskTypeA);

        ProsessTaskData vetoetTask = lagTask(taskTypeA, ProsessTaskStatus.VETO);
        vetoetTask.setBlokkertAvProsessTaskId(kjørendeTask.getId());
        ProsessTaskData taskIGruppeMedVetoet = lagTask(taskTypeC, ProsessTaskStatus.KLAR);
        ProsessTaskGruppe eksisterendeGruppe = new ProsessTaskGruppe(vetoetTask);
        eksisterendeGruppe.addNesteSekvensiell(taskIGruppeMedVetoet);
        fagsakProsessTaskRepository.lagreNyGruppe(eksisterendeGruppe);
        FagsakProsessTask fagsakProsessTask1 = new FagsakProsessTask(fagsakId, behandlingId.toString(), vetoetTask.getId(), 222L, vetoetTask.getTaskType());
        fagsakProsessTaskRepository.lagre(fagsakProsessTask1);
        FagsakProsessTask fagsakProsessTask2 = new FagsakProsessTask(fagsakId, behandlingId.toString(), taskIGruppeMedVetoet.getId(), 333L, taskIGruppeMedVetoet.getTaskType());
        fagsakProsessTaskRepository.lagre(fagsakProsessTask2);

        ProsessTaskData nyTask1 = lagTask(taskTypeA, ProsessTaskStatus.KLAR);
        ProsessTaskData nyTask2 = lagTask(taskTypeB, ProsessTaskStatus.KLAR);
        ProsessTaskGruppe nyGruppe = new ProsessTaskGruppe(nyTask1);
        nyGruppe.addNesteSekvensiell(nyTask2);

        assertThrows(IllegalStateException.class, () ->
            fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId, nyGruppe));
    }

    @Test
    void skalFeileNårViSkalOppretteTreTasksOgViKunHarEnVetoetTaskOgEnISammeGruppeSomVetoetAvSammeType() {
        ProsessTaskData kjørendeTask = setupKjørendeTask(taskTypeA);

        ProsessTaskData vetoetTask = lagTask(taskTypeA, ProsessTaskStatus.VETO);
        vetoetTask.setBlokkertAvProsessTaskId(kjørendeTask.getId());
        ProsessTaskData taskIGruppeMedVetoet = lagTask(taskTypeB, ProsessTaskStatus.KLAR);
        ProsessTaskGruppe eksisterendeGruppe = new ProsessTaskGruppe(vetoetTask);
        eksisterendeGruppe.addNesteSekvensiell(taskIGruppeMedVetoet);
        fagsakProsessTaskRepository.lagreNyGruppe(eksisterendeGruppe);
        FagsakProsessTask fagsakProsessTask1 = new FagsakProsessTask(fagsakId, behandlingId.toString(), vetoetTask.getId(), 222L, vetoetTask.getTaskType());
        fagsakProsessTaskRepository.lagre(fagsakProsessTask1);
        FagsakProsessTask fagsakProsessTask2 = new FagsakProsessTask(fagsakId, behandlingId.toString(), taskIGruppeMedVetoet.getId(), 333L, taskIGruppeMedVetoet.getTaskType());
        fagsakProsessTaskRepository.lagre(fagsakProsessTask2);

        ProsessTaskData nyTask1 = lagTask(taskTypeA, ProsessTaskStatus.KLAR);
        ProsessTaskData nyTask2 = lagTask(taskTypeB, ProsessTaskStatus.KLAR);
        ProsessTaskData nyTask3 = lagTask(taskTypeC, ProsessTaskStatus.KLAR);
        ProsessTaskGruppe nyGruppe = new ProsessTaskGruppe(nyTask1);
        nyGruppe.addNesteSekvensiell(nyTask2);
        nyGruppe.addNesteSekvensiell(nyTask3);

        assertThrows(IllegalStateException.class, () ->
            fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId, nyGruppe));
    }
}
