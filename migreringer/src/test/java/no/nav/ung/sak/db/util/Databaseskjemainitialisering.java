package no.nav.ung.sak.db.util;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.eclipse.jetty.plus.jndi.EnvEntry;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.configuration.ClassicConfiguration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Initielt skjemaoppsett + migrering av unittest-skjemaer
 */
public final class Databaseskjemainitialisering {
    private static final AtomicBoolean GUARD_UNIT_TEST_SKJEMAER = new AtomicBoolean();

    static final String USER = "ung_sak_unit";

    private static final DataSource DS = settJdniOppslag(USER);

    public static void main(String[] args) {
        migrerUnittestSkjemaer();
    }

    @SuppressWarnings("resource")
    public static void migrerUnittestSkjemaer() {
        if (GUARD_UNIT_TEST_SKJEMAER.compareAndSet(false, true)) {
            var location = "/db/postgres/";

            ClassicConfiguration conf = new ClassicConfiguration();


            conf.setDataSource(createDs(USER));
            conf.setLocationsAsStrings(location);
            conf.setBaselineOnMigrate(true);
            conf.setCleanDisabled(false);
            Flyway flyway = new Flyway(conf);
            try {
                flyway.migrate();
            } catch (FlywayException fwe) {
                try {
                    // prøver igjen
                    flyway.clean();
                    flyway.migrate();
                } catch (FlywayException fwe2) {
                    throw new IllegalStateException("Migrering feiler", fwe2);
                }
            }
        }
    }

    private static synchronized DataSource settJdniOppslag(String user) {

        var ds = createDs(user);

        try {

            new EnvEntry("jdbc/defaultDS", ds); // NOSONAR
            return ds;
        } catch (NamingException e) {
            throw new IllegalStateException("Feil under registrering av JDNI-entry for default datasource", e); // NOSONAR
        }
    }

    private static HikariDataSource createDs(String user) {
        Objects.requireNonNull(user, "user");
        var cfg = new HikariConfig();
        cfg.setJdbcUrl(System.getProperty("datasource.defaultDS.url", String.format("jdbc:postgresql://127.0.0.1:5432/%s?reWriteBatchedInserts=true", USER)));
        cfg.setUsername(USER);
        cfg.setPassword(USER);
        cfg.setConnectionTimeout(1500);
        cfg.setValidationTimeout(120L * 1000L);
        cfg.setMaximumPoolSize(4);
        cfg.setAutoCommit(false);

        var ds = new HikariDataSource(cfg);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                ds.close();
            }
        }));
        return ds;
    }

    public static void settJdniOppslag() {
        if (DS != null) {
            return;
        }
        settJdniOppslag(USER);
    }

}
