package no.nav.k9.sak.web.app.tjenester;

import jakarta.inject.Inject;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import no.nav.k9.felles.integrasjon.sensu.SensuKlient;
import no.nav.k9.prosesstask.impl.BatchTaskScheduler;
import no.nav.k9.prosesstask.impl.TaskManager;

/**
 * Initialiserer bakgrunns tasks.
 */
@WebListener
public class AppServicesStarter implements ServletContextListener {

    @Inject
    private TaskManager taskManager;  // NOSONAR

    @Inject
    private BatchTaskScheduler batchTaskScheduler;

    @Inject
    private SensuKlient sensuKlient;

    public AppServicesStarter() { // NOSONAR
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        sensuKlient.start();
        //taskManager.start();
        batchTaskScheduler.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        batchTaskScheduler.stop();
        taskManager.stop();
        sensuKlient.stop();
    }

}
