package no.nav.k9.sak.hendelse.brukerdialoginnsyn;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(FullPubliseringAvBrukerdialoginnsynTask.TASKTYPE)
public class FullPubliseringAvBrukerdialoginnsynTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "init.fullPubliseringAvBrukerdialoginnsyn";
    private static final Logger logger = LoggerFactory.getLogger(FullPubliseringAvBrukerdialoginnsynTask.class);

    private EntityManager entityManager;
    private boolean enableBrukerdialoginnsyn;

    
    public FullPubliseringAvBrukerdialoginnsynTask() {}

    @Inject
    public FullPubliseringAvBrukerdialoginnsynTask(EntityManager entityManager,
            @KonfigVerdi(value = "ENABLE_BRUKERDIALOGINNSYN", defaultVerdi = "false") boolean enableBrukerdialoginnsyn) {
        this.entityManager = entityManager;
        this.enableBrukerdialoginnsyn = enableBrukerdialoginnsyn;
    }

    
    @Override
    public void doTask(ProsessTaskData pd) {
        if (!enableBrukerdialoginnsyn) {
            return;
        }

        final Query q = entityManager.createNativeQuery("insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)\n"
                + "select nextval('seq_prosess_task'),\n"
                + "  'brukerdialoginnsyn.publiserSoknad',\n"
                + "  nextval('seq_prosess_task_gruppe'),\n"
                + "  current_timestamp at time zone 'UTC' + interval '5 minutes',\n"
                + "  'saksnummer=' || f.saksnummer || '\n"
                + "aktoerId=' || f.bruker_aktoer_id || '\n"
                + "pleietrengendeAktoerId=' || f.pleietrengende_aktoer_id || '\n"
                + "mottattDokumentId=' || m.id\n"
                + "from mottatt_dokument m inner join behandling b ON (\n"
                + "    b.id = m.behandling_id\n"
                + "    AND b.fagsak_id = m.fagsak_id\n"
                + "  ) inner join fagsak f ON (\n"
                + "    f.id = m.fagsak_id\n"
                + "  )\n"
                + "where m.status = 'GYLDIG'\n"
                + "  AND m.type = 'PLEIEPENGER_SOKNAD'\n"
                + "  AND f.ytelse_type = 'PSB'\n"
                + "ORDER BY m.mottatt_tidspunkt ASC");
        
        final int antall = q.executeUpdate();
        
        logger.info("Full init søknadbrukerdialoginnsyn med antall: " + antall);
        
        final Query omsorgenForQuery = entityManager.createNativeQuery("insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)\n"
                + "select nextval('seq_prosess_task'),\n"
                + "  'brukerdialoginnsyn.publiserOmsorg',\n"
                + "  nextval('seq_prosess_task_gruppe'),\n"
                + "  current_timestamp at time zone 'UTC' + interval '5 minutes',\n"
                + "  'fagsakId=' || f.id\n"
                + "from fagsak f\n"
                + "where f.ytelse_type = 'PSB'");
        
        final int antallOmsorgenFor = omsorgenForQuery.executeUpdate();
        
        logger.info("Full init omsorgbrukerdialoginnsyn med antall: " + antallOmsorgenFor);
    }
}
