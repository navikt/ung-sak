package no.nav.k9.sak.web.app;

import java.util.Set;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath(FrontendApiConfig.API_URI)
public class FrontendApiConfig extends Application {

    public static final String API_URI = "/resource";

    public FrontendApiConfig() {
    }

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(FrontendLoginResource.class);
    }
}
