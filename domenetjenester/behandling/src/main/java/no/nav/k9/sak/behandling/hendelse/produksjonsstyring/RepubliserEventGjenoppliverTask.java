package no.nav.k9.sak.behandling.hendelse.produksjonsstyring;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;

/**
 * Flytter republiseringtasks tidligere avbrutt og markert med STOPPET_MANUELT tilbake til å kjøres
 */
@ApplicationScoped
@ProsessTask(RepubliserEventGjenoppliverTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class RepubliserEventGjenoppliverTask implements ProsessTaskHandler {
    private static final Logger log = LoggerFactory.getLogger(RepubliserEventGjenoppliverTask.class);
    public static final String TASKTYPE = "oppgavebehandling.RepubliserEventGjenoppliver";

    private EntityManager entityManager;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    RepubliserEventGjenoppliverTask() {
        // for CDI proxy
    }

    @Inject
    public RepubliserEventGjenoppliverTask(EntityManager entityManager,
                                           ProsessTaskTjeneste prosessTaskTjeneste) {
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var antallProperty = prosessTaskData.getPropertyValue("antall");
        if (antallProperty == null) {
            log.warn("Kan ikke starte uten property for antall tasks som skal kjøres");
            return;
        }

        var antall = Integer.parseInt(antallProperty);

        var query = entityManager.createNativeQuery(
        "UPDATE prosess_task SET" +
                "    status = 'KLAR'," +
                "    task_payload = null," +
                "    neste_kjoering_etter = current_timestamp at time zone 'UTC' + floor(random() * 3600) * '1 second'\\:\\:interval" +
                "    WHERE id IN (SELECT id" +
                "                 FROM prosess_task" +
                "                 WHERE status = 'FERDIG'" +
                "                   AND task_type = 'oppgavebehandling.RepubliserEvent'" +
                "                   AND task_payload = 'STOPPET_MANUELT'" +
                "                 LIMIT :antall FOR UPDATE SKIP LOCKED" +
                "                 )"
        );
        query.setParameter("antall", antall);
        var antallRaderPåvirket = query.executeUpdate();

        if (antallRaderPåvirket > 0) {
            log.info("Flyttet "+antallRaderPåvirket+" republiseringstasker tilbake til KLAR");
            final ProsessTaskData nyProsessTask = ProsessTaskData.forProsessTask(RepubliserEventGjenoppliverTask.class);
            nyProsessTask.setCallIdFraEksisterende();
            nyProsessTask.setPrioritet(50);
            nyProsessTask.setProperty("antall", String.valueOf(antall));

            var klokkeslettNesteKjøring = LocalTime.now().plusHours(1);
            if (klokkeslettNesteKjøring.isAfter(LocalTime.of(6, 0, 0)) &
                klokkeslettNesteKjøring.isBefore(LocalTime.of(17, 0, 0))) {
                nyProsessTask.setNesteKjøringEtter(LocalDateTime.of(LocalDate.now(), LocalTime.of(17, 30, 0)));
            } else {
                nyProsessTask.setNesteKjøringEtter(LocalDateTime.now().plusHours(1));
            }

            prosessTaskTjeneste.lagre(nyProsessTask);
        } else {
            log.info("Ingen flere republiseringstasker");
        }
    }
}
