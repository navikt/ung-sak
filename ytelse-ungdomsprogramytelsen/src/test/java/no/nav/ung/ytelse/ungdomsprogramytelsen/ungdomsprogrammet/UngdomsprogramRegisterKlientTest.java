package no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet;

import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.ung.sak.kontrakt.person.AktørIdDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UngdomsprogramRegisterKlientTest {

    private OidcRestClient restClient;
    private UngdomsprogramRegisterKlient klient;

    @BeforeEach
    void setUp() {
        restClient = mock(OidcRestClient.class);
        klient = new UngdomsprogramRegisterKlient(restClient, "http://localhost:8080");
    }

    @Test
    void hentForAktørId_senderAktørIdMotHentUri() {
        // Arrange
        var forventetResultat = new UngdomsprogramRegisterKlient.DeltakerOpplysningerDTO(java.util.List.of());
        when(restClient.post(any(URI.class), any(), eq(UngdomsprogramRegisterKlient.DeltakerOpplysningerDTO.class)))
            .thenReturn(forventetResultat);

        // Act
        var resultat = klient.hentForAktørId("1234567890123");

        // Assert
        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        verify(restClient).post(uriCaptor.capture(), bodyCaptor.capture(), eq(UngdomsprogramRegisterKlient.DeltakerOpplysningerDTO.class));

        assertThat(uriCaptor.getValue()).hasToString("http://localhost:8080/register/hent/alle");
        assertThat(bodyCaptor.getValue()).isInstanceOf(AktørIdDto.class);
        assertThat(((AktørIdDto) bodyCaptor.getValue()).getAktorId()).isEqualTo("1234567890123");
        assertThat(resultat).isEqualTo(forventetResultat);
    }

    @Test
    void markerSomSøkt_senderAktørIdMotMarkerSoktUriForDeltakelsen() {
        // Arrange
        UUID deltakelseId = UUID.randomUUID();

        // Act
        klient.markerSomSøkt("1234567890123", deltakelseId);

        // Assert
        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        verify(restClient).patch(uriCaptor.capture(), bodyCaptor.capture());

        assertThat(uriCaptor.getValue()).hasToString("http://localhost:8080/register/" + deltakelseId + "/marker-sokt");
        assertThat(bodyCaptor.getValue()).isInstanceOf(AktørIdDto.class);
        assertThat(((AktørIdDto) bodyCaptor.getValue()).getAktorId()).isEqualTo("1234567890123");
    }
}
