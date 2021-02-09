package no.nav.k9.sak.domene.person.tps;

import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.person.NavBrukerKjønn;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.domene.person.pdl.AktørTjeneste;
import no.nav.k9.sak.domene.person.pdl.PersonBasisTjeneste;
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.domene.person.pdl.PersoninfoTjeneste;
import no.nav.k9.sak.domene.person.pdl.TilknytningTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;

public class PersoninfoAdapterTest {

    private PersoninfoAdapter testSubject;

    private static final AktørId AKTØR_ID_SØKER = AktørId.dummy();
    private static final AktørId AKTØR_ID_BARN = AktørId.dummy();

    private static final PersonIdent PERSONIDENT_FNR_SØKER = new PersonIdent("07078516261");
    private static final PersonIdent PERSONIDENT_FNR_BARN = new PersonIdent("02028033445");

    private Personinfo mockPersoninfo;

    @BeforeEach
    public void setup() {
        Personinfo kjerneinfoSøker = lagHentPersonResponseForSøker();
        Personinfo kjerneinfobarn = lagHentPersonResponseForBarn();

        AktørTjeneste aktørTjeneste = mock(AktørTjeneste.class);

        when(aktørTjeneste.hentAktørIdForPersonIdent(PERSONIDENT_FNR_BARN)).thenReturn(of(AKTØR_ID_BARN));
        when(aktørTjeneste.hentPersonIdentForAktørId(AKTØR_ID_SØKER)).thenReturn(of(PERSONIDENT_FNR_SØKER));
        when(aktørTjeneste.hentPersonIdentForAktørId(AKTØR_ID_BARN)).thenReturn(of(PERSONIDENT_FNR_BARN));

        mockPersoninfo = mock(Personinfo.class);
        when(mockPersoninfo.getFødselsdato()).thenReturn(LocalDate.now()); // trenger bare en verdi

        PersonBasisTjeneste personBasisTjeneste = mock(PersonBasisTjeneste.class);
        PersoninfoTjeneste personinfoTjeneste = mock(PersoninfoTjeneste.class);
        when(personinfoTjeneste.hentKjerneinformasjon(AKTØR_ID_BARN, PERSONIDENT_FNR_BARN)).thenReturn(kjerneinfobarn);
        when(personinfoTjeneste.hentKjerneinformasjon(AKTØR_ID_SØKER, PERSONIDENT_FNR_SØKER)).thenReturn(kjerneinfoSøker);

        testSubject = new PersoninfoAdapter(personBasisTjeneste, personinfoTjeneste, aktørTjeneste, mock(TilknytningTjeneste.class));
    }

    @Test
    public void skal_innhente_saksopplysninger_for_søker() {
        // Arrange
        when(mockPersoninfo.getAktørId()).thenReturn(AKTØR_ID_SØKER);

        // Act and assert
        assertThat(testSubject.hentPersoninfo(AKTØR_ID_SØKER))
            .isNotNull()
            .extracting(Personinfo::getAktørId)
            .isEqualTo(AKTØR_ID_SØKER);
    }

    @Test
    public void skal_innhente_saksopplysninger_for_barn() {
        // Arrange
        when(mockPersoninfo.getAktørId()).thenReturn(AKTØR_ID_BARN);

        // Act and assert
        assertThat(testSubject.innhentSaksopplysningerForBarn(PERSONIDENT_FNR_BARN))
            .hasValueSatisfying(barn -> {
                    assertThat(barn.getAktørId()).isEqualTo(AKTØR_ID_BARN);
                    assertThat(barn.getFødselsdato()).isNotNull();
                }
            );
    }

    private Personinfo lagHentPersonResponseForSøker() {
        return new Personinfo.Builder().medAktørId(AKTØR_ID_SØKER).medPersonIdent(PERSONIDENT_FNR_SØKER).medNavn("Kari Nordmann").medFødselsdato(LocalDate.of(1985, 7, 7)).medKjønn(NavBrukerKjønn.KVINNE).build();
    }

    private Personinfo lagHentPersonResponseForBarn() {
        return new Personinfo.Builder().medAktørId(AKTØR_ID_BARN).medPersonIdent(PERSONIDENT_FNR_BARN).medNavn("Kari Nordmann Junior").medFødselsdato(LocalDate.of(2000, 7, 7)).medKjønn(NavBrukerKjønn.KVINNE).build();
    }
}
