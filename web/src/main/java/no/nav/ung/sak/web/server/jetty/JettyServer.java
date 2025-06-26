package no.nav.ung.sak.web.server.jetty;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.security.auth.message.config.AuthConfigFactory;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.sikkerhet.jaspic.OidcAuthModule;
import no.nav.ung.sak.web.app.ApplicationConfig;
import no.nav.ung.sak.web.app.FrontendApiConfig;
import no.nav.ung.sak.web.server.InternalApplicationConfig;
import no.nav.ung.sak.web.server.jetty.db.DatabaseScript;
import no.nav.ung.sak.web.server.jetty.db.DatasourceUtil;
import no.nav.ung.sak.web.server.jetty.db.EnvironmentClass;
import org.eclipse.jetty.ee9.security.ConstraintSecurityHandler;
import org.eclipse.jetty.ee9.security.jaspi.DefaultAuthConfigFactory;
import org.eclipse.jetty.ee9.security.jaspi.JaspiAuthenticatorFactory;
import org.eclipse.jetty.ee9.security.jaspi.provider.JaspiAuthConfigProvider;
import org.eclipse.jetty.ee9.servlet.ServletContainerInitializerHolder;
import org.eclipse.jetty.ee9.webapp.MetaInfConfiguration;
import org.eclipse.jetty.ee9.webapp.WebAppContext;
import org.eclipse.jetty.plus.jndi.EnvEntry;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.jaas.JAASLoginService;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.glassfish.jersey.servlet.init.JerseyServletContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class JettyServer {

    public static final AtomicBoolean KILL_APPLICATION = new AtomicBoolean(false);

    // Prefix for database miljøvariabler. Konfigurert i nais-filene. Blir konvertert til store bokstaver
    public static final String envVarPrefix = "db";

    private static final Environment ENV = Environment.current();
    private static final Logger log = LoggerFactory.getLogger(JettyServer.class);
    private AppKonfigurasjon appKonfigurasjon;

    private static byte[] EMERGENCY_HEAP_SPACE = new byte[8192000];


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


        final JettyServer jettyServer;

        if (args.length > 0) {
            int serverPort = Integer.parseUnsignedInt(args[0]);
            jettyServer = new JettyServer(serverPort);
        } else {
            jettyServer = new JettyServer();
        }

        taNedApplikasjonVedError();

        jettyServer.bootStrap();
    }

    private static void taNedApplikasjonVedError() {
            Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
                // Frigir minne for å sikre at vi kan logge exception
                EMERGENCY_HEAP_SPACE = null;
                log.error("Uncaught exception for thread " + t.getId(), e);

                if (e instanceof Error) {
                    KILL_APPLICATION.set(true);
                }

            });
    }

    private void start(AppKonfigurasjon appKonfigurasjon) throws Exception {
        // https://jetty.org/docs/jetty/12/programming-guide/arch/threads.html
        QueuedThreadPool threadPool = new QueuedThreadPool();

        Server server = new Server(threadPool);
        server.setConnectors(createConnectors(appKonfigurasjon, server).toArray(new Connector[]{}));

        WebAppContext webAppContext = createContext(appKonfigurasjon, server);
        server.setHandler(new Handler.Sequence(
            new ClearMdcHandler(),
            webAppContext.get()
        ));

        server.addEventListener(new JettyServerLifeCyleListener());
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
        // må være << antall db connectdions. Summen av runner threads + kall fra ulike
        // løsninger bør ikke overgå antall conns (vi isåfall kunne
        // medføre connection timeouts)
        System.setProperty("task.manager.runner.threads", "7");

        //øker kø-størrelse og antall task som polles om gangen, gjør at systemet oppnår bedre ytelse når det finnes mange klare tasks
        System.setProperty("task.manager.tasks.queue.size", "20");
        System.setProperty("task.manager.polling.tasks.size", "10");
        System.setProperty("task.manager.polling.scrolling.select.size", "10");

        if (ENV.isDev()) {
            System.setProperty("task.manager.auto.traceparent", "true");
        }
    }

    private void konfigurerJndi() throws Exception {
        new EnvEntry("jdbc/defaultDS",
            DatasourceUtil.createDatasource(envVarPrefix, getEnvironmentClass(), 15));
    }

    protected void konfigurerSikkerhet() {
        var factory = new DefaultAuthConfigFactory();

        factory.registerConfigProvider(new JaspiAuthConfigProvider(new OidcAuthModule()),
            "HttpServlet",
            "server " + appKonfigurasjon.getContextPath(),
            "OIDC Authentication");

        AuthConfigFactory.setFactory(factory);
    }

    protected void migrerDatabaser() throws IOException {
        EnvironmentClass environmentClass = getEnvironmentClass();

        try (HikariDataSource migreringDs = DatasourceUtil.createDatasource(envVarPrefix, environmentClass, 2)) {
            var flywayRepairOnFail = Boolean.valueOf(ENV.getProperty("FLYWAY_REPAIR_ON_FAIL", "false"));
            DatabaseScript.migrate(migreringDs, null, flywayRepairOnFail);
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
    protected WebAppContext createContext(AppKonfigurasjon appKonfigurasjon, Server server) throws IOException {
        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setParentLoaderPriority(true);

        // må hoppe litt bukk for å hente web.xml fra classpath i stedet for fra filsystem.

        String descriptor = ResourceFactory.of(server).newClassLoaderResource("/WEB-INF/web.xml").getURI().toURL().toExternalForm();
        webAppContext.setDescriptor(descriptor);
        webAppContext.setBaseResource(createResourceCollection(server));
        webAppContext.setContextPath(appKonfigurasjon.getContextPath());

        webAppContext.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");

        /*
         * lar jetty scanne flere jars for web resources (eks. WebFilter/WebListener
         * annotations),
         * men bare de som matchr pattern for raskere oppstart
         */
        webAppContext.setAttribute(MetaInfConfiguration.WEBINF_JAR_PATTERN, "^.*jersey-.*.jar$|^.*felles-sikkerhet.*.jar$");
        webAppContext.setSecurityHandler(createSecurityHandler());

        final ServletContainerInitializerHolder jerseyHolder = webAppContext.addServletContainerInitializer(new JerseyServletContainerInitializer());
        jerseyHolder.addStartupClasses(getJaxRsApplicationClasses());

        webAppContext.setThrowUnavailableOnStartupException(true);

        return webAppContext;
    }

    protected Class<?>[] getJaxRsApplicationClasses() {
        return new Class<?>[]{
            no.nav.k9.felles.oidc.OidcApplication.class,
            ApplicationConfig.class,
            InternalApplicationConfig.class,
            FrontendApiConfig.class
        };
    }

    protected HttpConfiguration createHttpConfiguration() {
        // Create HTTP Config
        HttpConfiguration httpConfig = new HttpConfiguration();

        // Add support for X-Forwarded headers
        httpConfig.addCustomizer(new org.eclipse.jetty.server.ForwardedRequestCustomizer());

        return httpConfig;

    }

    private org.eclipse.jetty.ee9.security.SecurityHandler createSecurityHandler() {
        ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
        securityHandler.setAuthenticatorFactory(new JaspiAuthenticatorFactory());

        JAASLoginService loginService = new JAASLoginService();
        loginService.setName("jetty-login");
        loginService.setLoginModuleName("jetty-login");
        loginService.setIdentityService(new DefaultIdentityService());
        securityHandler.setLoginService(loginService);

        return securityHandler;
    }

    @SuppressWarnings("resource")
    protected Resource createResourceCollection(Server server) {
        return ResourceFactory.combine(
            ResourceFactory.of(server).newClassLoaderResource("META-INF/resources/webjars/"),
            ResourceFactory.of(server).newClassLoaderResource("/web"));
    }



    /**
     * brukes for å slette tilstand i MDC på starten av en request
     */
    private static class ClearMdcHandler extends Handler.Abstract{
        @Override
        public boolean handle(Request request, Response response, Callback callback) {
            MDC.clear();
            return false;
        }
    }

}
