package no.nav.ung.sak.web.server.jetty.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.util.Locale;
import java.util.Properties;

public class DatasourceUtil {

    public static HikariDataSource createDatasource(String envVarPrefix, EnvironmentClass environmentClass, int maxPoolSize) {
        String username = username(envVarPrefix);
        String password = getProperty(envVarPrefix + ".password");
        HikariConfig config = initConnectionPoolConfig(envVarPrefix, maxPoolSize);
        if (EnvironmentClass.LOCALHOST.equals(environmentClass)) {
            return createDatasource(config, "public", username, password);
        } else {
            return createDatasource(config, environmentClass.mountPath(), username, password);
        }
    }

    public static String getDbRole(String datasoureName, DatasourceRole role) {
        return String.format("%s-%s", username(datasoureName), role.name().toLowerCase());
    }

    private static String username(String envVarPrefix) {
        return getProperty(envVarPrefix + ".username");
    }

    private static String getProperty(String key) {
        return System.getProperty(key, System.getenv(key.toUpperCase(Locale.getDefault()).replace('.', '_')));
    }

    private static HikariConfig initConnectionPoolConfig(String envVarPrefix, int maxPoolSize) {
        var config = new HikariConfig();
        config.setJdbcUrl(getProperty(envVarPrefix + ".jdbc.url"));

        config.setMinimumIdle(0);
        config.setValidationTimeout(30000);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaximumPoolSize(maxPoolSize);
        config.setConnectionTestQuery("select 1");
        config.setDriverClassName("org.postgresql.Driver");

        // optimaliserer inserts for postgres
        var dsProperties = new Properties();
        dsProperties.setProperty("reWriteBatchedInserts", "true");
        dsProperties.setProperty("logServerErrorDetail", "false"); // skrur av batch exceptions som lekker statements i åpen logg
        config.setDataSourceProperties(dsProperties);

        // skrur av autocommit her, da kan vi bypasse dette senere når hibernate setter opp entitymanager for bedre conn mgmt
        config.setAutoCommit(false);

        return config;
    }

    private static HikariDataSource createDatasource(HikariConfig config, String schema, String username, String password) {
        config.setUsername(username);
        config.setPassword(password); // NOSONAR false positive
        if (schema != null && !schema.isEmpty()) {
            config.setSchema(schema);
        }
        return new HikariDataSource(config);
    }
}
