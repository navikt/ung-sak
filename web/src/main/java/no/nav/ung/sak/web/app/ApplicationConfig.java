package no.nav.ung.sak.web.app;


import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.ws.rs.ApplicationPath;
import no.nav.openapi.spec.utils.http.DynamicObjectMapperResolverVaryFilter;
import no.nav.openapi.spec.utils.jackson.DynamicJacksonJsonProvider;
import no.nav.openapi.spec.utils.openapi.OpenApiSetupHelper;
import no.nav.ung.sak.web.app.exceptions.KnownExceptionMappers;
import no.nav.ung.sak.web.app.jackson.ObjectMapperResolver;
import no.nav.ung.sak.web.app.tjenester.RestImplementationClasses;
import no.nav.ung.sak.web.server.caching.CacheControlFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import java.util.LinkedHashSet;

@ApplicationPath(ApplicationConfig.API_URI)
public class ApplicationConfig extends ResourceConfig {

    public static final String API_URI = "/api";

    public OpenAPI resolveOpenAPI() {
        final var info = new Info()
            .title("Ung saksbehandling - Saksbehandling for ungdomsprogramytelsen")
            .version("0.2")
            .description("REST grensesnitt for Vedtaksløsningen.");

        final var server =new Server().url("/ung/sak");
        final var openapiSetupHelper = new OpenApiSetupHelper(this, info, server);
        openapiSetupHelper.addResourcePackage("no.nav.ung.sak");
        openapiSetupHelper.addResourcePackage("no.nav.k9");
        try {
            return openapiSetupHelper.resolveOpenAPI();
        } catch (OpenApiConfigurationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public ApplicationConfig() {
        register(DynamicJacksonJsonProvider.class); // Denne må registrerast før anna OpenAPI oppsett for å fungere.
        final var resolvedOpenAPI = resolveOpenAPI();
        register(new no.nav.openapi.spec.utils.openapi.OpenApiResource(resolvedOpenAPI));

        property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);

        registerClasses(new LinkedHashSet<>(new RestImplementationClasses().getImplementationClasses()));

        register(ObjectMapperResolver.class);
        register(DynamicObjectMapperResolverVaryFilter.class);

        registerInstances(new LinkedHashSet<>(new KnownExceptionMappers().getExceptionMappers()));
        register(CacheControlFeature.class);

        property(org.glassfish.jersey.server.ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED, true);

    }

}
