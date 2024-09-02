package no.nav.k9.sak.behandlingslager.behandling.vilkår.periode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
@ProsessTask(MigrerVilkårPeriodeRegelsporingTask.TASKTYPE)
public class MigrerVilkårPeriodeRegelsporingTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "forvaltning.migrerregelsporing";
    public static final String ANTALL_PERIODER = "antallPerioder";
    public static final String SKAL_KJORE_REKURSIVT = "skalKjoreRekursivt";


    private EntityManager entityManager;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    public MigrerVilkårPeriodeRegelsporingTask(EntityManager entityManager, ProsessTaskTjeneste prosessTaskTjeneste) {
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    public MigrerVilkårPeriodeRegelsporingTask() {
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var maksAntallPerioder = Integer.parseInt(prosessTaskData.getPropertyValue(ANTALL_PERIODER));
        var skalKjoreRekursivt = Boolean.parseBoolean(prosessTaskData.getPropertyValue(SKAL_KJORE_REKURSIVT));


        var query = entityManager.createNativeQuery(
            "UPDATE vr_vilkar_periode SET" +
                "    regel_input_oid = regel_input::oid, " +
                "    regel_evaluering_oid = regel_evaluering::oid " +
                "    WHERE id IN (SELECT id" +
                "                 FROM vr_vilkar_periode" +
                "                 WHERE regel_evaluering is not null and " +
                " regel_evaluering_oid is null " +
                "                 LIMIT :antall FOR UPDATE SKIP LOCKED" +
                "                 )"
        ).setParameter("antall", maksAntallPerioder);


        var antallRaderPåvirket = query.executeUpdate();

        if (skalKjoreRekursivt && maksAntallPerioder == antallRaderPåvirket) {
            var nyTask = ProsessTaskData.forProsessTask(MigrerVilkårPeriodeRegelsporingTask.class);
            nyTask.setProperty(ANTALL_PERIODER, String.valueOf(maksAntallPerioder));
            nyTask.setProperty(SKAL_KJORE_REKURSIVT, "true");
            prosessTaskTjeneste.lagre(nyTask);
        }


    }

}
