package no.nav.ung.sak.web.app.selftest;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.sak.web.app.selftest.checks.ExtHealthCheck;

@ApplicationScoped
public class Selftests {

    private static final Logger LOGGER = LoggerFactory.getLogger(Selftests.class);
    private static final String BUILD_PROPERTIES = "build.properties";
    private HealthCheckRegistry registry;
    private Map<String, Boolean> erKritiskTjeneste = new HashMap<>();
    private Instance<ExtHealthCheck> healthChecks;
    private volatile boolean hasSetupChecks;
    private String applicationName;

    @Inject
    public Selftests(HealthCheckRegistry registry,
                     @Any Instance<ExtHealthCheck> healthChecks,
                     @KonfigVerdi(value = "NAIS_APP_NAME", defaultVerdi = "ung-sak") String applicationName) {

        this.registry = registry;
        this.healthChecks = healthChecks;
        this.applicationName = applicationName;
    }

    Selftests() {
        // for CDI proxy
    }

    public SelftestResultat run() {
        return run(false);
    }

    public SelftestResultat run(boolean kunKritiskeTester) {
        setupChecks();

        SelftestResultat samletResultat = new SelftestResultat();
        populateBuildtimeProperties(samletResultat);
        samletResultat.setTimestamp(LocalDateTime.now());

        for (String name : registry.getNames()) {
            Boolean kritiskTjeneste = erKritiskTjeneste.get(name);
            if (kunKritiskeTester && !kritiskTjeneste) {
                continue;
            }
            HealthCheck.Result result = registry.runHealthCheck(name);
            if (kritiskTjeneste) {
                samletResultat.leggTilResultatForKritiskTjeneste(result);
            } else {
                samletResultat.leggTilResultatForIkkeKritiskTjeneste(result);
            }
        }
        return samletResultat;
    }

    private synchronized void setupChecks() {
        if (!hasSetupChecks) {
            for (ExtHealthCheck healthCheck : healthChecks) {
                registrer(healthCheck);
            }
            hasSetupChecks = true;
        }
    }

    private void registrer(ExtHealthCheck healthCheck) {
        String name = healthCheck.getClass().getName();
        if (erKritiskTjeneste.containsKey(name)) {
            throw SelftestFeil.FACTORY.dupliserteSelftestNavn(name).toException();
        }
        registry.register(name, healthCheck);
        erKritiskTjeneste.put(name, healthCheck.erKritiskTjeneste());
    }

    private void populateBuildtimeProperties(SelftestResultat samletResultat) {
        String version = null;
        String revision = null;
        String timestamp = null;

        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(BUILD_PROPERTIES)) {
            Properties prop = new Properties();
            if (is == null) {
                // det er forventet at build.properties-filen ikke er tilgjengelig lokalt.
                // unngår derfor å forsøke å lese den.
                return;
            }
            prop.load(is);
            version = prop.getProperty("version");
            revision = prop.getProperty("revision");
            timestamp = prop.getProperty("timestamp");
        } catch (IOException e) {
            SelftestFeil.FACTORY.klarteIkkeÅLeseBuildTimePropertiesFil(e).log(LOGGER);
            // Ikke re-throw - dette er ikke kritisk
        }

        samletResultat.setVersion(buildtimePropertyValueIfNull(version));
        samletResultat.setApplication(applicationName);
        samletResultat.setRevision(buildtimePropertyValueIfNull(revision));
        samletResultat.setBuildTime(buildtimePropertyValueIfNull(timestamp));
    }

    private String buildtimePropertyValueIfNull(String value) {
        String newValue = value;
        if (newValue == null) {
            newValue = "?.?.?";
        }
        return newValue;
    }

}
