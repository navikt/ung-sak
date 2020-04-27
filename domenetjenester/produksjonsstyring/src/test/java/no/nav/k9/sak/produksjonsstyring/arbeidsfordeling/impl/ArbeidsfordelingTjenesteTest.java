package no.nav.k9.sak.produksjonsstyring.arbeidsfordeling.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.sak.produksjonsstyring.arbeidsfordeling.ArbeidsfordelingTjeneste;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Enhetsstatus;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Organisasjonsenhet;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.meldinger.FinnAlleBehandlendeEnheterListeRequest;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.meldinger.FinnAlleBehandlendeEnheterListeResponse;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.meldinger.FinnBehandlendeEnhetListeRequest;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.meldinger.FinnBehandlendeEnhetListeResponse;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.arbeidsfordeling.klient.ArbeidsfordelingConsumer;

public class ArbeidsfordelingTjenesteTest {

    private static final FagsakYtelseType YTELSE_TYPE = FagsakYtelseType.OMSORGSPENGER;
    private static final String RESPONSE_ENHETS_ID = "1234";
    private static final String RESPONSE_ENHETS_ID2 = "35456";
    private static final String GEOGRAFISK_TILKNYTNING = "0219";
    private static final String DISKRESJONSKODE = "UFB";
    private static final String KODE6_DISKRESJON = "SPSF";
    private static final String KODE6_ENHET = "2103";

    private ArbeidsfordelingConsumer consumer = mock(ArbeidsfordelingConsumer.class);
    private ArbeidsfordelingTjeneste tjeneste = new ArbeidsfordelingTjeneste(consumer);

    @Test(expected = TekniskException.class)
    public void skal_kaste_exception_når_søk_etter_behandlende_enhet_er_tom() throws Exception {
        ArgumentCaptor<FinnBehandlendeEnhetListeRequest> captor = ArgumentCaptor.forClass(FinnBehandlendeEnhetListeRequest.class);
        FinnBehandlendeEnhetListeResponse tomResponse = new FinnBehandlendeEnhetListeResponse();
        when(consumer.finnBehandlendeEnhetListe(captor.capture())).thenReturn(tomResponse);

        tjeneste.finnBehandlendeEnhet(GEOGRAFISK_TILKNYTNING, DISKRESJONSKODE, YTELSE_TYPE);
    }

    @Test
    public void skal_hente_ut_den_første_enhetens_id_og_logge_WARN_når_svaret_inneholder_flere_enheter() throws Exception {

        ArgumentCaptor<FinnBehandlendeEnhetListeRequest> captor = ArgumentCaptor.forClass(FinnBehandlendeEnhetListeRequest.class);
        when(consumer.finnBehandlendeEnhetListe(captor.capture())).thenReturn(opprettResponseMedToEnheter());

        OrganisasjonsEnhet organisasjonsEnhet = tjeneste.finnBehandlendeEnhet(GEOGRAFISK_TILKNYTNING, DISKRESJONSKODE, YTELSE_TYPE);
        //Verifiser svar
        assertThat(organisasjonsEnhet.getEnhetId()).isEqualTo(RESPONSE_ENHETS_ID);
    }

    @Test
    public void skal_hente_ut_enhets_liste_ved_normal_svar() throws Exception {
        // Arrange
        ArgumentCaptor<FinnAlleBehandlendeEnheterListeRequest> captor = ArgumentCaptor.forClass(FinnAlleBehandlendeEnheterListeRequest.class);
        when(consumer.finnAlleBehandlendeEnheterListe(captor.capture())).thenReturn(opprettResponseForHentAlleEnheterListe());

        // Act
        List<OrganisasjonsEnhet> orgEnheter = tjeneste.finnAlleBehandlendeEnhetListe(YTELSE_TYPE);

        // Assert

        List<String> forventetEnhetsIder = List.of(RESPONSE_ENHETS_ID, RESPONSE_ENHETS_ID2);
        assertThat(orgEnheter).hasSize(3); // inkluderer også klageenheten by default
        assertThat(orgEnheter.stream().map(OrganisasjonsEnhet::getEnhetId).collect(Collectors.toList()))
            .containsAll(forventetEnhetsIder);
    }

    @Test(expected = TekniskException.class)
    public void skal_kaste_exception_når_søk_etter_behandlende_enhetsliste_er_tom() throws Exception {
        ArgumentCaptor<FinnAlleBehandlendeEnheterListeRequest> captor = ArgumentCaptor.forClass(FinnAlleBehandlendeEnheterListeRequest.class);
        FinnAlleBehandlendeEnheterListeResponse tomResponse = new FinnAlleBehandlendeEnheterListeResponse();
        when(consumer.finnAlleBehandlendeEnheterListe(captor.capture())).thenReturn(tomResponse);

        tjeneste.finnAlleBehandlendeEnhetListe(YTELSE_TYPE);
    }

    @Test
    public void skal_returnere_enhet_for_kode6() throws Exception {
        FinnAlleBehandlendeEnheterListeResponse kode6 = opprettResponseMedKode6Enhet();
        when(consumer.finnAlleBehandlendeEnheterListe(any())).thenReturn(kode6);

        OrganisasjonsEnhet organisasjonsEnhet = tjeneste.hentEnhetForDiskresjonskode(KODE6_DISKRESJON, YTELSE_TYPE);

        //Verifiser svar
        assertThat(organisasjonsEnhet.getEnhetId()).isEqualTo(KODE6_ENHET);
    }

    private FinnAlleBehandlendeEnheterListeResponse opprettResponseMedKode6Enhet() throws Exception {
        FinnAlleBehandlendeEnheterListeResponse response = new FinnAlleBehandlendeEnheterListeResponse();
        Organisasjonsenhet enhet = new Organisasjonsenhet();
        enhet.setEnhetId(KODE6_ENHET);
        enhet.setEnhetNavn("NAV Viken");
        enhet.setStatus(Enhetsstatus.AKTIV);
        enhet.setOrganisasjonsnummer("33333");

        response.getBehandlendeEnhetListe().add(enhet);
        return response;
    }

    private FinnBehandlendeEnhetListeResponse opprettResponseMedToEnheter() throws Exception {
        FinnBehandlendeEnhetListeResponse response = new FinnBehandlendeEnhetListeResponse();
        Organisasjonsenhet enhet1 = new Organisasjonsenhet();
        enhet1.setEnhetId(RESPONSE_ENHETS_ID);
        enhet1.setEnhetNavn("test navn");
        enhet1.setOrganisasjonsnummer("45678");
        enhet1.setStatus(Enhetsstatus.AKTIV);
        response.getBehandlendeEnhetListe().add(enhet1);

        Organisasjonsenhet enhet2 = new Organisasjonsenhet();
        enhet2.setEnhetId(RESPONSE_ENHETS_ID2);
        enhet2.setEnhetNavn("test navn2");
        enhet2.setOrganisasjonsnummer("45679");
        enhet2.setStatus(Enhetsstatus.AKTIV);
        response.getBehandlendeEnhetListe().add(enhet2);
        return response;
    }

    private FinnAlleBehandlendeEnheterListeResponse opprettResponseForHentAlleEnheterListe() throws Exception {
        FinnAlleBehandlendeEnheterListeResponse response = new FinnAlleBehandlendeEnheterListeResponse();
        Organisasjonsenhet enhet1 = new Organisasjonsenhet();
        enhet1.setEnhetId(RESPONSE_ENHETS_ID);
        enhet1.setEnhetNavn("Anne Lier");
        enhet1.setOrganisasjonsnummer("5443");
        enhet1.setStatus(Enhetsstatus.AKTIV);
        response.getBehandlendeEnhetListe().add(enhet1);

        Organisasjonsenhet enhet2 = new Organisasjonsenhet();
        enhet2.setEnhetId(RESPONSE_ENHETS_ID2);
        enhet2.setEnhetNavn("Hansen");
        enhet2.setOrganisasjonsnummer("6543");
        enhet2.setStatus(Enhetsstatus.AKTIV);
        response.getBehandlendeEnhetListe().add(enhet2);
        return response;
    }

}
