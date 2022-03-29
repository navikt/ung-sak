package no.nav.k9.sak.fagsak.prosessering.avsluttning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
@ProsessTask(value = "batch.finnFagsakerForAvsluttning", cronExpression = "0 30 17 * * *", maxFailedRuns = 1)
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

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        log.info("Finner saker som kan avsluttes");

        if (!enableFagsakAvslutting) {
            log.info("Fagsak avslutting er togglet av");
            return;
        }
        var fagsakerSomKanAvsluttes = tjeneste.finnFagsakerSomSkalAvsluttes();

        log.info("Fant {} saker som kan avsluttes", fagsakerSomKanAvsluttes.size());

        fagsakerSomKanAvsluttes.stream()
            .map(AvsluttFagsakTask::opprettTask)
            .forEach(taskdata -> prosessTaskRepository.lagre(taskdata));
    }
}
