package no.nav.ung.sak.web.app.tjenester;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.felles.apptjeneste.AppServiceHandler;
import no.nav.k9.felles.integrasjon.sensu.SensuKlient;
import no.nav.k9.prosesstask.impl.BatchTaskScheduler;
import no.nav.k9.prosesstask.impl.TaskManager;
import no.nav.ung.fordel.kafka.KafkaIntegration;

@ApplicationScoped
public class ApplicationServiceStarterImpl implements ApplicationServiceStarter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationServiceStarterImpl.class);
    private Map<AppServiceHandler, AtomicBoolean> serviceMap = new HashMap<>();
    private SensuKlient sensuKlient;
    private TaskManager taskManager;
    private BatchTaskScheduler batchTaskScheduler;

    ApplicationServiceStarterImpl() {
        // CDI
    }

    @Inject
    public ApplicationServiceStarterImpl(@Any Instance<KafkaIntegration> serviceHandlers,
                                         SensuKlient sensuKlient,
                                         TaskManager taskManager,
                                         BatchTaskScheduler batchTaskScheduler) {
        this.sensuKlient = sensuKlient;
        this.taskManager = taskManager;
        this.batchTaskScheduler = batchTaskScheduler;
        serviceHandlers.forEach(handler -> serviceMap.put(handler, new AtomicBoolean()));
    }

    @Override
    public void startServices() {
        sensuKlient.start();
        taskManager.start();
        batchTaskScheduler.start();
        serviceMap.forEach((key, value) -> {
            if (value.compareAndSet(false, true)) {
                LOGGER.info("starter service: {}", key.getClass().getSimpleName());
                key.start();
            }
        });
    }

    @Override
    public void stopServices() {
        List<Thread> threadList = new ArrayList<>();
        serviceMap.forEach((key, value) -> {
            if (value.compareAndSet(true, false)) {
                LOGGER.info("stopper service: {}", key.getClass().getSimpleName());
                Thread t = new Thread(key::stop);
                t.start();
                threadList.add(t);
            }
        });
        while (!threadList.isEmpty()) {
            Thread t = threadList.get(0);
            try {
                t.join(31000);
                threadList.remove(t);
            } catch (InterruptedException e) {
                LOGGER.warn(e.getMessage());
                t.interrupt();
            }
        }

        batchTaskScheduler.stop();
        taskManager.stop();
        sensuKlient.stop();
    }

}
