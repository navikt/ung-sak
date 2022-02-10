package no.nav.k9.sak.web.server.jetty;

import org.eclipse.jetty.util.component.LifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.inject.spi.CDI;
import no.nav.k9.prosesstask.impl.TaskManager;

public class JettyServerLifeCyleListener implements LifeCycle.Listener {

    private static final Logger log = LoggerFactory.getLogger(JettyServerLifeCyleListener.class);

    @Override
    public void lifeCycleStarted(LifeCycle event) {
        log.info("Starting task consumption");
        var taskManager = CDI.current().select(TaskManager.class).get();
        taskManager.start();
    }

    @Override
    public void lifeCycleStopping(LifeCycle event) {
        log.info("Shutting down tasks");
        var taskManager = CDI.current().select(TaskManager.class).get();
        taskManager.stop();
    }
}
