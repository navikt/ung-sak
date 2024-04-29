package no.nav.k9.sak.innsyn.hendelse.republisering;

import static no.nav.k9.sak.innsyn.hendelse.republisering.RepubliserInnsynEventTask.ANTALL_PER_KJØRING_PROP;
import static no.nav.k9.sak.innsyn.hendelse.republisering.RepubliserInnsynEventTask.KJØRING_ID_PROP;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;

/**
 * Setter i gang publisering av alle behandlinger til innsyn. Kjøres ved behov f.eks. hvis nye felter skal sendes til innsyn.
 * Parameter antall styrer hvor mange som sendes om gangen per prosesstask. Default er 1000
 *
 * Kansellerer eksisterende kjøringer.
 * Bør bare kjøre en kjøring om gangen.
 * Bruk KansellerRepublisering for å stoppe alle kjøringer manuelt.
 * Bruk SlettFerdigeRepubliseringInnsynTask for å slette alle ferdige rader i tabellen. Kan være greit etter mange kjøringer.
 *
 * Bakgrunn for å bruke egen arbeidstabell istedenfor prosesstask er at prosesstask bruker ikke databaseindekser,
 * og hvis det finnes svært mange aktive prosesstasker så streiker prosesstaskmotoren. Med egen tabell så styres antall aktive prosesstask selv,
 * samtidig som man får brukt prosesstask for selve utførelsen og transaksjonshåndtering.
 *
 */
@ApplicationScoped
@ProsessTask(StartRepubliseringInnsynTask.TASKTYPE)
public class StartRepubliseringInnsynTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "innsyn.StartRepubliseringInnsyn";
    private PubliserBehandlingInnsynRepository repository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private static final String KANSELLER_EKSISTERENDE_PROP = "kansellerEksisterende";
    private static final Logger logger = LoggerFactory.getLogger(StartRepubliseringInnsynTask.class);

    public StartRepubliseringInnsynTask() {
    }

    @Inject
    public StartRepubliseringInnsynTask(PubliserBehandlingInnsynRepository repository, ProsessTaskTjeneste prosessTaskTjeneste) {
        this.repository = repository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var kjøringId = UUID.randomUUID();
        logger.info("Initierer republisering med kjøringid={}", kjøringId);

        //Stopper kjørende - bør ikke ha flere aktive i PROD, men enklere å teste med dette
        if (Optional.ofNullable(prosessTaskData.getPropertyValue(KANSELLER_EKSISTERENDE_PROP))
            .map(Boolean::valueOf)
            .orElse(false)) {
            kansellerEksisterende(kjøringId);
        }

        repository.klargjørNyKjøring(kjøringId);
        logger.info("Arbeidstabell populert for kjøringid={}. ", kjøringId);

        var pd = ProsessTaskData.forProsessTask(RepubliserInnsynEventTask.class);
        pd.setCallIdFraEksisterende();
        pd.setProperty(KJØRING_ID_PROP, kjøringId.toString());
        pd.setProperty(ANTALL_PER_KJØRING_PROP,
            Optional.ofNullable(prosessTaskData.getPropertyValue(ANTALL_PER_KJØRING_PROP))
                .orElse("1000"));
        pd.setPrioritet(0);
        prosessTaskTjeneste.lagre(pd);


    }

    private void kansellerEksisterende(UUID kjøringId) {
        int endring = repository.kansellerAlleAktive("kansellert pga ny kjøring=" + kjøringId);
        if (endring > 0) {
            logger.warn("eksisterende kjøring kansellert! Antall rader påvirket = {}", endring);
        }
    }
}
