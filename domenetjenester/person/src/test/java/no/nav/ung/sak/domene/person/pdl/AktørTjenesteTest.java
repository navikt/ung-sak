package no.nav.ung.sak.domene.person.pdl;

import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import no.nav.ung.sak.test.util.aktør.FiktiveFnr;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;
import no.nav.k9.felles.exception.VLException;
import no.nav.k9.felles.integrasjon.pdl.HentIdenterBolkResult;
import no.nav.k9.felles.integrasjon.pdl.HentIdenterQueryRequest;
import no.nav.k9.felles.integrasjon.pdl.IdentGruppe;
import no.nav.k9.felles.integrasjon.pdl.IdentInformasjon;
import no.nav.k9.felles.integrasjon.pdl.Identliste;
import no.nav.k9.felles.integrasjon.pdl.PdlKlient;

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
        when(pdlMock.hentIdenter(any(), any())).thenReturn(new Identliste(of(new IdentInformasjon(aktørId.getId(), IdentGruppe.AKTORID, false))));

        assertThat(testSubject.hentAktørIdForPersonIdent(personIdent))
            .hasValue(aktørId);
    }

    @Test
    public void hent_personident_for_aktørid_skal_ikke_feile_selv_om_pdlklient_ikke_finner_den() {
        when(pdlMock.hentIdenter(any(), any())).thenReturn(new Identliste(of(new IdentInformasjon(personIdent.getIdent(), IdentGruppe.FOLKEREGISTERIDENT, false))));

        assertThat(testSubject.hentPersonIdentForAktørId(aktørId))
            .hasValue(personIdent);
    }

    @Test
    void hentAktørIdForPersonIdentSet_skal_gi_tilsvarende_som_kommer_fra_pdlklient() {
        Set<PersonIdent> personIdent = Set.of(this.personIdent);

        when(pdlMock.hentIdenterBolkResults(any(), any()))
            .thenReturn(
                of(
                    new HentIdenterBolkResult(
                        aktørId.getId(),
                        of(new IdentInformasjon(aktørId.getId(), IdentGruppe.AKTORID, false)),
                        "ok")
                )
            );

        assertThat(testSubject.hentAktørIdForPersonIdentSet(personIdent))
            .containsExactly(aktørId);
    }

    @Test
    void hentAlleAktørIderForPersonIdent_skal_returnere_gjeldende_og_historiske_aktørIder() {
        var historiskAktørId = AktørId.dummy();
        when(pdlMock.hentIdenter(any(), any())).thenReturn(new Identliste(of(
            new IdentInformasjon(aktørId.getId(), IdentGruppe.AKTORID, false),
            new IdentInformasjon(historiskAktørId.getId(), IdentGruppe.AKTORID, true))));

        var resultat = testSubject.hentAlleAktørIderForPersonIdent(personIdent);

        assertThat(resultat).containsExactlyInAnyOrder(aktørId, historiskAktørId);
        var requestCaptor = ArgumentCaptor.forClass(HentIdenterQueryRequest.class);
        verify(pdlMock).hentIdenter(requestCaptor.capture(), any());
        assertThat(requestCaptor.getValue().getInput()).containsEntry("historikk", true);
    }

    @Test
    void hentAlleAktørIderForPersonIdent_skal_gi_tom_mengde_når_person_ikke_finnes() {
        var ikkeFunnet = mock(VLException.class);
        when(ikkeFunnet.getKode()).thenReturn(PdlKlient.PDL_KLIENT_NOT_FOUND_KODE);
        when(pdlMock.hentIdenter(any(), any())).thenThrow(ikkeFunnet);

        assertThat(testSubject.hentAlleAktørIderForPersonIdent(personIdent)).isEmpty();
    }
}
