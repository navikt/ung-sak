package no.nav.ung.sak.web.app.oppgave;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Optional;

import jakarta.servlet.ServletException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.ung.sak.kontrakt.produksjonsstyring.OppgaveIdDto;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKobling;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKoblingRepository;
import no.nav.ung.sak.test.util.Whitebox;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.k9.felles.sikkerhet.ContextPathHolder;

@SuppressWarnings({ "deprecation", "resource" })
public class OppgaveRedirectTjenesteTest {

    private OppgaveBehandlingKoblingRepository oppgaveRepo = Mockito.mock(OppgaveBehandlingKoblingRepository.class);
    private FagsakRepository fagsakRepo = Mockito.mock(FagsakRepository.class);

    private OppgaveRedirectTjeneste tjeneste = new OppgaveRedirectTjeneste(oppgaveRepo, fagsakRepo);

    private final Saksnummer saksnummer = new Saksnummer("22");

    @BeforeEach
    public void setContextPath() {
        ContextPathHolder.instance("/ung/sak");
    }

    @Test
    public void skal_lage_url_med_feilmelding_når_hverken_oppgaveId_eller_sakId_finnes() throws ServletException, IOException {
        Response response = tjeneste.doRedirect(null, null);

        String feilmelding = "Sak+kan+ikke+%C3%A5pnes%2C+da+referanse+mangler.";
        assertThat(response.getStatus()).isEqualTo(Response.Status.TEMPORARY_REDIRECT.getStatusCode());
        assertThat(response.getLocation().toString()).contains("/ung/sak/#?errormessage=" + feilmelding);
    }

    @Test
    public void skal_lage_url_med_feilmelding_når_både_oppgaveId_og_sakId_finnes_i_url_men_ikke_finnes_ikke_i_vl() throws ServletException, IOException {
        Response response = tjeneste.doRedirect(new OppgaveIdDto("1"), new SaksnummerDto("2"));
        String feilmelding = "Det+finnes+ingen+sak+med+dette+saksnummeret%3A+2";
        assertThat(response.getStatus()).isEqualTo(Response.Status.TEMPORARY_REDIRECT.getStatusCode());
        assertThat(response.getLocation().toString()).contains("/ung/sak/#?errormessage=" + feilmelding);
    }

    @Test
    public void skal_lage_url_med_saksnummer_og_behandlingId_når_oppgave_finnes_og_sakId_ikke_finnes_i_url() throws ServletException, IOException {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, null, saksnummer);
        fagsak.setId(2L);

        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        OppgaveBehandlingKobling kobling = new OppgaveBehandlingKobling(OppgaveÅrsak.BEHANDLE_SAK_VL, "1", saksnummer, behandling);
        Mockito.when(oppgaveRepo.hentOppgaveBehandlingKobling("1")).thenReturn(Optional.of(kobling));
        Mockito.when(fagsakRepo.finnEksaktFagsak(2)).thenReturn(fagsak);

        Response response = tjeneste.doRedirect(new OppgaveIdDto("1"), null);
        assertThat(response.getStatus()).isEqualTo(Response.Status.TEMPORARY_REDIRECT.getStatusCode());
        assertThat(response.getLocation().toString()).contains("/ung/sak/fagsak/22/");
    }

    @Test
    public void skal_lage_url_med_saksnummer_når_oppgave_ikke_finnes() throws ServletException, IOException {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, null, saksnummer);
        fagsak.setId(12L);
        Mockito.when(fagsakRepo.hentSakGittSaksnummer(saksnummer)).thenReturn(Optional.of(fagsak));
        Mockito.when(fagsakRepo.finnUnikFagsak(12L)).thenReturn(Optional.of(fagsak));

        Response response = tjeneste.doRedirect(new OppgaveIdDto("1"), new SaksnummerDto(saksnummer));
        assertThat(response.getStatus()).isEqualTo(Response.Status.TEMPORARY_REDIRECT.getStatusCode());
        assertThat(response.getLocation().toString()).contains("/ung/sak/fagsak/22/");
    }

    @Test
    public void skal_lage_feil_vedinkonsistens_oppgave_fagsakfinnes() throws ServletException, IOException {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, null);
        Whitebox.setInternalState(fagsak, "id", 3l);
        Whitebox.setInternalState(fagsak, "saksnummer", new Saksnummer("123"));
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        OppgaveBehandlingKobling kobling = new OppgaveBehandlingKobling(OppgaveÅrsak.BEHANDLE_SAK_VL, "1", saksnummer, behandling);
        Mockito.when(fagsakRepo.hentSakGittSaksnummer(saksnummer)).thenReturn(Optional.of(fagsak));
        Mockito.when(fagsakRepo.finnEksaktFagsak(3)).thenReturn(fagsak);

        Mockito.when(oppgaveRepo.hentOppgaveBehandlingKobling("1")).thenReturn(Optional.of(kobling));

        Response response = tjeneste.doRedirect(new OppgaveIdDto("1"), new SaksnummerDto(saksnummer));
        assertThat(response.getStatus()).isEqualTo(Response.Status.TEMPORARY_REDIRECT.getStatusCode());
        String feilmelding = "Oppgaven+med+1+er+ikke+registrert+p%C3%A5+sak+22";
        assertThat(response.getLocation().toString()).contains("/ung/sak/#?errormessage=" + feilmelding);
    }

    @Test
    public void skal_lage_url_med_behandlingsid_og_saksnummer_når_oppgave_finnes() throws ServletException, IOException {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, null);
        Whitebox.setInternalState(fagsak, "id", 5l);
        Whitebox.setInternalState(fagsak, "saksnummer", saksnummer);
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        OppgaveBehandlingKobling kobling = new OppgaveBehandlingKobling(OppgaveÅrsak.BEHANDLE_SAK_VL, "1", saksnummer, behandling);
        Mockito.when(fagsakRepo.hentSakGittSaksnummer(saksnummer)).thenReturn(Optional.of(fagsak));
        Mockito.when(fagsakRepo.finnEksaktFagsak(5)).thenReturn(fagsak);

        Mockito.when(oppgaveRepo.hentOppgaveBehandlingKobling("1")).thenReturn(Optional.of(kobling));

        Response response = tjeneste.doRedirect(new OppgaveIdDto("1"), new SaksnummerDto(saksnummer));
        assertThat(response.getStatus()).isEqualTo(Response.Status.TEMPORARY_REDIRECT.getStatusCode());
        assertThat(response.getLocation().toString()).contains("/ung/sak/fagsak/22/");
    }

    @Test
    public void skal_lage_url_med_saksnummer_når_oppgave_ikke_oppgitt() throws ServletException, IOException {
        Saksnummer saksnummer = new Saksnummer("22");
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, null, saksnummer);
        Mockito.when(fagsakRepo.hentSakGittSaksnummer(saksnummer)).thenReturn(Optional.of(fagsak));

        Response responseSnr = tjeneste.doRedirect(null, new SaksnummerDto(saksnummer));
        assertThat(responseSnr.getStatus()).isEqualTo(Response.Status.TEMPORARY_REDIRECT.getStatusCode());
        assertThat(responseSnr.getLocation().toString()).contains("/ung/sak/fagsak/22/");

    }
}
