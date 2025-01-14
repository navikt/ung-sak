package no.nav.ung.sak.web.app.selftest;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.health.SharedHealthCheckRegistries;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
class HealthCheckRegistryProducer {

    private static final String SELFTEST_HEALTHCHECK_REGISTRY_NAME = "healthchecks";

    @Produces
    @ApplicationScoped
    public HealthCheckRegistry getHealthCheckRegistry() {
        return SharedHealthCheckRegistries.getOrCreate(SELFTEST_HEALTHCHECK_REGISTRY_NAME);
    }
}
