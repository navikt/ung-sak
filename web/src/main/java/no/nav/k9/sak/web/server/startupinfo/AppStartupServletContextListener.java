package no.nav.k9.sak.web.server.startupinfo;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppStartupServletContextListener implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(AppStartupServletContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        startupLogging();
    }

    private void startupLogging() {
        // Henter dependent instance og destroyer etterpå.
        AppStartupInfoLogger appStartupInfoLogger = null;
        try {
            appStartupInfoLogger = CDI.current().select(AppStartupInfoLogger.class).get();
            appStartupInfoLogger.logAppStartupInfo();
        } catch (Exception e) {
            OppstartFeil.FACTORY.uventetExceptionVedOppstart(e).log(logger);
            // men ikke re-throw - vi ønsker ikke at oppstart skal feile pga. feil i logging
        } finally {
            if (appStartupInfoLogger != null) {
                CDI.current().destroy(appStartupInfoLogger);
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        // ikke noe
    }
}
