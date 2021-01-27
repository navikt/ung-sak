package no.nav.k9.sak.web.app.tjenester;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import no.nav.vedtak.felles.integrasjon.sensu.SensuKlient;
import no.nav.vedtak.felles.prosesstask.impl.BatchTaskScheduler;
import no.nav.vedtak.felles.prosesstask.impl.TaskManager;

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
        taskManager.start();
        batchTaskScheduler.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        batchTaskScheduler.stop();
        taskManager.stop();
        sensuKlient.stop();
    }

}
