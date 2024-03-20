package no.nav.k9.sak.innsyn.hendelse.republisering;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;

/**
 * For å manuelt slette ferdige kjøringer for å rydde i databasen
 */
@ApplicationScoped
@ProsessTask(SlettFerdigeRepubliseringInnsynTask.TASKTYPE)
public class SlettFerdigeRepubliseringInnsynTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "innsyn.SlettFerdigeRepubliseringInnsyn";
    private PubliserInnsynRepository repository;
    private static final Logger logger = LoggerFactory.getLogger(SlettFerdigeRepubliseringInnsynTask.class);

    public SlettFerdigeRepubliseringInnsynTask() {
    }

    @Inject
    public SlettFerdigeRepubliseringInnsynTask(PubliserInnsynRepository repository) {
        this.repository = repository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        //Sletter ferdige kjøringer
        repository.slettFerdige();
        logger.info("Slettet alle ferdige kjøringer.");

    }
}
