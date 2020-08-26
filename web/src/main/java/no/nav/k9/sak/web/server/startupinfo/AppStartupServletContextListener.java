package no.nav.k9.sak.web.server.startupinfo;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.kafka.streams.KafkaStreams;
import org.jboss.resteasy.annotations.Query;
import org.jboss.weld.util.reflection.Formats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppStartupServletContextListener implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(AppStartupServletContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        startupLogging();
    }

    private void startupLogging() {
        log("******** OPPSTARTSINFO start: ********");
        logVersjoner();
        log("******** OPPSTARTSINFO slutt. ********");
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        // ikke noe
    }

    private void logVersjoner() {
        // Noen biblioteker er bundlet med jboss og kan skape konflikter, eller jboss overstyrer vår overstyring via modul classpath
        // her logges derfor hva som er effektivt tilgjengelig av ulike biblioteker som kan være påvirket ved oppstart
        log("Bibliotek: Hibernate: {}", org.hibernate.Version.getVersionString());
        log("Bibliotek: Weld: {}", Formats.version(null));
        log("Bibliotek: CDI: {}", CDI.class.getPackage().getImplementationVendor() + ":" + CDI.class.getPackage().getSpecificationVersion());
        log("Bibliotek: Resteasy: {}", Query.class.getPackage().getImplementationVersion()); // tilfeldig valgt Resteasy klasse
        log("Bibliotek: KafkaStreams: {}", KafkaStreams.class.getPackage().getImplementationVersion());
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
