package no.nav.ung.domenetjenester.personhendelser;


import no.nav.k9.felles.integrasjon.pdl.ForelderBarnRelasjonRolle;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.ung.domenetjenester.personhendelser.utils.PersonhendelseTestUtils;
import no.nav.ung.fordel.repo.hendelser.HendelseRepository;
import no.nav.ung.fordel.repo.hendelser.InngåendeHendelseEntitet;
import no.nav.ung.sak.kontrakt.hendelser.FødselsHendelse;
import no.nav.ung.sak.kontrakt.hendelser.Hendelse;
import no.nav.ung.sak.kontrakt.hendelser.HendelseInfo;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdlLeesahHendelseHåndtererTest {

    private static final String AKTØR_ID = "1234567890123";
    private static final String RELATERT_PERSON_IDENT = "10987654321";
    private static final String RELATERT_PERSON_AKTØRID = "12465013798042";

    private final ProsessTaskTjeneste prosessTaskTjeneste = mock(ProsessTaskTjeneste.class);
    private final HendelseRepository hendelseRepository = mock(HendelseRepository.class);
    private final ForsinkelseTjeneste forsinkelseTjeneste = mock(ForsinkelseTjeneste.class);
    private final PdlLeesahHendelseFiltrerer hendelseFiltrerer = mock(PdlLeesahHendelseFiltrerer.class);
    private final PdlLeesahOversetter oversetter = mock(PdlLeesahOversetter.class);

    private PdlLeesahHendelseHåndterer pdlLeesahHendelseHåndterer;

    @BeforeEach
    public void beforeEach() {
        this.pdlLeesahHendelseHåndterer = new PdlLeesahHendelseHåndterer(prosessTaskTjeneste);
    }

    @Test
    void skal_lagre_inngående_hendelse() {
        // Arrange
        Personhendelse personhendelse = PersonhendelseTestUtils.byggDødsfallHendelse(List.of(AKTØR_ID));

        when(hendelseFiltrerer.finnAktørerMedPåvirketUngFagsak(any(Hendelse.class)))
            .thenReturn(List.of(new AktørId(AKTØR_ID)));
        when(forsinkelseTjeneste.finnTidspunktForInnsendingAvHendelse()).thenReturn(LocalDateTime.now());

        when(oversetter.oversettStøttetPersonhendelse(personhendelse)).thenCallRealMethod();

        // Act
        pdlLeesahHendelseHåndterer.håndterHendelse(UUID.randomUUID().toString(), personhendelse);

        // Assert
        var taskData = captureAndVerifyTaskData(1);
        assertThat(taskData.getTaskType()).isEqualTo(HåndterPdlHendelseTask.TASKNAME);

        var task = new HåndterPdlHendelseTask(prosessTaskTjeneste, hendelseRepository, forsinkelseTjeneste, hendelseFiltrerer, oversetter);
        task.doTask(taskData);
        verify(hendelseRepository).lagreInngåendeHendelse(any(InngåendeHendelseEntitet.class));

        var taskData2 = captureAndVerifyTaskData(2);
        assertThat(taskData2.getTaskType()).isEqualTo(SendInnUngHendelseTask.TASKNAME);
    }

    @Test
    void skal_lagre_forelder_barn_relasjon_hendelse() {
        // Arrange
        Personhendelse personhendelse = PersonhendelseTestUtils.byggForelderBarnRelasjonHendelse(List.of(AKTØR_ID), RELATERT_PERSON_IDENT, ForelderBarnRelasjonRolle.BARN, ForelderBarnRelasjonRolle.MOR);

        when(hendelseFiltrerer.finnAktørerMedPåvirketUngFagsak(any(Hendelse.class)))
            .thenReturn(List.of(new AktørId(AKTØR_ID)));
        when(forsinkelseTjeneste.finnTidspunktForInnsendingAvHendelse()).thenReturn(LocalDateTime.now());

        when(oversetter.oversettStøttetPersonhendelse(personhendelse)).thenReturn(byggFødselshendelse(personhendelse, LocalDate.now()));

        // Act
        pdlLeesahHendelseHåndterer.håndterHendelse(UUID.randomUUID().toString(), personhendelse);

        // Assert
        var taskData = captureAndVerifyTaskData(1);
        assertThat(taskData.getTaskType()).isEqualTo(HåndterPdlHendelseTask.TASKNAME);

        var task = new HåndterPdlHendelseTask(prosessTaskTjeneste, hendelseRepository, forsinkelseTjeneste, hendelseFiltrerer, oversetter);
        task.doTask(taskData);
        verify(hendelseRepository).lagreInngåendeHendelse(any(InngåendeHendelseEntitet.class));

        var taskData2 = captureAndVerifyTaskData(2);
        assertThat(taskData2.getTaskType()).isEqualTo(SendInnUngHendelseTask.TASKNAME);
    }

    private static Optional<Hendelse> byggFødselshendelse(Personhendelse personhendelse, LocalDate fødselsdato) {
        return Optional.of(
            new FødselsHendelse.Builder()
                .medBarnIdent(new PersonIdent(personhendelse.getForelderBarnRelasjon().getRelatertPersonsIdent().toString()))
                .medFødselsdato(fødselsdato)
                .medHendelseInfo(new HendelseInfo.Builder()
                    .medHendelseId(personhendelse.getHendelseId().toString())
                    .medOpprettet(LocalDateTime.now())
                    .leggTilAktør(new AktørId(personhendelse.getPersonidenter().getFirst().toString()))
                    .build()
                ).build()
        );
    }

    private ProsessTaskData captureAndVerifyTaskData(int numberOfInvocations) {
        var argumentCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste, times(numberOfInvocations)).lagre(argumentCaptor.capture());
        return argumentCaptor.getValue();
    }
}
