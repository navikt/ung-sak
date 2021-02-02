package no.nav.k9.sak.domene.person.tps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import no.nav.k9.kodeverk.geografisk.AdresseType;
import no.nav.k9.kodeverk.person.NavBrukerKjønn;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.sak.behandlingslager.aktør.Adresseinfo;
import no.nav.k9.sak.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentGeografiskTilknytningPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentGeografiskTilknytningSikkerhetsbegrensing;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Diskresjonskoder;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Kommune;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningResponse;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.person.PersonConsumer;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class TpsAdapterImplTest {
    private final PersonConsumer personProxyServiceMock = mock(PersonConsumer.class);
    private TpsAdapterImpl testSubject;
    private final AktørId aktørId = AktørId.dummy();
    private final PersonIdent fnr = new PersonIdent("31018143212");

    @BeforeEach
    public void setup() {
        TpsAdresseOversetter tpsAdresseOversetter = new TpsAdresseOversetter();
        TpsOversetter tpsOversetter = new TpsOversetter(
            tpsAdresseOversetter);
        testSubject = new TpsAdapterImpl(personProxyServiceMock, tpsOversetter);
    }

    @Test
    public void test_hentKjerneinformasjon_normal() throws Exception {
        AktørId aktørId = AktørId.dummy();
        String navn = "John Doe";
        LocalDate fødselsdato = LocalDate.of(1343, 12, 12);
        NavBrukerKjønn kjønn = NavBrukerKjønn.KVINNE;

        HentPersonResponse response = new HentPersonResponse();
        Bruker person = new Bruker();
        response.setPerson(person);
        Mockito.when(personProxyServiceMock.hentPersonResponse(Mockito.any())).thenReturn(response);

        TpsOversetter tpsOversetterMock = mock(TpsOversetter.class);
        Personinfo personinfo0 = new Personinfo.Builder()
            .medPersonIdent(fnr)
            .medNavn(navn)
            .medFødselsdato(fødselsdato)
            .medKjønn(kjønn)
            .medAktørId(aktørId)
            .build();

        Mockito.when(tpsOversetterMock.tilBrukerInfo(Mockito.any(AktørId.class), eq(person))).thenReturn(personinfo0);
        testSubject = new TpsAdapterImpl(personProxyServiceMock, tpsOversetterMock);

        Personinfo personinfo = testSubject.hentKjerneinformasjon(fnr, aktørId);
        assertNotNull(personinfo);
        assertThat(personinfo.getAktørId()).isEqualTo(aktørId);
        assertThat(personinfo.getPersonIdent()).isEqualTo(fnr);
        assertThat(personinfo.getNavn()).isEqualTo(navn);
        assertThat(personinfo.getFødselsdato()).isEqualTo(fødselsdato);
    }

    @Test
    public void test_hentGegrafiskTilknytning_vha_fnr() throws Exception {
        final String diskresjonskode = "SPSF";
        final String kommune = "0219";

        HentGeografiskTilknytningResponse response = mockHentGeografiskTilknytningResponse(kommune, diskresjonskode);
        Mockito.when(personProxyServiceMock.hentGeografiskTilknytning(Mockito.any())).thenReturn(response);

        GeografiskTilknytning tilknytning = testSubject.hentGeografiskTilknytning(fnr);
        assertNotNull(tilknytning);
        assertThat(tilknytning.getDiskresjonskode().getKode()).isEqualTo(diskresjonskode);
        assertThat(tilknytning.getTilknytning()).isEqualTo(kommune);
    }

    @SuppressWarnings("SameParameterValue")
    private HentGeografiskTilknytningResponse mockHentGeografiskTilknytningResponse(String kommune, String diskresjonskode) {
        HentGeografiskTilknytningResponse response = new HentGeografiskTilknytningResponse();
        Kommune k = new Kommune();
        k.setGeografiskTilknytning(kommune);
        response.setGeografiskTilknytning(k);

        Diskresjonskoder dk = new Diskresjonskoder();
        dk.setValue(diskresjonskode);
        response.setDiskresjonskode(dk);

        return response;
    }

    @Test
    public void skal_få_exception_når_tjenesten_ikke_kan_finne_personen() {
        Assertions.assertThrows(TekniskException.class, () -> {
            Mockito.when(personProxyServiceMock.hentPersonResponse(Mockito.any()))
                .thenThrow(new HentPersonPersonIkkeFunnet(null, null));

            testSubject.hentKjerneinformasjon(fnr, aktørId);
        });
    }

    @Test
    public void skal_få_exception_når_tjenesten_ikke_kan_aksesseres_pga_manglende_tilgang() {
        Assertions.assertThrows(ManglerTilgangException.class, () -> {
            when(personProxyServiceMock.hentPersonResponse(any(HentPersonRequest.class)))
                .thenThrow(new HentPersonSikkerhetsbegrensning(null, null));

            testSubject.hentKjerneinformasjon(fnr, aktørId);
        });
    }

    @Test
    public void skal_få_exception_når_tjenesten_ikke_kan_finne_geografisk_tilknytning_for_personen() {
        Assertions.assertThrows(TekniskException.class, () -> {
            Mockito.when(personProxyServiceMock.hentGeografiskTilknytning(Mockito.any()))
                .thenThrow(new HentGeografiskTilknytningPersonIkkeFunnet(null, null));

            testSubject.hentGeografiskTilknytning(fnr);
        });
    }

    @Test
    public void skal_få_exception_ved_henting_av_geografisk_tilknytning_når_tjenesten_ikke_kan_aksesseres_pga_manglende_tilgang() {
        Assertions.assertThrows(ManglerTilgangException.class, () -> {
            when(personProxyServiceMock.hentGeografiskTilknytning(Mockito.any()))
                .thenThrow(new HentGeografiskTilknytningSikkerhetsbegrensing(null, null));

            testSubject.hentGeografiskTilknytning(fnr);
        });
    }

    @Test
    public void test_hentAdresseinformasjon_normal() throws HentPersonSikkerhetsbegrensning, HentPersonPersonIkkeFunnet {
        HentPersonResponse response = new HentPersonResponse();
        Bruker person = new Bruker();
        response.setPerson(person);

        ArgumentCaptor<HentPersonRequest> captor = ArgumentCaptor.forClass(HentPersonRequest.class);
        when(personProxyServiceMock.hentPersonResponse(captor.capture())).thenReturn(response);

        final String addresse = "Veien 17";

        TpsOversetter tpsOversetterMock = mock(TpsOversetter.class);
        Adresseinfo.Builder builder = new Adresseinfo.Builder(AdresseType.BOSTEDSADRESSE,
            new PersonIdent("31018143212"),
            "Tjoms",
            PersonstatusType.BOSA);
        Adresseinfo adresseinfoExpected = builder.medAdresselinje1(addresse).build();

        when(tpsOversetterMock.tilAdresseInfo(eq(person))).thenReturn(adresseinfoExpected);
        testSubject = new TpsAdapterImpl(personProxyServiceMock, tpsOversetterMock);

        Adresseinfo adresseinfoActual = testSubject.hentAdresseinformasjon(fnr);

        assertThat(adresseinfoActual).isNotNull();
        assertThat(adresseinfoActual).isEqualTo(adresseinfoExpected);
        assertThat(adresseinfoActual.getAdresselinje1()).isEqualTo(adresseinfoExpected.getAdresselinje1());
    }

    @Test
    public void test_hentAdresseinformasjon_personIkkeFunnet() {
        Assertions.assertThrows(TekniskException.class, () -> {
            when(personProxyServiceMock.hentPersonResponse(any())).thenThrow(new HentPersonPersonIkkeFunnet(null, null));

            testSubject.hentAdresseinformasjon(fnr);
        });

    }

    @Test
    public void test_hentAdresseinformasjon_manglende_tilgang() {
        Assertions.assertThrows(ManglerTilgangException.class, () -> {
            when(personProxyServiceMock.hentPersonResponse(any())).thenThrow(new HentPersonSikkerhetsbegrensning(null, null));

            testSubject.hentAdresseinformasjon(fnr);
        });

    }
}
