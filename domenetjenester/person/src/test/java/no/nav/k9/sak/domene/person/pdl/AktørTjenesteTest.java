package no.nav.k9.sak.domene.person.pdl;

import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.k9.sak.test.util.aktør.FiktiveFnr;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.vedtak.felles.integrasjon.aktør.klient.AktørConsumer;
import no.nav.vedtak.felles.integrasjon.pdl.PdlKlient;

public class AktørTjenesteTest {
    private final AktørConsumer aktørConsumerMock = Mockito.mock(AktørConsumer.class);
    private final PersonIdent personIdent = new PersonIdent(new FiktiveFnr().nesteKvinneFnr());
    private final PdlKlient pdlMock = Mockito.mock(PdlKlient.class);

    private final AktørId aktørId = AktørId.dummy();
    private AktørTjeneste testSubject;

    @BeforeEach
    public void setup() {
        testSubject = new AktørTjeneste(pdlMock, aktørConsumerMock);
    }

    @Test
    public void hent_aktørid_for_personident_skal_ikke_feile_selv_om_pdlklient_ikke_finner_den() {
        when(aktørConsumerMock.hentAktørIdForPersonIdent(personIdent.getIdent())).thenReturn(of(aktørId.getId()));

        assertThat(testSubject.hentAktørIdForPersonIdent(personIdent))
            .hasValue(aktørId);
    }

    @Test
    public void hent_personident_for_aktørid_skal_ikke_feile_selv_om_pdlklient_ikke_finner_den() {
        when(aktørConsumerMock.hentPersonIdentForAktørId(aktørId.getId())).thenReturn(of(personIdent.getIdent()));

        assertThat(testSubject.hentPersonIdentForAktørId(aktørId))
            .hasValue(personIdent);
    }
}
