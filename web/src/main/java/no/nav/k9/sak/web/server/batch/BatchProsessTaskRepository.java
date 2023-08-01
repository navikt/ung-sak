package no.nav.k9.sak.web.server.batch;

import java.time.LocalDate;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import no.nav.k9.prosesstask.api.ProsessTaskStatus;

/**
 * Implementasjon av repository som er tilgjengelig for å lagre og opprette nye tasks.
 */
@ApplicationScoped
public class BatchProsessTaskRepository {

    private static final Logger logger = LoggerFactory.getLogger(BatchProsessTaskRepository.class);

    private EntityManager entityManager;

    BatchProsessTaskRepository() {
        // for CDI proxying
    }

    @Inject
    public BatchProsessTaskRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    private static String utledPartisjonsNr(LocalDate date) {
        int måned = date.plusMonths(1).getMonth().getValue();
        if (måned < 10) {
            return "0" + måned;
        }
        return "" + måned;
    }

    int rekjørAlleFeiledeTasks() {
        Query query = entityManager.createNativeQuery("UPDATE PROSESS_TASK " +
            "SET status = :status, " +
            "feilede_forsoek = feilede_forsoek-1 " +
            "WHERE STATUS = :feilet");
        query.setParameter("status", ProsessTaskStatus.KLAR.getDbKode())
            .setParameter("feilet", ProsessTaskStatus.FEILET.getDbKode());
        int updatedRows = query.executeUpdate();
        entityManager.flush();

        return updatedRows;
    }

    long tømNestePartisjon() {
        String partisjonsNr = utledPartisjonsNr(LocalDate.now());
        Long antall = (Long) entityManager.createNativeQuery("select count(*) from prosess_task_partition_ferdig_" + partisjonsNr).getSingleResult();
        logger.info("Sletter {} rader fra prosess_task_partition_ferdig_{}", antall, partisjonsNr);


        Query query = entityManager.createNativeQuery("TRUNCATE prosess_task_partition_ferdig_" + partisjonsNr);
        query.executeUpdate(); //TRUNCATE rapporterer 0 rader uansett hvor mange som blir slettet
        entityManager.flush();

        return antall;
    }

}
