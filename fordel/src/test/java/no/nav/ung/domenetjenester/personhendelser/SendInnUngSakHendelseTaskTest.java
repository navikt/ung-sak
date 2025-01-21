package no.nav.ung.domenetjenester.personhendelser;

import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.fordel.repo.hendelser.HendelseRepository;
import no.nav.ung.fordel.repo.hendelser.InngåendeHendelseEntitet;
import no.nav.ung.sak.hendelsemottak.tjenester.HendelsemottakTjeneste;
import no.nav.ung.sak.kontrakt.hendelser.DødsfallHendelse;
import no.nav.ung.sak.kontrakt.hendelser.FødselHendelse;
import no.nav.ung.sak.kontrakt.hendelser.Hendelse;
import no.nav.ung.sak.kontrakt.hendelser.HendelseInfo;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static no.nav.ung.domenetjenester.personhendelser.HendelseMapper.toJson;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendInnUngSakHendelseTaskTest {

    private final HendelseRepository hendelseRepository = mock(HendelseRepository.class);
    private final HendelsemottakTjeneste hendelsemottakTjeneste = mock(HendelsemottakTjeneste.class);

    private SendInnUngHendelseTask sendInnHendelseTask;

    @BeforeEach
    public void beforeEach() {
        this.sendInnHendelseTask = new SendInnUngHendelseTask(hendelseRepository, hendelsemottakTjeneste);
    }

    @Test
    void skal_sende_dødsfallhendelse_til_ung_sak_og_oppdatere_håndtert_status() {
        // Arrange
        Long inngåendeHendelseId = 1L;
        String aktørId = "1234567890123";

        var prosessTaskData = ProsessTaskData.forProsessTask(SendInnUngHendelseTask.class);
        prosessTaskData.setProperty(SendInnUngHendelseTask.INNGÅENDE_HENDELSE_ID, "" + inngåendeHendelseId);

        DødsfallHendelse dødsfallHendelse = new DødsfallHendelse.Builder()
            .medHendelseInfo(new HendelseInfo.Builder()
                .medHendelseId("123")
                .leggTilAktør(new AktørId(aktørId))
                .build())
            .medDødsdato(LocalDate.now())
            .build();

        InngåendeHendelseEntitet inngåendeHendelse = byggInngåendeHendelse(inngåendeHendelseId, dødsfallHendelse);
        when(hendelseRepository.finnEksaktHendelse(any())).thenReturn(inngåendeHendelse);
        when(hendelseRepository.finnUhåndterteHendelser(any())).thenReturn(List.of(inngåendeHendelse));

        // Act
        sendInnHendelseTask.doTask(prosessTaskData);

        // Assert
        verify(hendelsemottakTjeneste).mottaHendelse(any(Hendelse.class));
        verify(hendelseRepository).oppdaterHåndtertStatus(any(), any(), any());
    }

    @Test
    void skal_sende_fødselshendelse_til_ung_sak_og_oppdatere_håndtert_status() {
        // Arrange
        Long inngåendeHendelseId = 1L;
        String aktørId = "1234567890123";
        String barnIdent = "32109876543";

        var prosessTaskData = ProsessTaskData.forProsessTask(SendInnUngHendelseTask.class);
        prosessTaskData.setProperty(SendInnUngHendelseTask.INNGÅENDE_HENDELSE_ID, "" + inngåendeHendelseId);

        FødselHendelse dødsfallHendelse = new FødselHendelse.Builder()
            .medHendelseInfo(new HendelseInfo.Builder()
                .medHendelseId("123")
                .leggTilAktør(new AktørId(aktørId))
                .build())
            .medBarnIdent(PersonIdent.fra(barnIdent))
            .medFødselsdato(LocalDate.now())
            .build();

        InngåendeHendelseEntitet inngåendeHendelse = byggInngåendeHendelse(inngåendeHendelseId, dødsfallHendelse);
        when(hendelseRepository.finnEksaktHendelse(any())).thenReturn(inngåendeHendelse);
        when(hendelseRepository.finnUhåndterteHendelser(any())).thenReturn(List.of(inngåendeHendelse));

        // Act
        sendInnHendelseTask.doTask(prosessTaskData);

        // Assert
        verify(hendelsemottakTjeneste).mottaHendelse(any(Hendelse.class));
        verify(hendelseRepository).oppdaterHåndtertStatus(any(), any(), any());
    }


    private InngåendeHendelseEntitet byggInngåendeHendelse(Long inngåendeHendelseId, Hendelse hendelse) {

        // Hendelse lagres som json-payload i InngåendeHendelseEntitet
        var hendelseInfo = hendelse.getHendelseInfo();
        var payload = toJson(hendelse);

        InngåendeHendelseEntitet inngåendeHendelse = InngåendeHendelseEntitet.builder()
                .id(inngåendeHendelseId)
                .aktørId(hendelseInfo.getAktørIder().iterator().next().getAktørId())
                .hendelseType(hendelse.getHendelseType())
                .hendelseId(hendelseInfo.getHendelseId())
                .meldingOpprettet(hendelseInfo.getOpprettet())
                .payload(payload)
                .håndtertStatus(InngåendeHendelseEntitet.HåndtertStatusType.MOTTATT)
                .build();
        return inngåendeHendelse;
    }
}
