package no.nav.ung.sak.hendelse.stønadstatistikk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(FullPubliseringAvStønadstatistikkTask.TASKTYPE)
public class FullPubliseringAvStønadstatistikkTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "init.fullPubliseringAvStonadstatistikk";
    private static final Logger logger = LoggerFactory.getLogger(FullPubliseringAvStønadstatistikkTask.class);

    private EntityManager entityManager;


    public FullPubliseringAvStønadstatistikkTask() {}

    @Inject
    public FullPubliseringAvStønadstatistikkTask(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    @Override
    public void doTask(ProsessTaskData pd) {
        final Query q = entityManager.createNativeQuery("insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)\n"
                + "select nextval('seq_prosess_task'),\n"
                + "  'iverksetteVedtak.publiserStonadstatistikk',\n"
                + "  nextval('seq_prosess_task_gruppe'),\n"
                + "  current_timestamp at time zone 'UTC' + interval '5 minutes',\n"
                + "  'saksnummer=' || f.saksnummer || '\n"
                + "behandlingId=' || b.id\n"
                + "FROM Behandling b INNER JOIN Fagsak f ON (\n"
                + "  f.id = b.fagsak_id\n"
                + ")\n"
                + "WHERE b.behandling_status IN ('AVSLU', 'IVED')\n"
                + "  AND f.ytelse_type = 'OMP'\n"
                + "ORDER BY b.opprettet_dato ASC");

        final int antall = q.executeUpdate();
        logger.info("Full init stønadstatistikk med antall: " + antall);
    }
}
