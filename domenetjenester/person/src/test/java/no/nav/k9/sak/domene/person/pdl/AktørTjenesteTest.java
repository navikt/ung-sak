package no.nav.k9.sak.domene.person.pdl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

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
    private final PdlKlient pdlMock = Mockito.mock(PdlKlient.class);
    private final AktørId aktørId = AktørId.dummy();
    private final PersonIdent fnr = new PersonIdent(new FiktiveFnr().nesteKvinneFnr());
    private AktørTjeneste aktørTjeneste;

    @BeforeEach
    public void setup() {
        aktørTjeneste = new AktørTjeneste(pdlMock);
    }

    @Test
    public void basics_hent_aktørid() {
        Mockito.when(pdlMock.hentIdenter(any(), any(), any())).thenReturn(new Identliste(List.of(new IdentInformasjon(aktørId.getId(), IdentGruppe.AKTORID, false))));

        assertThat(aktørTjeneste.hentAktørIdForPersonIdent(fnr))
            .hasValue(aktørId);
    }

    @Test
    public void basics_hent_ident() {
        Mockito.when(pdlMock.hentIdenter(any(), any(), any())).thenReturn(new Identliste(List.of(new IdentInformasjon(fnr.getIdent(), IdentGruppe.FOLKEREGISTERIDENT, false))));

        assertThat(aktørTjeneste.hentPersonIdentForAktørId(aktørId))
            .hasValue(fnr);
    }
}
