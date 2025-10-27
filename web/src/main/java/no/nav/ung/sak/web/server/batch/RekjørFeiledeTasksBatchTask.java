package no.nav.ung.sak.web.server.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.BatchProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.impl.cron.CronExpression;

@ApplicationScoped
@ProsessTask(value = RekjørFeiledeTasksBatchTask.TASKTYPE)
public class RekjørFeiledeTasksBatchTask implements BatchProsessTaskHandler {

    public static final String TASKTYPE = "batch.retryFeilendeTasks";
    private static final Logger log = LoggerFactory.getLogger(RekjørFeiledeTasksBatchTask.class);
    private BatchProsessTaskRepository taskRepository;

    @Inject
    public RekjørFeiledeTasksBatchTask(BatchProsessTaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public CronExpression getCron() {
        return CronExpression.create("0 30 7,12,17 * * *");
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        int rekjørAlleFeiledeTasks = taskRepository.rekjørAlleFeiledeTasks();
        log.info("Rekjører alle feilende tasks, oppdaterte {} tasks", rekjørAlleFeiledeTasks);
    }
}
