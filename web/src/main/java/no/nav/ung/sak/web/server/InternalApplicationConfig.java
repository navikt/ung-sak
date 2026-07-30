package no.nav.ung.sak.web.server;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import no.nav.ung.sak.web.app.selftest.HealthCheckRestService;
import no.nav.ung.sak.web.app.selftest.SelftestRestTjeneste;
import no.nav.ung.sak.web.server.metrics.PrometheusRestService;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


/**
 * Konfigurer Prometheus
 */
@ApplicationScoped
@ApplicationPath(InternalApplicationConfig.INTERNAL_URI)
public class InternalApplicationConfig extends Application {

    public static final String INTERNAL_URI = "/internal";

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();

        classes.add(PrometheusRestService.class);
        classes.add(HealthCheckRestService.class);
        classes.add(SelftestRestTjeneste.class);
        return Collections.unmodifiableSet(classes);
    }


}
