package no.nav.ung.sak.metrikker;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task for å kjøre full publisering av kontrollerte inntektperioder metrikker til BigQuery.
 * Denne tasken oppretter prosess tasks for alle behandlinger som har blitt iverksatt og er av type inntektskontroll.
 * Brukes typisk ved initiell opplasting eller re-publisering av historiske data.
 *
 * <p>Tasken kan kjøres manuelt via forvaltning eller startes programmatisk.</p>
 *
 * <p>Eksempel på manuell kjøring:</p>
 * <pre>
 * INSERT INTO prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
 * VALUES (nextval('seq_prosess_task'),
 *         'init.fullPubliseringAvKontrollerteInntektperioder',
 *         nextval('seq_prosess_task_gruppe'),
 *         current_timestamp at time zone 'UTC',
 *         '');
 * </pre>
 *
 * <p>Tasken vil identifisere alle relevante behandlinger basert på følgende kriterier:</p>
 * <ul>
 *   <li>Behandlingsstatus er AVSLUTTET eller IVERKSATT</li>
 *   <li>Fagsak er av type UNGDOMSYTELSE</li>
 *   <li>Behandlingsårsak er RE_KONTROLL_REGISTER_INNTEKT</li>
 *   <li>Vedtak har status IVERKSATT</li>
 * </ul>
 */
@ApplicationScoped
@ProsessTask(FullPubliseringAvKontrollerteInntektperioderMetrikkTask.TASKTYPE)
public class FullPubliseringAvKontrollerteInntektperioderMetrikkTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "init.fullPubliseringAvKontrollerteInntektperioder";
    private static final Logger logger = LoggerFactory.getLogger(FullPubliseringAvKontrollerteInntektperioderMetrikkTask.class);

    private EntityManager entityManager;

    public FullPubliseringAvKontrollerteInntektperioderMetrikkTask() {
        // for CDI proxy
    }

    @Inject
    public FullPubliseringAvKontrollerteInntektperioderMetrikkTask(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        final Query query = entityManager.createNativeQuery(
            "INSERT INTO prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere) " +
            "SELECT nextval('seq_prosess_task'), " +
            "  'bigquery.kontrollertinntektmetrikk', " +
            "  nextval('seq_prosess_task_gruppe'), " +
            "  current_timestamp at time zone 'UTC' + (row_number() OVER (ORDER BY b.opprettet_dato ASC) * interval '2 seconds'), " + // Spre kjøringen for å unngå for mange kall mot bigquery samtidig
            "  'saksnummer=' || f.saksnummer || E'\\n' || " +
            "  'behandlingId=' || b.id " +
            "FROM Behandling b " +
            "INNER JOIN Fagsak f ON (f.id = b.fagsak_id) " +
            "INNER JOIN Behandling_arsak ba ON (ba.behandling_id = b.id) " +
            "INNER JOIN Behandling_vedtak bv ON (bv.behandling_id = b.id) " +
            "WHERE b.behandling_status IN ('AVSLU', 'IVED') " +
            "  AND f.ytelse_type = 'UNG' " +
            "  AND ba.behandling_arsak_type = 'RE_KONTROLL_REGISTER_INNTEKT' " +
            "  AND bv.iverksetting_status = 'IVERKSATT' " +
            "ORDER BY b.opprettet_dato ASC"
        );

        final int antall = query.executeUpdate();
        logger.info("Full publisering av kontrollerte inntektperioder metrikk med antall behandlinger: {}", antall);
    }
}

