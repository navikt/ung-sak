package no.nav.ung.sak.web.server.jetty.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.util.Locale;
import java.util.Properties;

public class DatasourceUtil {

    public static HikariDataSource createDatasource(String envVarPrefix, DatasourceRole role, EnvironmentClass environmentClass, int maxPoolSize) {
        String rolePrefix = getRolePrefix(envVarPrefix);
        String password = getProperty(envVarPrefix + ".password");
        if (EnvironmentClass.LOCALHOST.equals(environmentClass)) {
            var config = initConnectionPoolConfig(envVarPrefix, null, maxPoolSize);
            return createDatasource(config, "public", rolePrefix, password);
        } else {
            String dbRole = getRole(rolePrefix, role);
            var config = initConnectionPoolConfig(envVarPrefix, dbRole, maxPoolSize);
            return createDatasource(config, environmentClass.mountPath(), dbRole, password);
        }
    }

    private static String getRole(String rolePrefix, DatasourceRole role) {
        return String.format("%s-%s", rolePrefix, role.name().toLowerCase());
    }

    public static String getDbRole(String datasoureName, DatasourceRole role) {
        return String.format("%s-%s", getRolePrefix(datasoureName), role.name().toLowerCase());
    }

    private static String getRolePrefix(String envVarPrefix) {
        return getProperty(envVarPrefix + ".username");
    }

    private static String getProperty(String key) {
        return System.getProperty(key, System.getenv(key.toUpperCase(Locale.getDefault()).replace('.', '_')));
    }

    private static HikariConfig initConnectionPoolConfig(String envVarPrefix, String dbRole, int maxPoolSize) {
        var config = new HikariConfig();
        config.setJdbcUrl(getProperty(envVarPrefix + ".jdbc.url"));

        config.setMinimumIdle(0);
        config.setValidationTimeout(30000);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaximumPoolSize(maxPoolSize);
        config.setConnectionTestQuery("select 1");
        config.setDriverClassName("org.postgresql.Driver");

        if (dbRole != null) {
            var initSql = String.format("SET ROLE \"%s\"", dbRole);
            config.setConnectionInitSql(initSql);
        }

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
