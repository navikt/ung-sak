package no.nav.k9.sak.web.server.startupinfo;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.CDI;

import org.jboss.resteasy.annotations.Query;
import org.jboss.weld.util.reflection.Formats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Dependent scope siden vi lukker denne når vi er ferdig. */
@Dependent
class AppStartupInfoLogger {

    private static final Logger logger = LoggerFactory.getLogger(AppStartupInfoLogger.class);

    AppStartupInfoLogger() {
        // for CDI proxy
    }

    synchronized void logAppStartupInfo() {
        log("********" + " " + "OPPSTARTSINFO" + " " + "start:" + " " + "********");
        logVersjoner();
        log("********" + " " + "OPPSTARTSINFO" + " " + "slutt." + " " + "********");
    }

    private void logVersjoner() {
        // Noen biblioteker er bundlet med jboss og kan skape konflikter, eller jboss overstyrer vår overstyring via modul classpath
        // her logges derfor hva som er effektivt tilgjengelig av ulike biblioteker som kan være påvirket ved oppstart
        log("Bibliotek: Hibernate: {}", org.hibernate.Version.getVersionString());
        log("Bibliotek: Weld: {}", Formats.version(null));
        log("Bibliotek: CDI: {}", CDI.class.getPackage().getImplementationVendor() + ":" + CDI.class.getPackage().getSpecificationVersion());
        log("Bibliotek: Resteasy: {}", Query.class.getPackage().getImplementationVersion()); // tilfeldig valgt Resteasy klasse
    }

    private void log(String msg, Object... args) {
        if (args == null || args.length == 0) {
            // skiller ut ellers logger logback ekstra paranteser og fnutter for tomme args
            logger.info(msg);
        } else {
            logger.info(msg, args);
        }
    }

}
