package no.nav.ung.sak.web.server.jetty;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.configuration.ClassicConfiguration;

class DevDatabaseScript {
    private static final String location = "classpath:/db/postgres/";

    static void clean(DataSource dataSource) {
        ClassicConfiguration conf = new ClassicConfiguration();
        conf.setDataSource(dataSource);
        conf.setLocationsAsStrings(location);
        conf.setBaselineOnMigrate(true);
        conf.setCleanDisabled(false);
        Flyway flyway = new Flyway(conf);
        try {
            flyway.clean();
        } catch (FlywayException fwe) {
            throw new IllegalStateException("Migrering feiler", fwe);
        }
    }
}
