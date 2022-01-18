package no.nav.k9.sak.hendelse.stønadstatistikk;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(FullPubliseringAvStønadstatistikkTask.TASKTYPE)
public class FullPubliseringAvStønadstatistikkTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "init.fullPubliseringAvStonadstatistikk";
    private static final Logger logger = LoggerFactory.getLogger(FullPubliseringAvStønadstatistikkTask.class);

    private EntityManager entityManager;
    private boolean enableStønadstatistikk;

    
    public FullPubliseringAvStønadstatistikkTask() {}

    @Inject
    public FullPubliseringAvStønadstatistikkTask(EntityManager entityManager,
            @KonfigVerdi(value = "ENABLE_STONADSTATISTIKK", defaultVerdi = "false") boolean enableStønadstatistikk) {
        this.entityManager = entityManager;
        this.enableStønadstatistikk = enableStønadstatistikk;
    }

    
    @Override
    public void doTask(ProsessTaskData pd) {
        if (!enableStønadstatistikk) {
            return;
        }
        
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
                + "  AND f.ytelse_type = 'PSB'\n"
                + "ORDER BY b.opprettet_dato ASC");
        
        final int antall = q.executeUpdate();
        logger.info("Full init stønadstatistikk med antall: " + antall);
    }
}
