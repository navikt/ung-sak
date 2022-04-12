package no.nav.k9.sak.web.server.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = CleanNextBucketBatchTask.TASKTYPE, cronExpression = "0 0 7 1 * *")
public class CleanNextBucketBatchTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "batch.partitionCleanBucket";
    private static final Logger log = LoggerFactory.getLogger(CleanNextBucketBatchTask.class);
    private BatchProsessTaskRepository taskRepository;

    @Inject
    public CleanNextBucketBatchTask(BatchProsessTaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        int antallSlettet = taskRepository.tømNestePartisjon();
        log.info("Tømmer neste partisjon med ferdige tasks, slettet {}", antallSlettet);
    }
}
