package no.nav.k9.sak.fagsak.prosessering.avsluttning;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;

@ApplicationScoped
@ProsessTask(value = "batch.finnFagsakerForAvsluttning", cronExpression = "0 30 21 * * *", maxFailedRuns = 1)
public class FinnFagsakerForAvsluttningBatchTask implements ProsessTaskHandler {

    private static final Logger log = LoggerFactory.getLogger(FinnFagsakerForAvsluttningBatchTask.class);
    private FagsakAvsluttningTjeneste tjeneste;
    private ProsessTaskTjeneste prosessTaskRepository;
    private boolean enableFagsakAvslutting;

    FinnFagsakerForAvsluttningBatchTask() {
        // CDI
    }

    @Inject
    public FinnFagsakerForAvsluttningBatchTask(FagsakAvsluttningTjeneste tjeneste, ProsessTaskTjeneste prosessTaskRepository,
                                               @KonfigVerdi(value = "enable.fagsak.avslutting", defaultVerdi = "true") boolean enableFagsakAvslutting) {
        this.tjeneste = tjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
        this.enableFagsakAvslutting = enableFagsakAvslutting;
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

        if (!enableFagsakAvslutting) {
            log.info("Fagsak avslutting er togglet av");
            return;
        }

        partitionBasedOnSize(fagsakerSomKanAvsluttes, 500).stream()
            .map(OpprettAvsluttFagsakBatchTask::opprettTask)
            .forEach(taskdata -> prosessTaskRepository.lagre(taskdata));
    }
}
