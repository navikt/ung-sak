package no.nav.ung.sak.etterlysning;

import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.ung.brukerdialog.kontrakt.AktørIdDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UngBrukerdialogOppgaveKlientTest {

    private OidcRestClient restClient;
    private UngBrukerdialogOppgaveKlient klient;

    @BeforeEach
    void setUp() {
        restClient = mock(OidcRestClient.class);
        klient = new UngBrukerdialogOppgaveKlient(restClient, "http://localhost:8080");
    }

    @Test
    void løsSøkYtelseOppgave_senderAktørIdSomObjektMedFelt() throws Exception {
        // Arrange
        var aktørId = new AktørIdDto("1234567890123");

        // Act
        klient.løsSøkYtelseOppgave(aktørId);

        // Assert
        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        verify(restClient).post(any(URI.class), bodyCaptor.capture());

        Object sentBody = bodyCaptor.getValue();
        assertThat(sentBody).isInstanceOf(AktørIdDto.class);
        AktørIdDto dto = (AktørIdDto) sentBody;
        assertThat(dto.getAktorId()).isEqualTo("1234567890123");
    }
}

