package no.nav.k9.sak.domene.person.pdl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.k9.sak.test.util.aktør.FiktiveFnr;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.pdl.IdentGruppe;
import no.nav.pdl.IdentInformasjon;
import no.nav.pdl.Identliste;
import no.nav.vedtak.felles.integrasjon.pdl.PdlKlient;

public class AktørTjenesteTest {
    private final PersonIdent personIdent = new PersonIdent(new FiktiveFnr().nesteKvinneFnr());
    private final PdlKlient pdlMock = Mockito.mock(PdlKlient.class);

    private final AktørId aktørId = AktørId.dummy();
    private AktørTjeneste testSubject;

    @BeforeEach
    public void setup() {
        testSubject = new AktørTjeneste(pdlMock);
    }

    @Test
    public void hent_aktørid_for_personident_skal_ikke_feile_selv_om_pdlklient_ikke_finner_den() {
        when(pdlMock.hentIdenter(any(), any())).thenReturn(new Identliste(List.of(new IdentInformasjon(aktørId.getId(), IdentGruppe.AKTORID, false))));

        assertThat(testSubject.hentAktørIdForPersonIdent(personIdent))
            .hasValue(aktørId);
    }

    @Test
    public void hent_personident_for_aktørid_skal_ikke_feile_selv_om_pdlklient_ikke_finner_den() {
        when(pdlMock.hentIdenter(any(), any())).thenReturn(new Identliste(List.of(new IdentInformasjon(personIdent.getIdent(), IdentGruppe.FOLKEREGISTERIDENT, false))));

        assertThat(testSubject.hentPersonIdentForAktørId(aktørId))
            .hasValue(personIdent);
    }
}
