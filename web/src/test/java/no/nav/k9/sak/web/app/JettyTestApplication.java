package no.nav.k9.sak.web.app;

import java.util.Set;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/jetty")
public class JettyTestApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(DummyRestTjenesteForTest.class,
            DummyFptilbakeRestTjeneste.class);
    }
}
