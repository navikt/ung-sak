package no.nav.k9.sak.web.app.tjenester;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import no.nav.vedtak.felles.prosesstask.impl.TaskManager;
import no.nav.vedtak.felles.prosesstask.impl.cron.BatchTaskScheduler;

/**
 * Initialiserer bakgrunns tasks.
 */
@WebListener
public class ProsessTaskStarter implements ServletContextListener {

    @Inject
    private TaskManager taskManager;  // NOSONAR
    @Inject
    private BatchTaskScheduler batchTaskSchedueler;

    public ProsessTaskStarter() { // NOSONAR
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        taskManager.start();
        batchTaskSchedueler.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        taskManager.stop();
        batchTaskSchedueler.stop();
    }

}
