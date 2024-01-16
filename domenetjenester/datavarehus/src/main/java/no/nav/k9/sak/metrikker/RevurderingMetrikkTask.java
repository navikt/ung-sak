package no.nav.k9.sak.metrikker;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.sensu.SensuEvent;
import no.nav.k9.felles.integrasjon.sensu.SensuKlient;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = RevurderingMetrikkTask.TASKTYPE, cronExpression = "0 0 8 * * *", maxFailedRuns = 20, firstDelay = 60)
public class RevurderingMetrikkTask implements ProsessTaskHandler {

    private static final int CHUNK_EVENT_SIZE = 1000;

    private static final int LOG_THRESHOLD = 5000;

    static final String TASKTYPE = "revurdering.metrikk.task";

    private static final Logger log = LoggerFactory.getLogger(RevurderingMetrikkTask.class);

    private SensuKlient sensuKlient;

    private RevurderingMetrikkRepository revurderingMetrikkRepository;

    RevurderingMetrikkTask() {
        // for proxyd
    }

    @Inject
    public RevurderingMetrikkTask(SensuKlient sensuKlient, RevurderingMetrikkRepository revurderingMetrikkRepository) {
        this.sensuKlient = sensuKlient;
        this.revurderingMetrikkRepository = revurderingMetrikkRepository;
    }

    @Override
    public void doTask(ProsessTaskData data) {
        var okrMetrikker = revurderingMetrikkRepository.hentAlle();
        logMetrics(okrMetrikker);
        if (okrMetrikker.size() > LOG_THRESHOLD) {
            log.info("Generert {} okr-metrikker til sensu", okrMetrikker.size());
        }


    }

    private void logMetrics(List<SensuEvent> events) {
        var counter = new AtomicInteger();
        var chunkSize = CHUNK_EVENT_SIZE;
        Map<Integer, List<SensuEvent>> chunked = events.stream().collect(Collectors.groupingBy(it -> counter.getAndIncrement() / chunkSize));
        chunked.entrySet().forEach(e -> sensuKlient.logMetrics(e.getValue()));
    }
}
