package no.nav.ung.sak.web.app.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import no.nav.openapi.spec.utils.jackson.DynamicJacksonJsonProvider;
import no.nav.ung.sak.kontrakt.FeilDto;
import no.nav.ung.sak.web.app.ApplicationConfig;

class JsonFeilResponsTest {

    private static ApplicationHandler applikasjon;

    @BeforeAll
    static void startApplikasjon() {
        applikasjon = new ApplicationHandler(new ApplicationConfig().register(TestRessurs.class));
    }

    @Test
    void skal_svare_med_json_når_dato_har_feil_format() throws Exception {
        var respons = post("{\"dato\":\"01.01.2025\"}");

        assertThat(respons.getStatus()).isEqualTo(400);
        assertThat(respons.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
        assertThat(respons.getEntity()).isInstanceOf(FeilDto.class);
    }

    @Test
    void skal_svare_med_json_når_json_er_ugyldig() throws Exception {
        var respons = post("{\"dato\":");

        assertThat(respons.getStatus()).isEqualTo(400);
        assertThat(respons.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
        assertThat(respons.getEntity()).isInstanceOf(FeilDto.class);
    }

    @Test
    void skal_fortsatt_deserialisere_gyldig_json() throws Exception {
        var respons = post("{\"dato\":\"2025-01-01\"}");

        assertThat(respons.getStatus()).isEqualTo(200);
        assertThat(respons.getEntity()).isEqualTo("2025-01-01");
    }

    @Test
    void skal_bruke_dynamic_jackson_provider_for_json() {
        var workers = applikasjon.getInjectionManager().getInstance(MessageBodyWorkers.class);
        var propertiesDelegate = new MapPropertiesDelegate();
        var annotasjoner = new Annotation[0];

        var leser = workers.getMessageBodyReader(TestDto.class, TestDto.class, annotasjoner, MediaType.APPLICATION_JSON_TYPE, propertiesDelegate);
        var skriver = workers.getMessageBodyWriter(TestDto.class, TestDto.class, annotasjoner, MediaType.APPLICATION_JSON_TYPE, propertiesDelegate);

        assertThat(leser).isInstanceOf(DynamicJacksonJsonProvider.class);
        assertThat(skriver).isInstanceOf(DynamicJacksonJsonProvider.class);
    }

    private static ContainerResponse post(String body) throws Exception {
        var request = new ContainerRequest(
            URI.create("http://localhost/"),
            URI.create("http://localhost/test"),
            "POST",
            null,
            new MapPropertiesDelegate(),
            applikasjon.getConfiguration());
        request.getHeaders().putSingle(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        request.getHeaders().putSingle(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        request.setEntityStream(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
        return applikasjon.apply(request).get();
    }

    @Path("/test")
    public static class TestRessurs {

        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public String post(TestDto dto) {
            return dto.dato().toString();
        }
    }

    public record TestDto(LocalDate dato) {
    }
}
