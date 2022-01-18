package no.nav.k9.sak.web.server.jetty;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.eclipse.jetty.jaas.JAASLoginService;
import org.eclipse.jetty.plus.jndi.EnvEntry;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.jaspi.DefaultAuthConfigFactory;
import org.eclipse.jetty.security.jaspi.JaspiAuthenticatorFactory;
import org.eclipse.jetty.security.jaspi.provider.JaspiAuthConfigProvider;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.webapp.MetaData;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import jakarta.security.auth.message.config.AuthConfigFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.oidc.OidcApplication;
import no.nav.k9.felles.sikkerhet.jaspic.OidcAuthModule;
import no.nav.k9.sak.web.app.ApplicationConfig;
import no.nav.k9.sak.web.server.jetty.db.DatabaseScript;
import no.nav.k9.sak.web.server.jetty.db.DatasourceRole;
import no.nav.k9.sak.web.server.jetty.db.DatasourceUtil;
import no.nav.k9.sak.web.server.jetty.db.EnvironmentClass;

public class JettyServer {

    /**
     * Legges først slik at alltid resetter context før prosesserer nye requests.
     * Kjøres først så ikke risikerer andre har satt
     * Request#setHandled(true).
     */
    static final class ResetLogContextHandler extends AbstractHandler {
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request,
                HttpServletResponse response) {
            MDC.clear();
        }
    }

    private static final Environment ENV = Environment.current();
    private static final Logger log = LoggerFactory.getLogger(JettyServer.class);
    private AppKonfigurasjon appKonfigurasjon;

    public JettyServer() {
        this(new JettyWebKonfigurasjon());
    }

    public JettyServer(int serverPort) {
        this(new JettyWebKonfigurasjon(serverPort));
    }

    JettyServer(AppKonfigurasjon appKonfigurasjon) {
        this.appKonfigurasjon = appKonfigurasjon;
    }

    public static void main(String[] args) throws Exception {
        JettyServer jettyServer;

        if (args.length > 0) {
            int serverPort = Integer.parseUnsignedInt(args[0]);
            jettyServer = new JettyServer(serverPort);
        } else {
            jettyServer = new JettyServer();
        }
        jettyServer.bootStrap();
    }

    private void start(AppKonfigurasjon appKonfigurasjon) throws Exception {
        Server server = new Server(appKonfigurasjon.getServerPort());
        server.setConnectors(createConnectors(appKonfigurasjon, server).toArray(new Connector[] {}));

        var handlers = new HandlerList(new ResetLogContextHandler(), createContext(appKonfigurasjon));
        server.setHandler(handlers);
        server.start();
        server.join();
    }

    protected void bootStrap() throws Exception { // NOSONAR
        konfigurer();
        migrerDatabaser();
        start(appKonfigurasjon);
    }

    private void konfigurer() throws Exception { // NOSONAR
        konfigurerMiljø();
        konfigurerSikkerhet();
        konfigurerJndi();
    }

    protected void konfigurerMiljø() {
    }

    private void konfigurerJndi() throws Exception {
        // må være << antall db connectdions. Summen av runner threads + kall fra ulike
        // løsninger bør ikke overgå antall conns (vi isåfall kunne
        // medføre connection timeouts)
        System.setProperty("task.manager.runner.threads", "7");
        new EnvEntry("jdbc/defaultDS",
                DatasourceUtil.createDatasource("defaultDS", DatasourceRole.ADMIN, getEnvironmentClass(), 15));
    }

    protected void konfigurerSikkerhet() {
        var factory = new DefaultAuthConfigFactory();

        factory.registerConfigProvider(new JaspiAuthConfigProvider(new OidcAuthModule()),
                "HttpServlet",
                "server /k9/sak",
                "OIDC Authentication");

        AuthConfigFactory.setFactory(factory);

    }

    protected void migrerDatabaser() throws IOException {
        EnvironmentClass environmentClass = getEnvironmentClass();
        String initSql = String.format("SET ROLE \"%s\"", DatasourceUtil.getDbRole("defaultDS", DatasourceRole.ADMIN));
        if (EnvironmentClass.LOCALHOST.equals(environmentClass)) {
            // TODO: Ønsker egentlig ikke dette, men har ikke satt opp skjema lokalt
            // til å ha en admin bruker som gjør migrering og en annen som gjør CRUD
            // operasjoner
            initSql = null;
        }
        DataSource migreringDs = DatasourceUtil.createDatasource("defaultDS", DatasourceRole.ADMIN, environmentClass,
                1);
        try {
            DatabaseScript.migrate(migreringDs, initSql);
        } finally {
            try {
                migreringDs.getConnection().close();
            } catch (SQLException e) {
                log.warn("Klarte ikke stenge connection etter migrering", e);
            }
        }
    }

    protected EnvironmentClass getEnvironmentClass() {
        return EnvironmentUtil.getEnvironmentClass();
    }

    @SuppressWarnings("resource")
    protected List<Connector> createConnectors(AppKonfigurasjon appKonfigurasjon, Server server) {
        List<Connector> connectors = new ArrayList<>();
        ServerConnector httpConnector = new ServerConnector(server,
                new HttpConnectionFactory(createHttpConfiguration()));
        httpConnector.setPort(appKonfigurasjon.getServerPort());
        connectors.add(httpConnector);

        return connectors;
    }

    @SuppressWarnings("resource")
    protected WebAppContext createContext(AppKonfigurasjon appKonfigurasjon) throws IOException {
        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setParentLoaderPriority(true);

        // må hoppe litt bukk for å hente web.xml fra classpath i stedet for fra
        // filsystem.
        String descriptor;
        try (var resource = Resource.newClassPathResource("/WEB-INF/web.xml")) {
            descriptor = resource.getURI().toURL().toExternalForm();
        }
        webAppContext.setDescriptor(descriptor);
        webAppContext.setBaseResource(createResourceCollection());
        webAppContext.setContextPath(appKonfigurasjon.getContextPath());

        webAppContext.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");

        /*
         * lar jetty scanne flere jars for web resources (eks. WebFilter/WebListener
         * annotations),
         * men bare de som matchr pattern for raskere oppstart
         */
        webAppContext.setAttribute("org.eclipse.jetty.server.webapp.WebInfIncludeJarPattern",
                "^.*jersey-.*.jar$|^.*felles-sikkerhet.*.jar$");
        webAppContext.setSecurityHandler(createSecurityHandler());

        updateMetaData(webAppContext.getMetaData());
        webAppContext.setThrowUnavailableOnStartupException(true);
        return webAppContext;
    }

    protected HttpConfiguration createHttpConfiguration() {
        // Create HTTP Config
        HttpConfiguration httpConfig = new HttpConfiguration();

        // Add support for X-Forwarded headers
        httpConfig.addCustomizer(new org.eclipse.jetty.server.ForwardedRequestCustomizer());

        return httpConfig;

    }

    private SecurityHandler createSecurityHandler() {
        ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
        securityHandler.setAuthenticatorFactory(new JaspiAuthenticatorFactory());

        JAASLoginService loginService = new JAASLoginService();
        loginService.setName("jetty-login");
        loginService.setLoginModuleName("jetty-login");
        loginService.setIdentityService(new DefaultIdentityService());
        securityHandler.setLoginService(loginService);

        return securityHandler;
    }

    private void updateMetaData(MetaData metaData) {
        // Find path to class-files while starting jetty from development environment.
        List<Class<?>> appClasses = getWebInfClasses();

        List<Resource> resources = appClasses.stream()
                .map(c -> Resource.newResource(c.getProtectionDomain().getCodeSource().getLocation()))
                .distinct()
                .collect(Collectors.toList());

        metaData.setWebInfClassesResources(resources);
    }

    protected List<Class<?>> getWebInfClasses() {
        return Arrays.asList(ApplicationConfig.class, OidcApplication.class);
    }

    @SuppressWarnings("resource")
    protected ResourceCollection createResourceCollection() {
        return new ResourceCollection(
                Resource.newClassPathResource("META-INF/resources/webjars/"),
                Resource.newClassPathResource("/web"));
    }

}
