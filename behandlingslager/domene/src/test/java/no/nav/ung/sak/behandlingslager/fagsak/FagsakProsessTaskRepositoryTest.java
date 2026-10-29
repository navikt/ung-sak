package no.nav.ung.sak.behandlingslager.fagsak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskStatus;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.prosesstask.api.TaskType;
import no.nav.k9.prosesstask.impl.ProsessTaskEntitet;
import no.nav.k9.prosesstask.impl.ProsessTaskRepositoryImpl;
import no.nav.k9.prosesstask.impl.ProsessTaskTjenesteImpl;
import no.nav.k9.prosesstask.impl.TaskManager;
import no.nav.ung.sak.db.util.CdiDbAwareTest;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;

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

    @Test
    void skalFeileNårTaskPropertiesIVetoetTaskIkkeMatcherNyTask() {
        ProsessTaskData kjørendeTask = setupKjørendeTask(taskTypeA);

        ProsessTaskData vetoetTask = lagTask(taskTypeB, ProsessTaskStatus.VETO);
        vetoetTask.setBlokkertAvProsessTaskId(kjørendeTask.getId());
        vetoetTask.setProperty("type", "A");
        vetoetTask.setCallId("187623");
        fagsakProsessTaskRepository.lagreNyGruppe(vetoetTask);
        FagsakProsessTask fagsakProsessTask1 = new FagsakProsessTask(fagsakId, behandlingId.toString(), vetoetTask.getId(), 222L, vetoetTask.getTaskType());
        fagsakProsessTaskRepository.lagre(fagsakProsessTask1);

        ProsessTaskData nyTask = lagTask(taskTypeB, ProsessTaskStatus.KLAR);
        nyTask.setProperty("type", "B");
        ProsessTaskGruppe nyGruppe = new ProsessTaskGruppe(nyTask);
        assertThrows(IllegalStateException.class, () ->
            fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId, nyGruppe));
    }

    @Test
    void skalIkkeFeileFordiIgnorerteTaskPropertiesErForskjellig() {
        ProsessTaskData kjørendeTask = setupKjørendeTask(taskTypeA);

        ProsessTaskData vetoetTask = lagTask(taskTypeB, ProsessTaskStatus.VETO);
        vetoetTask.setBlokkertAvProsessTaskId(kjørendeTask.getId());
        vetoetTask.setCallId("187623");
        fagsakProsessTaskRepository.lagreNyGruppe(vetoetTask);
        FagsakProsessTask fagsakProsessTask1 = new FagsakProsessTask(fagsakId, behandlingId.toString(), vetoetTask.getId(), 222L, vetoetTask.getTaskType());
        fagsakProsessTaskRepository.lagre(fagsakProsessTask1);

        ProsessTaskData nyTask = lagTask(taskTypeB, ProsessTaskStatus.KLAR);
        ProsessTaskGruppe nyGruppe = new ProsessTaskGruppe(nyTask);
        fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId, nyGruppe);
        assertThat(nyTask.getId()).isNull();
    }

    @Test
    void skalIkkeFeileSelvOmFortsettBehandlingHarForskjelligSpesialPropertiesNårViHarVetoetDiffOgReposisjonerTask() {
        ProsessTaskData kjørendeTask = setupKjørendeTask(taskTypeA);

        ProsessTaskData vetoetTask = lagTask(new TaskType("grunnlag.diffOgReposisjoner"), ProsessTaskStatus.VETO);
        vetoetTask.setBlokkertAvProsessTaskId(kjørendeTask.getId());
        ProsessTaskData taskIGruppeMedVetoet = lagTask(new TaskType("behandlingskontroll.fortsettBehandling"), ProsessTaskStatus.KLAR);
        taskIGruppeMedVetoet.setProperty("manuellFortsettelse", "true");
        ProsessTaskGruppe eksisterendeGruppe = new ProsessTaskGruppe(vetoetTask);
        eksisterendeGruppe.addNesteSekvensiell(taskIGruppeMedVetoet);
        fagsakProsessTaskRepository.lagreNyGruppe(eksisterendeGruppe);
        FagsakProsessTask fagsakProsessTask1 = new FagsakProsessTask(fagsakId, behandlingId.toString(), vetoetTask.getId(), 222L, vetoetTask.getTaskType());
        fagsakProsessTaskRepository.lagre(fagsakProsessTask1);
        FagsakProsessTask fagsakProsessTask2 = new FagsakProsessTask(fagsakId, behandlingId.toString(), taskIGruppeMedVetoet.getId(), 222L, taskIGruppeMedVetoet.getTaskType());
        fagsakProsessTaskRepository.lagre(fagsakProsessTask2);

        ProsessTaskData nyTask = lagTask(new TaskType("behandlingskontroll.fortsettBehandling"), ProsessTaskStatus.KLAR);
        nyTask.setProperty("manuellFortsettelse", "false");
        nyTask.setProperty("gjenopptaSteg", "test");
        ProsessTaskGruppe nyGruppe = new ProsessTaskGruppe(nyTask);

        fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId, nyGruppe);
        assertThat(nyTask.getId()).isNull();
    }

    @Test
    void skalFeileNårFortsettBehandlingHarForskjelligSpesialPropertiesOgViIkkeHarVetoetDiffOgReposisjonerTask() {
        ProsessTaskData kjørendeTask = setupKjørendeTask(taskTypeA);

        ProsessTaskData vetoetTask = lagTask(new TaskType("grunnlag.diffOgReposisjoner.not"), ProsessTaskStatus.VETO);
        vetoetTask.setBlokkertAvProsessTaskId(kjørendeTask.getId());
        ProsessTaskData taskIGruppeMedVetoet = lagTask(new TaskType("behandlingskontroll.fortsettBehandling"), ProsessTaskStatus.KLAR);
        taskIGruppeMedVetoet.setProperty("manuellFortsettelse", "true");
        ProsessTaskGruppe eksisterendeGruppe = new ProsessTaskGruppe(vetoetTask);
        eksisterendeGruppe.addNesteSekvensiell(taskIGruppeMedVetoet);
        fagsakProsessTaskRepository.lagreNyGruppe(eksisterendeGruppe);
        FagsakProsessTask fagsakProsessTask1 = new FagsakProsessTask(fagsakId, behandlingId.toString(), vetoetTask.getId(), 222L, vetoetTask.getTaskType());
        fagsakProsessTaskRepository.lagre(fagsakProsessTask1);
        FagsakProsessTask fagsakProsessTask2 = new FagsakProsessTask(fagsakId, behandlingId.toString(), taskIGruppeMedVetoet.getId(), 222L, taskIGruppeMedVetoet.getTaskType());
        fagsakProsessTaskRepository.lagre(fagsakProsessTask2);

        ProsessTaskData nyTask = lagTask(new TaskType("behandlingskontroll.fortsettBehandling"), ProsessTaskStatus.KLAR);
        nyTask.setProperty("manuellFortsettelse", "false");
        nyTask.setProperty("gjenopptaSteg", "test");
        ProsessTaskGruppe nyGruppe = new ProsessTaskGruppe(nyTask);

        assertThrows(IllegalStateException.class, () ->
            fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId, nyGruppe));
    }

    @Test
    void skalFeileNårFortsettBehandlingHarForskjelligPropertiesSomIkkeErSpesialPropertiesSelvOmViHarVetoetDiffOgReposisjonerTask() {
        ProsessTaskData kjørendeTask = setupKjørendeTask(taskTypeA);

        ProsessTaskData vetoetTask = lagTask(new TaskType("grunnlag.diffOgReposisjoner"), ProsessTaskStatus.VETO);
        vetoetTask.setBlokkertAvProsessTaskId(kjørendeTask.getId());
        ProsessTaskData taskIGruppeMedVetoet = lagTask(new TaskType("behandlingskontroll.fortsettBehandling"), ProsessTaskStatus.KLAR);
        taskIGruppeMedVetoet.setProperty("manuellFortsettelse", "true");
        taskIGruppeMedVetoet.setProperty("enannenprop", "noe");
        ProsessTaskGruppe eksisterendeGruppe = new ProsessTaskGruppe(vetoetTask);
        eksisterendeGruppe.addNesteSekvensiell(taskIGruppeMedVetoet);
        fagsakProsessTaskRepository.lagreNyGruppe(eksisterendeGruppe);
        FagsakProsessTask fagsakProsessTask1 = new FagsakProsessTask(fagsakId, behandlingId.toString(), vetoetTask.getId(), 222L, vetoetTask.getTaskType());
        fagsakProsessTaskRepository.lagre(fagsakProsessTask1);
        FagsakProsessTask fagsakProsessTask2 = new FagsakProsessTask(fagsakId, behandlingId.toString(), taskIGruppeMedVetoet.getId(), 222L, taskIGruppeMedVetoet.getTaskType());
        fagsakProsessTaskRepository.lagre(fagsakProsessTask2);

        ProsessTaskData nyTask = lagTask(new TaskType("behandlingskontroll.fortsettBehandling"), ProsessTaskStatus.KLAR);
        nyTask.setProperty("manuellFortsettelse", "false");
        nyTask.setProperty("gjenopptaSteg", "test");
        nyTask.setProperty("enannenprop", "noe annet");
        ProsessTaskGruppe nyGruppe = new ProsessTaskGruppe(nyTask);

        assertThrows(IllegalStateException.class, () ->
            fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId, nyGruppe));
    }

    @Test
    void ryddProsessTasks_skal_fjerne_koblinger_og_frigi_tasker_blokkert_av_slettede_tasker() {
        ProsessTaskData blokkerende = lagTask(taskTypeA, ProsessTaskStatus.KLAR);
        fagsakProsessTaskRepository.lagreNyGruppe(blokkerende);
        fagsakProsessTaskRepository.lagre(new FagsakProsessTask(fagsakId, behandlingId.toString(), blokkerende.getId(), 100L, blokkerende.getTaskType()));

        // Task på annen behandling i samme fagsak, blokkert av tasken som slettes
        Long annenBehandlingId = behandlingId + 1;
        ProsessTaskData vetoet = ProsessTaskData.forTaskType(taskTypeB);
        vetoet.setBehandling(fagsakId, annenBehandlingId);
        vetoet.setStatus(ProsessTaskStatus.VETO);
        vetoet.setBlokkertAvProsessTaskId(blokkerende.getId());
        fagsakProsessTaskRepository.lagreNyGruppe(vetoet);
        fagsakProsessTaskRepository.lagre(new FagsakProsessTask(fagsakId, annenBehandlingId.toString(), vetoet.getId(), 200L, vetoet.getTaskType()));
        entityManager.flush();

        fagsakProsessTaskRepository.ryddProsessTasks(fagsakId, behandlingId);
        entityManager.flush();
        entityManager.clear();

        assertThat(fagsakProsessTaskRepository.hent(blokkerende.getId(), false)).isEmpty();
        assertThat(entityManager.find(ProsessTaskEntitet.class, blokkerende.getId())).isNull();
        assertThat(fagsakProsessTaskRepository.hent(vetoet.getId(), false)).isPresent();
        ProsessTaskEntitet frigitt = entityManager.find(ProsessTaskEntitet.class, vetoet.getId());
        assertThat(frigitt.getStatus()).isEqualTo(ProsessTaskStatus.KLAR);
        assertThat(frigitt.getBlokkertAvProsessTaskId()).isNull();
    }

    @Test
    void hentKoblinger_skal_inkludere_koblinger_uten_prosesstask() {
        ProsessTaskData task = lagTask(taskTypeA, ProsessTaskStatus.KLAR);
        fagsakProsessTaskRepository.lagreNyGruppe(task);
        fagsakProsessTaskRepository.lagre(new FagsakProsessTask(fagsakId, behandlingId.toString(), task.getId(), 200L, task.getTaskType()));
        Long slettetTaskId = lagKoblingUtenProsessTask(100L);
        entityManager.flush();

        var koblinger = fagsakProsessTaskRepository.hentKoblinger(fagsakId);

        assertThat(koblinger).extracting(FagsakProsessTaskKobling::prosessTaskId).containsExactly(slettetTaskId, task.getId());
        var utenProsessTask = koblinger.get(0);
        assertThat(utenProsessTask.prosessTaskMangler()).isTrue();
        assertThat(utenProsessTask.prosessTaskStatus()).isNull();
        assertThat(utenProsessTask.gruppeSekvensNr()).isEqualTo(100L);
        assertThat(utenProsessTask.behandlingId()).isEqualTo(behandlingId);
        assertThat(utenProsessTask.opprettetTid()).isNotNull();
        var vanlig = koblinger.get(1);
        assertThat(vanlig.prosessTaskMangler()).isFalse();
        assertThat(vanlig.prosessTaskStatus()).isEqualTo(ProsessTaskStatus.KLAR);
        assertThat(fagsakProsessTaskRepository.prosessTaskFinnes(task.getId())).isTrue();
        assertThat(fagsakProsessTaskRepository.prosessTaskFinnes(slettetTaskId)).isFalse();
    }

    @Test
    void fjernKoblingerUtenProsessTask_skal_fjerne_kobling_og_frigi_vetoede_tasker() {
        Long slettetTaskId = lagKoblingUtenProsessTask(100L);
        ProsessTaskData vetoet = lagTask(taskTypeB, ProsessTaskStatus.VETO);
        vetoet.setBlokkertAvProsessTaskId(slettetTaskId);
        fagsakProsessTaskRepository.lagreNyGruppe(vetoet);
        fagsakProsessTaskRepository.lagre(new FagsakProsessTask(fagsakId, behandlingId.toString(), vetoet.getId(), 200L, vetoet.getTaskType()));
        entityManager.flush();

        int frigitt = fagsakProsessTaskRepository.fjernKoblingerUtenProsessTask(fagsakId, Set.of(slettetTaskId));
        entityManager.clear();

        assertThat(frigitt).isEqualTo(1);
        var koblinger = fagsakProsessTaskRepository.hentKoblinger(fagsakId);
        assertThat(koblinger).extracting(FagsakProsessTaskKobling::prosessTaskId).containsExactly(vetoet.getId());
        assertThat(koblinger.getFirst().prosessTaskStatus()).isEqualTo(ProsessTaskStatus.KLAR);
        assertThat(koblinger.getFirst().blokkertAv()).isNull();
    }

    @Test
    void fjernKoblingerUtenProsessTask_skal_ikke_fjerne_kobling_til_task_som_finnes() {
        ProsessTaskData task = lagTask(taskTypeA, ProsessTaskStatus.KLAR);
        fagsakProsessTaskRepository.lagreNyGruppe(task);
        fagsakProsessTaskRepository.lagre(new FagsakProsessTask(fagsakId, behandlingId.toString(), task.getId(), 100L, task.getTaskType()));
        Long slettetTaskId = lagKoblingUtenProsessTask(200L);
        entityManager.flush();

        assertThrows(IllegalStateException.class, () ->
            fagsakProsessTaskRepository.fjernKoblingerUtenProsessTask(fagsakId, Set.of(task.getId(), slettetTaskId)));
    }

    @Test
    void fjernKoblingerUtenProsessTask_skal_ikke_fjerne_kobling_på_annen_fagsak() {
        Long slettetTaskId = lagKoblingUtenProsessTask(100L);
        Fagsak annenFagsak = Fagsak.opprettNy(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, AktørId.dummy(), mock(Saksnummer.class), LocalDate.now(), null);
        Long annenFagsakId = fagsakRepository.opprettNy(annenFagsak);
        entityManager.flush();

        assertThrows(IllegalStateException.class, () ->
            fagsakProsessTaskRepository.fjernKoblingerUtenProsessTask(annenFagsakId, Set.of(slettetTaskId)));
        assertThat(fagsakProsessTaskRepository.hentKoblinger(fagsakId)).extracting(FagsakProsessTaskKobling::prosessTaskId).containsExactly(slettetTaskId);
    }

    @Test
    void finnFagsakerMedKoblingerUtenProsessTask_skal_kun_finne_fagsaker_med_koblinger_uten_prosesstask() {
        lagKoblingUtenProsessTask(100L);
        lagKoblingUtenProsessTask(null);
        Fagsak annenFagsak = Fagsak.opprettNy(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, AktørId.dummy(), mock(Saksnummer.class), LocalDate.now(), null);
        Long annenFagsakId = fagsakRepository.opprettNy(annenFagsak);
        ProsessTaskData task = lagTask(taskTypeA, ProsessTaskStatus.KLAR);
        task.setBehandling(annenFagsakId, behandlingId);
        fagsakProsessTaskRepository.lagreNyGruppe(task);
        fagsakProsessTaskRepository.lagre(new FagsakProsessTask(annenFagsakId, behandlingId.toString(), task.getId(), 100L, task.getTaskType()));
        entityManager.flush();

        var fagsakIder = fagsakProsessTaskRepository.finnFagsakerMedKoblingerUtenProsessTask();

        assertThat(fagsakIder).containsOnlyOnce(fagsakId).doesNotContain(annenFagsakId);
    }

    /** Kobling til en prosesstask som ikke lenger finnes, slik ryddProsessTasks har etterlatt i prod. */
    private Long lagKoblingUtenProsessTask(Long gruppeSekvensNr) {
        ProsessTaskData task = lagTask(taskTypeC, ProsessTaskStatus.KLAR);
        fagsakProsessTaskRepository.lagreNyGruppe(task);
        Long prosessTaskId = task.getId();
        fagsakProsessTaskRepository.lagre(new FagsakProsessTask(fagsakId, behandlingId.toString(), prosessTaskId, gruppeSekvensNr, task.getTaskType()));
        entityManager.flush();
        entityManager.clear();
        entityManager.createNativeQuery("delete from prosess_task where id = :id").setParameter("id", prosessTaskId).executeUpdate();
        return prosessTaskId;
    }
}
