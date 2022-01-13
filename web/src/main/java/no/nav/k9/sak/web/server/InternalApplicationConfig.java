package no.nav.k9.sak.web.server;


import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import io.prometheus.client.hotspot.DefaultExports;
import no.nav.k9.sak.web.server.metrics.PrometheusRestService;

/**
 * Konfigurer Prometheus
 */
@ApplicationScoped
@ApplicationPath(InternalApplicationConfig.INTERNAL_URI)
public class InternalApplicationConfig extends Application {

    public static final String INTERNAL_URI = "/internal";

    public InternalApplicationConfig() {
        //HS QAD siden registry ikke er tilgjengelig når klassen instansieres...
        DefaultExports.initialize();
    }

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(PrometheusRestService.class);
        classes.add(HealthCheckRestService.class);
        return Collections.unmodifiableSet(classes);
    }


}
