package no.nav.k9.sak.web.server.jetty;

import org.eclipse.jetty.util.component.LifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.inject.spi.CDI;
import no.nav.k9.felles.apptjeneste.AppServiceHandler;

public class JettyServerLifeCyleListener implements LifeCycle.Listener {

    private static final Logger log = LoggerFactory.getLogger(JettyServerLifeCyleListener.class);

    @Override
    public void lifeCycleStarted(LifeCycle event) {
        for (AppServiceHandler ash : findAppServiceHandlers()) {
            log.info("Starting " + ash.getClass().getSimpleName());
            ash.start();
        }
    }

    @Override
    public void lifeCycleStopping(LifeCycle event) {
        for (AppServiceHandler ash : findAppServiceHandlers()) {
            log.info("Stopping " + ash.getClass().getSimpleName());
            try {
                ash.stop();
            } catch (Exception e) {
                log.error("Exception while stopping AppServiceHandler.", e);
            }
        }
    }
    
    private static Iterable<AppServiceHandler> findAppServiceHandlers() {
        return CDI.current().select(AppServiceHandler.class);
    }
}
