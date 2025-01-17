package no.nav.ung.domenetjenester.personhendelser;


import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.person.pdl.leesah.Endringstype;
import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.person.pdl.leesah.doedsfall.Doedsfall;
import no.nav.ung.fordel.repo.hendelser.HendelseRepository;
import no.nav.ung.fordel.repo.hendelser.InngåendeHendelseEntitet;
import no.nav.ung.sak.kontrakt.hendelser.Hendelse;
import no.nav.ung.sak.typer.AktørId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdlLeesahHendelseHåndtererTest {

    private static final String AKTØR_ID = "1234567890123";

    private final ProsessTaskTjeneste prosessTaskTjeneste = mock(ProsessTaskTjeneste.class);
    private final HendelseRepository hendelseRepository = mock(HendelseRepository.class);
    private final ForsinkelseTjeneste forsinkelseTjeneste = mock(ForsinkelseTjeneste.class);
    private final PdlLeesahHendelseFiltrerer hendelseFiltrerer = mock(PdlLeesahHendelseFiltrerer.class);

    private PdlLeesahHendelseHåndterer pdlLeesahHendelseHåndterer;

    @BeforeEach
    public void beforeEach() {
        var oversetter = new PdlLeesahOversetter();

        this.pdlLeesahHendelseHåndterer = new PdlLeesahHendelseHåndterer(oversetter, prosessTaskTjeneste);
    }

    @Test
    void skal_lagre_inngående_hendelse() {
        // Arrange
        Personhendelse personhendelse = byggPersonhendelse("DOEDSFALL_V1");

        when(hendelseFiltrerer.finnAktørerMedPåvirketUngFagsak(any(Hendelse.class)))
                .thenReturn(List.of(new AktørId(AKTØR_ID)));
        when(forsinkelseTjeneste.finnTidspunktForInnsendingAvHendelse()).thenReturn(LocalDateTime.now());

        // Act
        pdlLeesahHendelseHåndterer.handleMessage(personhendelse);

        // Assert
        var argumentCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste).lagre(argumentCaptor.capture());
        var taskData = argumentCaptor.getValue();
        assertThat(taskData.getTaskType()).isEqualTo(HåndterUngSakHendelseTask.TASKNAME);

        var task = new HåndterUngSakHendelseTask(prosessTaskTjeneste, hendelseRepository, forsinkelseTjeneste, hendelseFiltrerer);
        task.doTask(taskData);
        verify(hendelseRepository).lagreInngåendeHendelse(any(InngåendeHendelseEntitet.class));

        var argumentCaptor2 = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste, times(2)).lagre(argumentCaptor2.capture());
        var taskData2 = argumentCaptor2.getAllValues().get(1);
        assertThat(taskData2.getTaskType()).isEqualTo(SendInnUngHendelseTask.TASKNAME);
    }

    private Personhendelse byggPersonhendelse(String opplysningType) {
        var personhendelse = new Personhendelse();
        personhendelse.setOpprettet(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        personhendelse.setHendelseId("123");
        personhendelse.setOpplysningstype(opplysningType);
        personhendelse.setEndringstype(Endringstype.OPPRETTET);
        personhendelse.setPersonidenter(List.of(AKTØR_ID));
        var doedsfall = new Doedsfall();
        doedsfall.setDoedsdato(LocalDate.now());
        personhendelse.setDoedsfall(doedsfall);
        return personhendelse;
    }
}
