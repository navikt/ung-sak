package no.nav.ung.domenetjenester.oppgave.behandlendeenhet;

import no.nav.ung.kodeverk.behandling.BehandlingTema;
import no.nav.ung.kodeverk.person.Diskresjonskode;
import no.nav.ung.kodeverk.produksjonsstyring.OmrådeTema;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.sak.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.ung.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.ung.sak.produksjonsstyring.behandlingenhet.EnhetsTjeneste;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BehandlendeEnhetServiceTest {

    private PersoninfoAdapter personinfoAdapter;
    private EnhetsTjeneste enhetsTjeneste;
    private BehandlendeEnhetService service;

    @BeforeEach
    void setUp() {
        personinfoAdapter = mock(PersoninfoAdapter.class);
        enhetsTjeneste = mock(EnhetsTjeneste.class);
        service = new BehandlendeEnhetService(personinfoAdapter, enhetsTjeneste);
    }

    @Test
    void skalIkkeKasteForUdefinertBehandlingTemaOgSkalSlåOppEnhetSomVanlig() {
        // Klager kommer inn uten kjent fagsak/ytelse, og har derfor BehandlingTema.UDEFINERT.
        // Dette skal ikke lenger føre til IllegalArgumentException.
        var aktørId = new AktørId("1234567890123");
        var personIdent = new PersonIdent("12345678901");
        when(personinfoAdapter.hentIdentForAktørId(aktørId)).thenReturn(Optional.of(personIdent));
        when(personinfoAdapter.hentGeografiskTilknytning(any(), any()))
            .thenReturn(new GeografiskTilknytning("0301", null));
        when(enhetsTjeneste.hentFordelingEnhetId(any(), any(), any()))
            .thenReturn(List.of(new OrganisasjonsEnhet("4487", "NAV enhet")));

        BehandlendeEnhet behandlendeEnhet = service.hentBehandlendeEnhet(OmrådeTema.UNG, BehandlingTema.UDEFINERT, aktørId);

        assertThat(behandlendeEnhet.nummer()).isEqualTo("4487");
    }

    @Test
    void skalSlåOppEnhetForUngdomsprogramytelsen() {
        var aktørId = new AktørId("1234567890123");
        var personIdent = new PersonIdent("12345678901");
        when(personinfoAdapter.hentIdentForAktørId(aktørId)).thenReturn(Optional.of(personIdent));
        when(personinfoAdapter.hentGeografiskTilknytning(any(), any()))
            .thenReturn(new GeografiskTilknytning("0301", Diskresjonskode.KODE6));
        when(enhetsTjeneste.hentFordelingEnhetId(any(), any(), any()))
            .thenReturn(List.of(new OrganisasjonsEnhet("4487", "NAV enhet")));

        BehandlendeEnhet behandlendeEnhet = service.hentBehandlendeEnhet(OmrådeTema.UNG, BehandlingTema.UNGDOMSPROGRAMYTELSEN, aktørId);

        assertThat(behandlendeEnhet.nummer()).isEqualTo("4487");
    }

    @Test
    void skalKasteVedNullBehandlingTema() {
        var aktørId = new AktørId("1234567890123");

        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class,
            () -> service.hentBehandlendeEnhet(OmrådeTema.UNG, null, aktørId));
    }
}
