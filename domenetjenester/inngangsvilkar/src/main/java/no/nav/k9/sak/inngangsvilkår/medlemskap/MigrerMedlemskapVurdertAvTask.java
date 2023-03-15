package no.nav.k9.sak.inngangsvilkår.medlemskap;

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
@ProsessTask(MigrerMedlemskapVurdertAvTask.TASKTYPE)
public class MigrerMedlemskapVurdertAvTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "migrer.medlemskap.vurdertav";
    private static final Logger logger = LoggerFactory.getLogger(MigrerMedlemskapVurdertAvTask.class);

    private EntityManager entityManager;

    public MigrerMedlemskapVurdertAvTask() {
    }

    @Inject
    public MigrerMedlemskapVurdertAvTask(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void doTask(ProsessTaskData pd) {
        final Long behandlingId = Long.parseLong(pd.getBehandlingId());
        final Query q = entityManager.createNativeQuery("with gr as (select * from gr_medlemskap where behandling_id = :behandlingId) " +
            "update medlemskap_vurdering_lopende it " +
            "set vurdert_av = " +
                "(select mvl.opprettet_av from medlemskap_vurdering_lopende mvl " +
                "where mvl.vurdert_periode_id in (select vurdering_lopende_id from gr) " +
                "and mvl.oppholdsrett_vurdering is not distinct from it.oppholdsrett_vurdering and mvl.lovlig_opphold_vurdering is not distinct from it.lovlig_opphold_vurdering and mvl.bosatt_vurdering is not distinct from it.bosatt_vurdering and mvl.er_eos_borger is not distinct from it.er_eos_borger and mvl.vurderingsdato = it.vurderingsdato and mvl.begrunnelse is not distinct from it.begrunnelse and mvl.manuell_vurd = it.manuell_vurd " +
                "order by opprettet_tid limit 1), " +
            "vurdert_tid = " +
                "(select mvl.opprettet_tid from medlemskap_vurdering_lopende mvl " +
                "where mvl.vurdert_periode_id in (select vurdering_lopende_id from gr) " +
                "and mvl.oppholdsrett_vurdering is not distinct from it.oppholdsrett_vurdering and mvl.lovlig_opphold_vurdering is not distinct from it.lovlig_opphold_vurdering and mvl.bosatt_vurdering is not distinct from it.bosatt_vurdering and mvl.er_eos_borger is not distinct from it.er_eos_borger and mvl.vurderingsdato = it.vurderingsdato and mvl.begrunnelse is not distinct from it.begrunnelse and mvl.manuell_vurd = it.manuell_vurd " +
                "order by opprettet_tid limit 1) " +
            "where vurdert_periode_id in (select vurdering_lopende_id from gr) and vurdert_av is null and vurdert_tid is null");
        q.setParameter("behandlingId", behandlingId);
        final int antall = q.executeUpdate();
        logger.info(TASKTYPE + " oppdatert " + antall + " rader");
    }
}
