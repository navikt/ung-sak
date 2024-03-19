package no.nav.k9.sak.innsyn.hendelse.republisering;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;

/**
 * For å manuelt kansellere alle aktive kjøringer
 */
@ApplicationScoped
@ProsessTask(KansellerRepubliseringInnsynTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class KansellerRepubliseringInnsynTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "innsyn.KansellerRepubliseringInnsyn";
    private PubliserInnsynRepository repository;
    private static final Logger logger = LoggerFactory.getLogger(KansellerRepubliseringInnsynTask.class);

    public KansellerRepubliseringInnsynTask() {
    }

    @Inject
    public KansellerRepubliseringInnsynTask(PubliserInnsynRepository repository) {
        this.repository = repository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        //Stopper kjørende
        int antall = repository.kansellerAlleAktive("kansellert av bruker");
        logger.info("Kansellert alle aktive kjøringer. {} ble kansellert", antall);

    }
}
