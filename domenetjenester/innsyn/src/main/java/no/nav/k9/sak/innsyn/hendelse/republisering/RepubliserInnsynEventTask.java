package no.nav.k9.sak.innsyn.hendelse.republisering;

import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.log.mdc.MdcExtendedLogContext;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.innsyn.hendelse.InnsynEventTjeneste;

/**
 * Republiserer en enkel behandling til innsyn, og lager en ny task av seg selv hvis det finnes flere usendte behandlinger
 * i arbeidstabell.
 *
 */
@ApplicationScoped
@ProsessTask(RepubliserInnsynEventTask.TASKTYPE)
public class RepubliserInnsynEventTask implements ProsessTaskHandler {
    private static final Logger log = LoggerFactory.getLogger(RepubliserInnsynEventTask.class);
    public static final String TASKTYPE = "innsyn.RepubliserInnsynEvent";
    public static final String KJØRING_ID_PROP = "kjoringId";
    public static final String ANTALL_PER_KJØRING_PROP = "antallBehandlingerPerTask";
    private static final MdcExtendedLogContext LOG_CONTEXT = MdcExtendedLogContext.getContext("prosess");

    private ProsessTaskTjeneste prosessTaskTjeneste;
    private InnsynEventTjeneste innsynEventTjeneste;
    private PubliserBehandlingInnsynRepository repository;

    RepubliserInnsynEventTask() {
        // for CDI proxy
    }

    @Inject
    public RepubliserInnsynEventTask(ProsessTaskTjeneste prosessTaskTjeneste,
                                     InnsynEventTjeneste innsynEventTjeneste,
                                     PubliserBehandlingInnsynRepository repository) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.innsynEventTjeneste = innsynEventTjeneste;
        this.repository = repository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var kjøringId = UUID.fromString(prosessTaskData.getPropertyValue(KJØRING_ID_PROP));
        var antallPerKjøring = prosessTaskData.getPropertyValue(ANTALL_PER_KJØRING_PROP);
        Objects.requireNonNull(antallPerKjøring);

        var rader = repository.hentNesteMedLås(kjøringId, Integer.parseInt(antallPerKjøring));

        if (rader.isEmpty()) {
            //Ingen flere ubehandlede elementer, lager ikke ny task.
            log.info("Ferdig med kjøring = {}. Resultat: {}", kjøringId, repository.kjørerapport(kjøringId));
            return;
        }

        for (var rad : rader) {
            try {
                LOG_CONTEXT.add("behandling", rad.getBehandlingId());
                innsynEventTjeneste.publiserBehandling(rad.getBehandlingId());
                rad.fullført();
            } catch (Exception e) {
                log.warn("Publisering til innsyn feilet for id={} behandling={} i kjøring={}", rad.getId(), rad.getBehandlingId(), kjøringId, e);
                rad.feilet(e.getMessage());
            }

        }

        repository.oppdater(rader);
        log.info("Behandlet antall={} i kjøring={} status: {} ", rader.size(), kjøringId, repository.kjørerapport(kjøringId));

        var pd = ProsessTaskData.forProsessTask(RepubliserInnsynEventTask.class);
        pd.setCallIdFraEksisterende();
        pd.setProperty(KJØRING_ID_PROP, kjøringId.toString());
        pd.setProperty(ANTALL_PER_KJØRING_PROP, antallPerKjøring);
        pd.setPrioritet(prosessTaskData.getPriority());
        prosessTaskTjeneste.lagre(pd);

    }


}
