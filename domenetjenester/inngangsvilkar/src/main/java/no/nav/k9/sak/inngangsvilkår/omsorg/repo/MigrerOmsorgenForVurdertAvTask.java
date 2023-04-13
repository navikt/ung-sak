package no.nav.k9.sak.inngangsvilkår.omsorg.repo;

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
@ProsessTask(MigrerOmsorgenForVurdertAvTask.TASKTYPE)
public class MigrerOmsorgenForVurdertAvTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "migrer.omsorgenfor.vurdertav";
    private static final Logger logger = LoggerFactory.getLogger(MigrerOmsorgenForVurdertAvTask.class);

    private EntityManager entityManager;

    public MigrerOmsorgenForVurdertAvTask() {
    }

    @Inject
    public MigrerOmsorgenForVurdertAvTask(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void doTask(ProsessTaskData pd) {
        final Long behandlingId = Long.parseLong(pd.getBehandlingId());
        final Query q = entityManager.createNativeQuery("with gr as (select * from gr_omsorgen_for where behandling_id = :behandlingId) " +
            "update omsorgen_for_periode it " +
            "set vurdert_av = " +
                "(select ofp.opprettet_av from omsorgen_for_periode ofp " +
                "where ofp.omsorgen_for_id in (select omsorgen_for_id from gr) " +
                "and ofp.fom = it.fom and ofp.tom = it.tom and ofp.begrunnelse is not distinct from it.begrunnelse and ofp.resultat = it.resultat and ofp.relasjon is not distinct from it.relasjon and ofp.relasjonsbeskrivelse is not distinct from it.relasjonsbeskrivelse " +
                "order by opprettet_tid limit 1), " +
            "vurdert_tid = " +
                "(select ofp.opprettet_tid from omsorgen_for_periode ofp " +
                "where ofp.omsorgen_for_id in (select omsorgen_for_id from gr) " +
                "and ofp.fom = it.fom and ofp.tom = it.tom and ofp.begrunnelse is not distinct from it.begrunnelse and ofp.resultat = it.resultat and ofp.relasjon is not distinct from it.relasjon and ofp.relasjonsbeskrivelse is not distinct from it.relasjonsbeskrivelse " +
                "order by opprettet_tid limit 1) " +
            "where omsorgen_for_id in (select omsorgen_for_id from gr) and resultat != 'IKKE_VURDERT' and vurdert_av is null and vurdert_tid is null");
        q.setParameter("behandlingId", behandlingId);
        final int antall = q.executeUpdate();
        logger.info(TASKTYPE + " oppdatert " + antall + " rader");
    }
}
