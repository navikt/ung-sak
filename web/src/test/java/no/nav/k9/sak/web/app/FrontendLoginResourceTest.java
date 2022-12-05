package no.nav.k9.sak.web.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.ContextPathHolder;
import no.nav.k9.sikkerhet.oidc.config.OpenIDProvider;
import no.nav.k9.sikkerhet.oidc.token.OidcToken;
import no.nav.k9.sikkerhet.oidc.token.bruker.BrukerTokenProvider;

public class FrontendLoginResourceTest {

    private FrontendLoginResource resource;

    @BeforeEach
    void setUp() {
        ContextPathHolder.instance("/k9/sak");
        var mock = mock(BrukerTokenProvider.class);
        var mockToken = mock(OidcToken.class);
        when(mockToken.getIssuer()).thenReturn(OpenIDProvider.ISSO);
        when(mock.getToken()).thenReturn(mockToken);
        resource = new FrontendLoginResource("", mock);
    }

    @SuppressWarnings("resource")
    @Test
    public void skal_hente_ut_relative_path_from_url() {
        Response response = resource.login("http://google.com/asdf", "", null);
        assertThat(response.getLocation()).hasPath("/asdf");

        response = resource.login("/", "", null);
        assertThat(response.getLocation()).hasPath("/");

        response = resource.login("/fagsak/1234/behandling/1234/opptjening", "", null);
        assertThat(response.getLocation()).hasPath("/fagsak/1234/behandling/1234/opptjening");

        response = resource.login("fagsak/1234/behandling/1234/opptjening", "", null);
        assertThat(response.getLocation()).hasPath("/fagsak/1234/behandling/1234/opptjening");
    }

    @SuppressWarnings("resource")
    @Test
    public void innlogging_fra_k9_sak_web() {
        var basepath = "https://app.adeo.no";
        var response = resource.login(basepath, "", null);
        assertThat(URI.create("/")).isEqualTo(response.getLocation());

        var hovedside = "/k9/web/";
        response = resource.login(hovedside, "", null);
        assertThat(URI.create(hovedside)).isEqualTo(response.getLocation());

        var medQuery = "/k9/web/fagsak/1234/behandling/?collapsed=true";
        response = resource.login(medQuery, "", null);
        assertThat(URI.create(medQuery)).isEqualTo(response.getLocation());

        var medFragment = "/k9/web/fagsak/1234/behandling/#panel-42";
        response = resource.login(medFragment, "", null);
        assertThat(URI.create(medFragment)).isEqualTo(response.getLocation());

        var medQueryOgFragment = "/k9/web/fagsak/1234/behandling/?collapsed=true#panel42";
        response = resource.login(medQueryOgFragment, "", null);
        assertThat(URI.create(medQueryOgFragment)).isEqualTo(response.getLocation());
    }
}
