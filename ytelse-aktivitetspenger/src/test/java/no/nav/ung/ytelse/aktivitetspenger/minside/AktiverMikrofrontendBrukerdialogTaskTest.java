package no.nav.ung.ytelse.aktivitetspenger.minside;

import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.brukerdialog.kontrakt.sak.mikrofrontend.AktiverMikrofrontendRequest;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.domene.vedtak.brukerdialog.UngBrukerdialogSakKlient;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AktiverMikrofrontendBrukerdialogTaskTest {

    private static final Long FAGSAK_ID = 1001L;
    private static final AktørId AKTØR_ID = new AktørId("1234567890123");
    private static final Saksnummer SAKSNUMMER = new Saksnummer("ABC123");

    @Mock
    private FagsakRepository fagsakRepository;
    @Mock
    private UngBrukerdialogSakKlient klient;

    @Test
    void senderAktørIdOgSaksnummerFraFagsakenTilBrukerdialog() {
        // Arrange
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.AKTIVITETSPENGER, AKTØR_ID, SAKSNUMMER, LocalDate.of(2026, 1, 1), null);
        fagsak.setId(FAGSAK_ID);
        when(fagsakRepository.finnEksaktFagsak(FAGSAK_ID)).thenReturn(fagsak);

        var prosessTaskData = ProsessTaskData.forProsessTask(AktiverMikrofrontendBrukerdialogTask.class);
        prosessTaskData.setFagsak(FAGSAK_ID, AKTØR_ID.getId());

        var task = new AktiverMikrofrontendBrukerdialogTask(fagsakRepository, klient);

        // Act
        task.doTask(prosessTaskData);

        // Assert
        var captor = ArgumentCaptor.forClass(AktiverMikrofrontendRequest.class);
        verify(klient).aktiverMikrofrontend(captor.capture());
        var request = captor.getValue();
        assertThat(request.aktørId().getId()).isEqualTo(AKTØR_ID.getId());
        assertThat(request.saksnummer().getVerdi()).isEqualTo(SAKSNUMMER.getVerdi());
    }
}
