package no.nav.ung.sak.fagsak.prosessering.avsluttning;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.BatchProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.prosesstask.impl.cron.CronExpression;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;

@ApplicationScoped
@ProsessTask(value = "batch.finnFagsakerForAvsluttning", maxFailedRuns = 1)
public class FinnFagsakerForAvsluttningBatchTask implements BatchProsessTaskHandler {

    private static final Logger log = LoggerFactory.getLogger(FinnFagsakerForAvsluttningBatchTask.class);
    private FagsakAvsluttningTjeneste tjeneste;
    private ProsessTaskTjeneste prosessTaskRepository;

    FinnFagsakerForAvsluttningBatchTask() {
        // CDI
    }

    @Inject
    public FinnFagsakerForAvsluttningBatchTask(FagsakAvsluttningTjeneste tjeneste, ProsessTaskTjeneste prosessTaskRepository) {
        this.tjeneste = tjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    @Override
    public CronExpression getCron() {
        return CronExpression.create("0 30 21 * * *");
    }

    static <T> Collection<List<T>> partitionBasedOnSize(Collection<T> inputList, int size) {
        final AtomicInteger counter = new AtomicInteger(0);
        return inputList.stream()
            .collect(Collectors.groupingBy(s -> counter.getAndIncrement() / size))
            .values();
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fagsakerSomKanAvsluttes = tjeneste.finnFagsakerSomSkalAvsluttes()
            .stream().map(Fagsak::getSaksnummer).collect(Collectors.toSet());

        log.info("Fant {} saker som kan avsluttes", fagsakerSomKanAvsluttes.size());

        partitionBasedOnSize(fagsakerSomKanAvsluttes, 500).stream()
            .map(OpprettAvsluttFagsakBatchTask::opprettTask)
            .forEach(taskdata -> prosessTaskRepository.lagre(taskdata));
    }
}
