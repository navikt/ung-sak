package no.nav.k9.sak.behandlingslager.behandling.vilkår.periode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
@ProsessTask(MigrerVedtakVarselFritekstbrevTask.TASKTYPE)
public class MigrerVedtakVarselFritekstbrevTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "forvaltning.migrerfritekstbrev";
    public static final String MAKS_ANTALL = "antall";
    public static final String SKAL_KJORE_REKURSIVT = "skalKjoreRekursivt";

    private static final Logger log = LoggerFactory.getLogger(MigrerVedtakVarselFritekstbrevTask.class);
    private EntityManager entityManager;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    public MigrerVedtakVarselFritekstbrevTask(EntityManager entityManager, ProsessTaskTjeneste prosessTaskTjeneste) {
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    public MigrerVedtakVarselFritekstbrevTask() {
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var maksAntallPerioder = Integer.parseInt(prosessTaskData.getPropertyValue(MAKS_ANTALL));
        var skalKjoreRekursivt = Boolean.parseBoolean(prosessTaskData.getPropertyValue(SKAL_KJORE_REKURSIVT));


        var query = entityManager.createNativeQuery(
            "UPDATE BEHANDLING_VEDTAK_VARSEL SET" +
                "    fritekstbrev_oid = fritekstbrev::oid " +
                "    WHERE id IN (SELECT id" +
                "                 FROM BEHANDLING_VEDTAK_VARSEL" +
                "                 WHERE fritekstbrev is not null AND " +
                " fritekstbrev_oid is null" +
                "                 LIMIT :antall FOR UPDATE SKIP LOCKED" +
                "                 )"
        ).setParameter("antall", maksAntallPerioder);


        var antallRaderPåvirket = query.executeUpdate();

        log.info("Antall rader oppdatert: "  + antallRaderPåvirket);

        if (skalKjoreRekursivt && maksAntallPerioder == antallRaderPåvirket) {
            var nyTask = ProsessTaskData.forProsessTask(MigrerVedtakVarselFritekstbrevTask.class);
            nyTask.setProperty(MAKS_ANTALL, String.valueOf(maksAntallPerioder));
            nyTask.setProperty(SKAL_KJORE_REKURSIVT, "true");
            prosessTaskTjeneste.lagre(nyTask);
        }


    }

}
