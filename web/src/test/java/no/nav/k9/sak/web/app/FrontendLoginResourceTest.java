package no.nav.k9.sak.web.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.Response;

import org.junit.Test;

import java.net.URI;

public class FrontendLoginResourceTest {

    private FrontendLoginResource resource = new FrontendLoginResource();

    @SuppressWarnings("resource")
    @Test
    public void skal_hente_ut_relative_path_from_url() {
        Response response = resource.login("http://google.com/asdf");
        assertThat(response.getLocation()).hasPath("/asdf");

        response = resource.login("/");
        assertThat(response.getLocation()).hasPath("/");

        response = resource.login("/fagsak/1234/behandling/1234/opptjening");
        assertThat(response.getLocation()).hasPath("/fagsak/1234/behandling/1234/opptjening");

        response = resource.login("fagsak/1234/behandling/1234/opptjening");
        assertThat(response.getLocation()).hasPath("/fagsak/1234/behandling/1234/opptjening");
    }

    @Test
    public void innlogging_fra_k9_sak_web() {
        var basepath = "https://app.adeo.no";
        var response = resource.login(basepath);
        assertEquals(URI.create("/"), response.getLocation());

        var hovedside = "/k9/web/";
        response = resource.login(hovedside);
        assertEquals(URI.create(hovedside), response.getLocation());

        var medQuery = "/k9/web/fagsak/1234/behandling/?collapsed=true";
        response = resource.login(medQuery);
        assertEquals(URI.create(medQuery), response.getLocation());

        var medFragment = "/k9/web/fagsak/1234/behandling/#panel-42";
        response = resource.login(medFragment);
        assertEquals(URI.create(medFragment), response.getLocation());

        var medQueryOgFragment = "/k9/web/fagsak/1234/behandling/?collapsed=true#panel42";
        response = resource.login(medQueryOgFragment);
        assertEquals(URI.create(medQueryOgFragment), response.getLocation());
    }
}
