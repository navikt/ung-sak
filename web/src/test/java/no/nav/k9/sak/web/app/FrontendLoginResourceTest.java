package no.nav.k9.sak.web.app;

import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.core.Response;

import org.junit.Test;

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
}
