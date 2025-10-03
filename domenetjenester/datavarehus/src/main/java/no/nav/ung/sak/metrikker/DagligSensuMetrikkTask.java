package no.nav.ung.sak.metrikker;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.sensu.SensuEvent;
import no.nav.k9.felles.integrasjon.sensu.SensuKlient;
import no.nav.k9.prosesstask.api.BatchProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.impl.cron.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@ApplicationScoped
@ProsessTask(value = DagligSensuMetrikkTask.TASKTYPE, maxFailedRuns = 20, firstDelay = 60)
@Deprecated // Trengs sannsynligvis ikke for ung
public class DagligSensuMetrikkTask implements BatchProsessTaskHandler {

    private static final int CHUNK_EVENT_SIZE = 1000;

    private static final int LOG_THRESHOLD = 5000;

    static final String TASKTYPE = "daglig.sensu.metrikk.task";

    private static final Logger log = LoggerFactory.getLogger(DagligSensuMetrikkTask.class);

    private SensuKlient sensuKlient;

    private StatistikkRepository statistikkRepository;

    DagligSensuMetrikkTask() {
        // for proxy
    }

    @Inject
    public DagligSensuMetrikkTask(SensuKlient sensuKlient, StatistikkRepository statistikkRepository) {
        this.sensuKlient = sensuKlient;
        this.statistikkRepository = statistikkRepository;
    }

    @Override
    public void doTask(ProsessTaskData data) {
        long startTime = System.nanoTime();

        try {
            var metrikker = statistikkRepository.hentDagligRapporterte();

            logMetrics(metrikker);

            if (metrikker.size() > LOG_THRESHOLD) {
                log.info("Generert {} metrikker til sensu", metrikker.size());
            }
        } finally {

            var varighet = Duration.ofNanos(System.nanoTime() - startTime);
            if (Duration.ofSeconds(20).minus(varighet).isNegative()) {
                // bruker for lang tid på logging av metrikker.
                log.warn("Generering av sensu metrikker tok : " + varighet);
            }
        }

    }

    @Override
    public CronExpression getCron() {
        return CronExpression.create("0 1 23 * * *");
    }

    private void logMetrics(List<SensuEvent> events) {
        var counter = new AtomicInteger();
        var chunkSize = CHUNK_EVENT_SIZE;
        Map<Integer, List<SensuEvent>> chunked = events.stream().collect(Collectors.groupingBy(it -> counter.getAndIncrement() / chunkSize));
        chunked.entrySet().forEach(e -> sensuKlient.logMetrics(e.getValue()));
    }
}
