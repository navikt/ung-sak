package no.nav.k9.sak.behandlingslager.behandling.vilkår.periode;

import java.util.ArrayList;
import java.util.List;

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
@ProsessTask(FjernDuplikatRegelsporingTask.TASKTYPE)
public class FjernDuplikatRegelsporingTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "forvaltning.fjernduplikatregelsporing";
    public static final String ANTALL_PERIODER = "antallPerioder";
    public static final String SKAL_KJORE_REKURSIVT = "skalKjoreRekursivt";
    public static final String OPPDATER_KUN_INPUT = "oppdaterKunInput";


    private static final Logger log = LoggerFactory.getLogger(FjernDuplikatRegelsporingTask.class);


    private EntityManager entityManager;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    public FjernDuplikatRegelsporingTask(EntityManager entityManager, ProsessTaskTjeneste prosessTaskTjeneste) {
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    public FjernDuplikatRegelsporingTask() {
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var maksAntallPerioder = Integer.parseInt(prosessTaskData.getPropertyValue(ANTALL_PERIODER));
        var skalKjoreRekursivt = Boolean.parseBoolean(prosessTaskData.getPropertyValue(SKAL_KJORE_REKURSIVT));
        var oppdaterKunInput = prosessTaskData.getPropertyValue(OPPDATER_KUN_INPUT) != null && Boolean.parseBoolean(prosessTaskData.getPropertyValue(OPPDATER_KUN_INPUT));

        List<Long> ider = new ArrayList<>();
        if (!oppdaterKunInput) {
            ider = forsøkOppdaterEvalueringOgInput(maksAntallPerioder);

        }

        if (ider.isEmpty()) {
            ider = forsøkOppdaterKunInput(maksAntallPerioder);
            oppdaterKunInput = true;
        }

        if (skalKjoreRekursivt && (maksAntallPerioder == ider.size())) {
            var nyTask = ProsessTaskData.forProsessTask(FjernDuplikatRegelsporingTask.class);
            nyTask.setProperty(ANTALL_PERIODER, String.valueOf(maksAntallPerioder));
            nyTask.setProperty(SKAL_KJORE_REKURSIVT, "true");
            nyTask.setProperty(OPPDATER_KUN_INPUT, String.valueOf(oppdaterKunInput));

            prosessTaskTjeneste.lagre(nyTask);
        }


    }

    private List<Long> forsøkOppdaterKunInput(int maksAntallPerioder) {
        var regelInputQuery = entityManager.createNativeQuery(
            "select id from vr_vilkar_periode where regel_input is not null and regel_input::oid != regel_input_oid limit :antall FOR UPDATE SKIP LOCKED"
        ).setParameter("antall", maksAntallPerioder);
        var regelInputider = regelInputQuery.getResultList();

        var unlinkInputQuery = entityManager.createNativeQuery(
            "select lo_unlink(regel_input::oid) " +
                "from vr_vilkar_periode where id in (:ider)"
        ).setParameter("ider", regelInputider);
        unlinkInputQuery.getResultList();

        var updateInputQuery = entityManager.createNativeQuery(
            "update vr_vilkar_periode " +
                " set regel_input=null " +
                " where id in (:ider)"
        ).setParameter("ider", regelInputider);
        updateInputQuery.executeUpdate();
        log.info("Antall rader oppdatert for input: " + regelInputider.size());
        return regelInputider;
    }

    private List<Long> forsøkOppdaterEvalueringOgInput(int maksAntallPerioder) {
        var regelInputQuery = entityManager.createNativeQuery(
            "select id from vr_vilkar_periode where regel_input is not null and regel_input::oid != regel_input_oid " +
                "and regel_evaluering is not null and regel_evaluering::oid != regel_evaluering_oid limit :antall FOR UPDATE SKIP LOCKED"
        ).setParameter("antall", maksAntallPerioder);
        var ider = regelInputQuery.getResultList();

        var unlinkQuery = entityManager.createNativeQuery(
            "select lo_unlink(regel_input::oid), " +
                " lo_unlink(regel_evaluering::oid) " +
                "from vr_vilkar_periode where id in (:ider)"
        ).setParameter("ider", ider);
        unlinkQuery.getResultList();

        var updateInputQuery = entityManager.createNativeQuery(
            "update vr_vilkar_periode " +
                " set regel_input=null, " +
                " regeL_evaluering=null " +
                " where id in (:ider)"
        ).setParameter("ider", ider);
        updateInputQuery.executeUpdate();

        log.info("Antall rader oppdatert for input og evaluering: " + ider.size());
        return ider;
    }

}
