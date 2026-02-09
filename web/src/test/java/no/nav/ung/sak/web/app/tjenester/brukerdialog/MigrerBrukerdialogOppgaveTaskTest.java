package no.nav.ung.sak.web.app.tjenester.brukerdialog;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.kontrakt.oppgaver.MigrerOppgaveDto;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.typer.inntektsrapportering.InntektsrapporteringOppgavetypeDataDTO;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveRepository;
import no.nav.ung.sak.typer.AktørId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class MigrerBrukerdialogOppgaveTaskTest {

    @Inject
    private EntityManager entityManager;

    private BrukerdialogOppgaveRepository repository;
    private MigrerBrukerdialogOppgaveTask task;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        repository = new BrukerdialogOppgaveRepository(entityManager);
        task = new MigrerBrukerdialogOppgaveTask(repository);
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    void skal_migrere_ny_oppgave() throws Exception {
        // Arrange
        UUID oppgaveReferanse = UUID.randomUUID();
        AktørId aktørId = new AktørId("1234567890123");

        var oppgaveData = new InntektsrapporteringOppgavetypeDataDTO(
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            null,
            false
        );

        var migrerOppgaveDto = new MigrerOppgaveDto(
            oppgaveReferanse,
            aktørId,
            OppgaveType.RAPPORTER_INNTEKT,
            oppgaveData,
            null, // bekreftelse
            no.nav.ung.sak.kontrakt.oppgaver.OppgaveStatus.ULØST, // status
            ZonedDateTime.now(), // opprettetDato
            null, // løstDato
            null, // åpnetDato
            null, // lukketDato
            ZonedDateTime.now().plusDays(7) // frist
        );

        ProsessTaskData taskData = ProsessTaskData.forProsessTask(MigrerBrukerdialogOppgaveTask.class);
        taskData.setProperty(
            MigrerBrukerdialogOppgaveTask.OPPGAVE_DATA,
            objectMapper.writeValueAsString(migrerOppgaveDto)
        );

        // Act
        task.doTask(taskData);

        // Assert
        var lagretOppgave = repository.hentOppgaveForOppgavereferanse(oppgaveReferanse);
        assertThat(lagretOppgave).isPresent();
        assertThat(lagretOppgave.get().getOppgavereferanse()).isEqualTo(oppgaveReferanse);
        assertThat(lagretOppgave.get().getAktørId()).isEqualTo(aktørId);
        assertThat(lagretOppgave.get().getOppgaveType()).isEqualTo(OppgaveType.RAPPORTER_INNTEKT);
    }

    @Test
    void skal_hoppe_over_eksisterende_oppgave() throws Exception {
        // Arrange
        UUID oppgaveReferanse = UUID.randomUUID();
        AktørId aktørId = new AktørId("1234567890123");

        var oppgaveData = new InntektsrapporteringOppgavetypeDataDTO(
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            null,
            false
        );

        var migrerOppgaveDto = new MigrerOppgaveDto(
            oppgaveReferanse,
            aktørId,
            OppgaveType.RAPPORTER_INNTEKT,
            oppgaveData,
            null, // bekreftelse
            no.nav.ung.sak.kontrakt.oppgaver.OppgaveStatus.ULØST, // status
            ZonedDateTime.now(), // opprettetDato
            null, // løstDato
            null, // åpnetDato
            null, // lukketDato
            ZonedDateTime.now().plusDays(7) // frist
        );

        ProsessTaskData taskData = ProsessTaskData.forProsessTask(MigrerBrukerdialogOppgaveTask.class);
        taskData.setProperty(
            MigrerBrukerdialogOppgaveTask.OPPGAVE_DATA,
            objectMapper.writeValueAsString(migrerOppgaveDto)
        );

        // Act - Kjør task to ganger
        task.doTask(taskData);
        task.doTask(taskData);

        // Assert - Skal kun finnes én oppgave
        var alleOppgaver = repository.hentAlleOppgaverForAktør(aktørId);
        assertThat(alleOppgaver).hasSize(1);
    }
}

