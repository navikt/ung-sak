package no.nav.k9.sak.web.app;

import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import no.nav.k9.sak.web.app.exceptions.KnownExceptionMappers;
import no.nav.k9.sak.web.app.jackson.JacksonJsonConfig;
import no.nav.k9.sak.web.app.tjenester.RestImplementationClasses;
import no.nav.k9.sak.web.app.tjenester.fordeling.FordelRestTjeneste.PsbInfotrygdFødselsnumre.PsbInfotrygdFødselsnumregMessageBodyReader;
import no.nav.k9.sak.web.app.tjenester.forvaltning.ForvaltningMidlertidigDriftRestTjeneste.OpprettManuellRevurdering.OpprettManuellRevurderingMessageBodyReader;

@ApplicationPath(ApplicationConfig.API_URI)
public class ApplicationConfig extends ResourceConfig {

    public static final String API_URI = "/api";

    public ApplicationConfig() {

        OpenAPI oas = new OpenAPI();
        Info info = new Info()
            .title("K9 saksbehandling - Saksbehandling av kapittel 9 i folketrygden")
            .version("1.0")
            .description("REST grensesnitt for Vedtaksløsningen.");

        oas.info(info)
            .addServersItem(new Server()
                .url("/k9/sak"));
        SwaggerConfiguration oasConfig = new SwaggerConfiguration()
            .openAPI(oas)
            .prettyPrint(true)
            .scannerClass("io.swagger.v3.jaxrs2.integration.JaxrsAnnotationScanner")
            .resourcePackages(Stream.of("no.nav.k9.", "no.nav.k9.sak", "no.nav.k9")
                .collect(Collectors.toSet()));

        try {
            new JaxrsOpenApiContextBuilder<>()
                .openApiConfiguration(oasConfig)
                .buildContext(true)
                .read();
        } catch (OpenApiConfigurationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);

        register(OpenApiResource.class);

        registerClasses(new LinkedHashSet<>(new RestImplementationClasses().getImplementationClasses()));

        register(new JacksonJsonConfig());

        register(new OpprettManuellRevurderingMessageBodyReader());
        register(new PsbInfotrygdFødselsnumregMessageBodyReader());

        registerInstances(new LinkedHashSet<>(new KnownExceptionMappers().getExceptionMappers()));

        property(org.glassfish.jersey.server.ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED, true);
    }

}
