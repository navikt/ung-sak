package no.nav.foreldrepenger.web.app;

import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("/jetty")
public class JettyTestApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(DummyRestTjeneste.class,
            DummyFptilbakeRestTjeneste.class);
    }
}
