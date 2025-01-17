package no.nav.ung.domenetjenester.personhendelser;

import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.fordel.repo.hendelser.HendelseRepository;
import no.nav.ung.fordel.repo.hendelser.InngåendeHendelseEntitet;
import no.nav.ung.sak.hendelsemottak.tjenester.HendelsemottakTjeneste;
import no.nav.ung.sak.kontrakt.hendelser.DødsfallHendelse;
import no.nav.ung.sak.kontrakt.hendelser.Hendelse;
import no.nav.ung.sak.kontrakt.hendelser.HendelseInfo;
import no.nav.ung.sak.typer.AktørId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

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
    void skal_sende_hendelse_til_ung_sak_og_oppdatere_håndtert_status() {
        // Arrange
        Long inngåendeHendelseId = 1L;
        String aktørId = "1234567890123";

        var prosessTaskData = ProsessTaskData.forProsessTask(SendInnUngHendelseTask.class);
        prosessTaskData.setProperty(SendInnUngHendelseTask.INNGÅENDE_HENDELSE_ID, "" + inngåendeHendelseId);

        InngåendeHendelseEntitet inngåendeHendelse = byggInngåendeHendelse(inngåendeHendelseId, aktørId);
        when(hendelseRepository.finnEksaktHendelse(any())).thenReturn(inngåendeHendelse);
        when(hendelseRepository.finnUhåndterteHendelser(any())).thenReturn(List.of(inngåendeHendelse));

        // Act
        sendInnHendelseTask.doTask(prosessTaskData);

        // Assert
        verify(hendelsemottakTjeneste).mottaHendelse(any(Hendelse.class));
        verify(hendelseRepository).oppdaterHåndtertStatus(any(), any(), any());
    }


    private InngåendeHendelseEntitet byggInngåendeHendelse(Long inngåendeHendelseId, String aktørId) {
        // Hendelse lagres som json-payload i InngåendeHendelseEntitet
        DødsfallHendelse hendelse = new DødsfallHendelse.Builder()
                .medHendelseInfo(new HendelseInfo.Builder()
                        .medHendelseId("123")
                        .leggTilAktør(new AktørId(aktørId))
                        .build())
                .medDødsdato(LocalDate.now())
                .build();
        var hendelseInfo = hendelse.getHendelseInfo();
        var payload = HåndterUngSakHendelseTask.toJson(hendelse);

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
