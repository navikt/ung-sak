package no.nav.ung.sak.web.app;


import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.ws.rs.ApplicationPath;
import no.nav.ung.sak.web.app.exceptions.KnownExceptionMappers;
import no.nav.ung.sak.web.app.jackson.CustomJacksonJsonProvider;
import no.nav.ung.sak.web.app.jackson.ObjectMapperResolver;
import no.nav.ung.sak.web.app.tjenester.RestImplementationClasses;
import no.nav.ung.sak.web.server.caching.CacheControlFeature;

@ApplicationPath(ApplicationConfig.API_URI)
public class ApplicationConfig extends ResourceConfig {

    public static final String API_URI = "/api";

    public static OpenAPI resolveOpenAPI() {
        OpenAPI oas = new OpenAPI();
        Info info = new Info()
            .title("K9 saksbehandling - Saksbehandling av kapittel 9 i folketrygden")
            .version("1.0")
            .description("REST grensesnitt for Vedtaksløsningen.");

        oas.info(info)
            .addServersItem(new Server()
                .url("/k9/sak"));
        // Alle properties som kan vere null skal ha nullable satt
        ModelConverters.getInstance().addConverter(new NullablePropertyConverter());
        SwaggerConfiguration oasConfig = new SwaggerConfiguration()
            .openAPI(oas)
            .prettyPrint(true)
            .readerClass(CustomResponseTypeAdjustingReader.class.getCanonicalName())
            .scannerClass("io.swagger.v3.jaxrs2.integration.JaxrsAnnotationScanner")
            .resourcePackages(Stream.of("no.nav.ung.sak", "no.nav.k9")
                .collect(Collectors.toSet()));

        try {
            return new JaxrsOpenApiContextBuilder<>()
                .openApiConfiguration(oasConfig)
                .buildContext(true)
                .read();
        } catch (OpenApiConfigurationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public ApplicationConfig() {
        ApplicationConfig.resolveOpenAPI();

        property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);

        register(OpenApiResource.class);

        registerClasses(new LinkedHashSet<>(new RestImplementationClasses().getImplementationClasses()));

        register(ObjectMapperResolver.class);
        register(CustomJacksonJsonProvider.class);

        registerInstances(new LinkedHashSet<>(new KnownExceptionMappers().getExceptionMappers()));
        register(CacheControlFeature.class);

        property(org.glassfish.jersey.server.ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED, true);

    }

}
