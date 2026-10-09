package no.nav.ung.ytelse.aktivitetspenger.minside;

import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.FagsakStatusEvent;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AktivitetspengerMikrofrontendObserverTest {

    private static final Long FAGSAK_ID = 1001L;
    private static final AktørId AKTØR_ID = new AktørId("1234567890123");
    private static final Saksnummer SAKSNUMMER = new Saksnummer("ABC123");
    private static final LocalDate FOM = LocalDate.of(2026, 1, 1);

    @Mock
    private ProsessTaskTjeneste prosessTaskTjeneste;
    @Mock
    private FagsakRepository fagsakRepository;

    @Test
    void oppretterTaskForNyAktivitetspengerFagsakForDigitalBruker() {
        // Arrange
        when(fagsakRepository.finnEksaktFagsak(FAGSAK_ID)).thenReturn(lagFagsak(false));
        var observer = lagObserver(true);

        // Act
        observer.observerFagsakOpprettet(nyFagsakEvent(FagsakYtelseType.AKTIVITETSPENGER));

        // Assert
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste).lagre(captor.capture());
        var task = captor.getValue();
        assertThat(task.getTaskType()).isEqualTo(AktiverMikrofrontendBrukerdialogTask.TASKTYPE);
        assertThat(task.getFagsakId()).isEqualTo(FAGSAK_ID);
        assertThat(task.getAktørId()).isEqualTo(AKTØR_ID.getId());
    }

    @Test
    void oppretterIkkeTaskForUngdomsytelse() {
        // Arrange
        var observer = lagObserver(true);

        // Act
        observer.observerFagsakOpprettet(nyFagsakEvent(FagsakYtelseType.UNGDOMSYTELSE));

        // Assert
        verifyNoInteractions(prosessTaskTjeneste, fagsakRepository);
    }

    @Test
    void oppretterIkkeTaskForIkkeDigitalBruker() {
        // Arrange
        when(fagsakRepository.finnEksaktFagsak(FAGSAK_ID)).thenReturn(lagFagsak(true));
        var observer = lagObserver(true);

        // Act
        observer.observerFagsakOpprettet(nyFagsakEvent(FagsakYtelseType.AKTIVITETSPENGER));

        // Assert
        verifyNoInteractions(prosessTaskTjeneste);
    }

    @Test
    void oppretterIkkeTaskVedAndreStatusoverganger() {
        // Arrange
        var observer = lagObserver(true);
        var event = new FagsakStatusEvent(FAGSAK_ID, AKTØR_ID, FagsakYtelseType.AKTIVITETSPENGER,
            FagsakStatus.OPPRETTET, FagsakStatus.UNDER_BEHANDLING);

        // Act
        observer.observerFagsakOpprettet(event);

        // Assert
        verifyNoInteractions(prosessTaskTjeneste, fagsakRepository);
    }

    @Test
    void oppretterIkkeTaskNårToggleErAv() {
        // Arrange
        var observer = lagObserver(false);

        // Act
        observer.observerFagsakOpprettet(nyFagsakEvent(FagsakYtelseType.AKTIVITETSPENGER));

        // Assert
        verifyNoInteractions(prosessTaskTjeneste, fagsakRepository);
    }

    private AktivitetspengerMikrofrontendObserver lagObserver(boolean enabled) {
        return new AktivitetspengerMikrofrontendObserver(prosessTaskTjeneste, fagsakRepository, enabled);
    }

    private static FagsakStatusEvent nyFagsakEvent(FagsakYtelseType ytelseType) {
        return new FagsakStatusEvent(FAGSAK_ID, AKTØR_ID, ytelseType, null, FagsakStatus.OPPRETTET);
    }

    private static Fagsak lagFagsak(boolean ikkeDigitalBruker) {
        var fagsak = ikkeDigitalBruker
            ? Fagsak.opprettNyForIkkeDigitalBruker(FagsakYtelseType.AKTIVITETSPENGER, AKTØR_ID, SAKSNUMMER, FOM, null)
            : Fagsak.opprettNy(FagsakYtelseType.AKTIVITETSPENGER, AKTØR_ID, SAKSNUMMER, FOM, null);
        fagsak.setId(FAGSAK_ID);
        return fagsak;
    }
}
