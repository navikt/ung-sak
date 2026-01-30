package no.nav.ung.sak.web.app;

import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.ws.rs.ApplicationPath;
import no.nav.openapi.spec.utils.http.DynamicObjectMapperResolverVaryFilter;
import no.nav.openapi.spec.utils.jackson.DynamicJacksonJsonProvider;
import no.nav.openapi.spec.utils.openapi.OpenApiSetupHelper;
import no.nav.openapi.spec.utils.openapi.PrefixStrippingFQNTypeNameResolver;
import no.nav.ung.sak.web.app.exceptions.KnownExceptionMappers;
import no.nav.ung.sak.web.app.jackson.ObjectMapperResolver;
import no.nav.ung.sak.web.app.tjenester.brukerdialog.BrukerdialogOppgaveRestTjeneste;
import no.nav.ung.sak.web.server.caching.CacheControlFeature;
import no.nav.ung.sak.web.server.typedresponse.TypedResponseFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@ApplicationPath(OppgaverApiConfig.API_URI)
public class OppgaverApiConfig extends ResourceConfig {

    private static final Logger LOG = LoggerFactory.getLogger(OppgaverApiConfig.class);
    public static final String API_URI = "/brukerdialog-oppgaver/api";

    public OppgaverApiConfig() {
        LOG.info("Initialiserer: {}", API_URI);

        register(DynamicJacksonJsonProvider.class); // Denne må registrerast før anna OpenAPI oppsett for å fungere.
        final var resolvedOpenAPI = resolveOpenAPI();
        register(new no.nav.openapi.spec.utils.openapi.OpenApiResource(resolvedOpenAPI));

        setApplicationName(OppgaverApiConfig.class.getSimpleName());
        // REST
        registerClasses(getEksternalApplicationClasses());

        register(ObjectMapperResolver.class);
        register(DynamicObjectMapperResolverVaryFilter.class);

        registerInstances(new LinkedHashSet<>(new KnownExceptionMappers().getExceptionMappers()));
        register(CacheControlFeature.class);
        register(TypedResponseFeature.class);

        setProperties(getApplicationProperties());
        LOG.info("Ferdig med initialisering av {}", API_URI);
    }


    public OpenAPI resolveOpenAPI() {
        final var info = new Info()
            .title("Ung-sak brukerdialog")
            .version("2.0")
            .description("REST grensesnitt for brukerdialog oppgaver i Ung-sak. Alle kall må autentiseres med TokenX.");

        final var server =new Server().url("/ung/sak");
        final var openapiSetupHelper = new OpenApiSetupHelper(this, info, server);
        openapiSetupHelper.addResourcePackage("no.nav.ung.sak");
        openapiSetupHelper.addResourcePackage("no.nav.k9");
        openapiSetupHelper.setTypeNameResolver(new PrefixStrippingFQNTypeNameResolver("no.nav."));
        try {
            return openapiSetupHelper.resolveOpenAPI();
        } catch (OpenApiConfigurationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }


    private Set<Class<?>> getEksternalApplicationClasses() {
        // eksponert grensesnitt
        return Set.of(BrukerdialogOppgaveRestTjeneste.class);
    }

    private Map<String, Object> getApplicationProperties() {
        Map<String, Object> properties = new HashMap<>();
        // Ref Jersey doc
        properties.put(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        properties.put(ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED, true);
        return properties;
    }

}
