package no.nav.k9.sak.behandling.hendelse;

import org.hibernate.jpa.SpecHints;
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
@ProsessTask(MigrerUnntakEtablertTilsynVurdertAvTask.TASKTYPE)
public class MigrerUnntakEtablertTilsynVurdertAvTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "migrer.unntaketablerttilsynperiode.vurdertav";
    private static final Logger logger = LoggerFactory.getLogger(MigrerUnntakEtablertTilsynVurdertAvTask.class);

    private EntityManager entityManager;

    public MigrerUnntakEtablertTilsynVurdertAvTask() {
    }

    @Inject
    public MigrerUnntakEtablertTilsynVurdertAvTask(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void doTask(ProsessTaskData pd) {
        final Long behandlingId = Long.parseLong(pd.getBehandlingId());
        final Query q = entityManager.createNativeQuery("update psb_unntak_etablert_tilsyn_periode it " +
            "set vurdert_av = (select opprettet_av from psb_unntak_etablert_tilsyn_periode where " +
                    "fom = it.fom and tom = it.tom and begrunnelse is not distinct from it.begrunnelse and resultat = it.resultat and soeker_aktoer_id = it.soeker_aktoer_id and kilde_behandling_id = it.kilde_behandling_id " +
                    "order by opprettet_tid limit 1), " +
                "vurdert_tid = (select opprettet_tid from psb_unntak_etablert_tilsyn_periode where " +
                    "fom = it.fom and tom = it.tom and begrunnelse is not distinct from it.begrunnelse and resultat = it.resultat and soeker_aktoer_id = it.soeker_aktoer_id and kilde_behandling_id = it.kilde_behandling_id " +
                    "order by opprettet_tid limit 1) " +
            "where kilde_behandling_id = " + behandlingId + " and vurdert_av is null and vurdert_tid is null")
            .setHint(SpecHints.HINT_SPEC_QUERY_TIMEOUT, 30000);
        final int antall = q.executeUpdate();
        logger.info(TASKTYPE + " oppdatert " + antall + " rader");
    }
}
