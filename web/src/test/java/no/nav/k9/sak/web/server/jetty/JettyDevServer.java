package no.nav.k9.sak.web.server.jetty;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.sak.web.app.JettyTestApplication;
import no.nav.k9.sak.web.server.jetty.db.DatasourceRole;
import no.nav.k9.sak.web.server.jetty.db.DatasourceUtil;
import no.nav.k9.sak.web.server.jetty.db.EnvironmentClass;

public class JettyDevServer extends JettyServer {

    private static final Logger log = LoggerFactory.getLogger(JettyDevServer.class);

    public JettyDevServer() {
        super(new JettyDevKonfigurasjon());
    }

    public static void main(String[] args) throws Exception {
        JettyDevServer devServer = new JettyDevServer();
        devServer.bootStrap();
    }

    private static String initCryptoStoreConfig(String storeName, String storeProperty, String storePasswordProperty,
            String defaultPassword) {
        String defaultLocation = getProperty("user.home", ".") + "/.modig/" + storeName + ".jks";

        String storePath = getProperty(storeProperty, defaultLocation);
        File storeFile = new File(storePath);
        if (!storeFile.exists()) {
            throw new IllegalStateException("Finner ikke " + storeName + " i " + storePath
                    + "\n\tKonfigurer enten som System property \'" + storeProperty + "\' eller environment variabel \'"
                    + storeProperty.toUpperCase().replace('.', '_') + "\'");
        }
        String password = getProperty(storePasswordProperty, defaultPassword);
        if (password == null) {
            throw new IllegalStateException(
                    "Passord for å aksessere store " + storeName + " i " + storePath + " er null");
        }

        System.setProperty(storeProperty, storeFile.getAbsolutePath());
        System.setProperty(storePasswordProperty, password);

        // Aiven:
        System.setProperty("KAFKA_TRUSTSTORE_PATH", storeFile.getAbsolutePath());
        System.setProperty("KAFKA_KEYSTORE_PATH", storeFile.getAbsolutePath());
        System.setProperty("KAFKA_CREDSTORE_PASSWORD", password);

        return storePath;
    }

    private static String getProperty(String key, String defaultValue) {
        String val = System.getProperty(key, defaultValue);
        if (val == null) {
            val = System.getenv(key.toUpperCase().replace('.', '_'));
            val = val == null ? defaultValue : val;
        }
        return val;
    }

    @Override
    protected void migrerDatabaser() throws IOException {
        try {
            super.migrerDatabaser();
        } catch (IllegalStateException e) {
            log.info("Migreringer feilet, cleaner og prøver på nytt for lokal db.");
            DataSource migreringDs = DatasourceUtil.createDatasource("defaultDS", DatasourceRole.ADMIN,
                    getEnvironmentClass(), 1);
            try {
                DevDatabaseScript.clean(migreringDs);
            } finally {
                try {
                    migreringDs.getConnection().close();
                } catch (SQLException sqlException) {
                    log.warn("Klarte ikke stenge connection etter migrering", sqlException);
                }
            }
            super.migrerDatabaser();
        }
    }

    @Override
    protected EnvironmentClass getEnvironmentClass() {
        return EnvironmentClass.LOCALHOST;
    }

    @Override
    protected void konfigurerMiljø() {
        System.setProperty("develop-local", "true");
        PropertiesUtils.initProperties();

        JettyDevDbKonfigurasjon konfig = new JettyDevDbKonfigurasjon();
        System.setProperty("defaultDS.url", konfig.getUrl());
        System.setProperty("defaultDS.username", konfig.getUser()); // benyttes kun hvis vault.enable=false
        System.setProperty("defaultDS.password", konfig.getPassword()); // benyttes kun hvis vault.enable=false
    }

    @Override
    protected void konfigurerSikkerhet() {
        super.konfigurerSikkerhet();

        /**
         * @see https://docs.oracle.com/en/java/javase/11/security/java-secure-socket-extension-jsse-reference-guide.html
         */

        // truststore avgjør hva vi stoler på av sertifikater når vi gjør utadgående TLS kall
        initCryptoStoreConfig("truststore", "javax.net.ssl.trustStore", "javax.net.ssl.trustStorePassword", "changeit");
        initCryptoStoreConfig("keystore", "javax.net.ssl.keyStore", "javax.net.ssl.keyStorePassword",
                "devillokeystore1234");
    }

    @SuppressWarnings("resource")
    @Override
    protected List<Connector> createConnectors(AppKonfigurasjon appKonfigurasjon, Server server) {
        List<Connector> connectors = super.createConnectors(appKonfigurasjon, server);

        var sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStorePath(System.getProperty("javax.net.ssl.keyStore"));
        sslContextFactory.setKeyStorePassword(System.getProperty("javax.net.ssl.keyStorePassword"));
        sslContextFactory.setKeyManagerPassword(System.getProperty("javax.net.ssl.keyStorePassword"));

        HttpConfiguration https = createHttpConfiguration();
        https.addCustomizer(new SecureRequestCustomizer());

        ServerConnector sslConnector = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory, "http/1.1"),
                new HttpConnectionFactory(https));
        sslConnector.setPort(appKonfigurasjon.getSslPort());
        connectors.add(sslConnector);

        return connectors;
    }

    @Override
    protected WebAppContext createContext(AppKonfigurasjon appKonfigurasjon) throws IOException {
        WebAppContext webAppContext = super.createContext(appKonfigurasjon);
        // https://www.eclipse.org/jetty/documentation/9.4.x/troubleshooting-locked-files-on-windows.html
        webAppContext.setInitParameter("org.eclipse.jetty.servlet.Default.useFileMappedBuffer", "false");
        return webAppContext;
    }

    @Override
    protected Class<?>[] getJaxRsApplicationClasses() {
        final List<Class<?>> classes = new ArrayList<>(List.of(super.getJaxRsApplicationClasses()));
        classes.add(JettyTestApplication.class);
        return classes.toArray(new Class<?>[0]);
    }

}
