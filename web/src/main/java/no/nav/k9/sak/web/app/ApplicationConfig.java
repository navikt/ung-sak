package no.nav.k9.sak.web.app;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import no.nav.k9.sak.web.app.exceptions.ConstraintViolationMapper;
import no.nav.k9.sak.web.app.exceptions.GeneralRestExceptionMapper;
import no.nav.k9.sak.web.app.exceptions.JsonMappingExceptionMapper;
import no.nav.k9.sak.web.app.exceptions.JsonParseExceptionMapper;
import no.nav.k9.sak.web.app.jackson.JacksonJsonConfig;
import no.nav.k9.sak.web.app.tjenester.RestImplementationClasses;


@ApplicationPath(ApplicationConfig.API_URI)
public class ApplicationConfig extends Application {

    public static final String API_URI = "/api";

    private Set<Object> singletons;

    public ApplicationConfig() {

        singletons = Set.of(new JacksonJsonConfig());

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
    }

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>(new RestImplementationClasses().getImplementationClasses());

        classes.add(OpenApiResource.class);

        classes.add(ConstraintViolationMapper.class);
        classes.add(JsonMappingExceptionMapper.class);
        classes.add(JsonParseExceptionMapper.class);
        classes.add(GeneralRestExceptionMapper.class);
        classes.add(JacksonJsonConfig.class);

        return Collections.unmodifiableSet(classes);
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }
}
