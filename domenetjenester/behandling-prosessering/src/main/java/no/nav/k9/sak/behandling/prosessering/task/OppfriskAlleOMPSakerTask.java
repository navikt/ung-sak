package no.nav.k9.sak.behandling.prosessering.task;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandling.prosessering.ProsesseringAsynkTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;

@ApplicationScoped
@ProsessTask(OppfriskAlleOMPSakerTask.TASKTYPE)
public class OppfriskAlleOMPSakerTask implements ProsessTaskHandler {

    private static final Logger log = LoggerFactory.getLogger(OppfriskAlleOMPSakerTask.class);

    public static final String TASKTYPE = "omp.oppdateralle";

    private EntityManager entityManager;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private ProsesseringAsynkTjeneste prosesseringAsynkTjeneste;

    public OppfriskAlleOMPSakerTask() {}

    @Inject
    public OppfriskAlleOMPSakerTask(EntityManager entityManager,
                                    ProsessTaskTjeneste prosessTaskTjeneste,
                                    ProsesseringAsynkTjeneste prosesseringAsynkTjeneste) {
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.prosesseringAsynkTjeneste = prosesseringAsynkTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {

        //finn id på alle åpne behandlinger av OMP
        final Query q = entityManager.createNativeQuery("select b.* from behandling b " +
            "inner join fagsak f on f.id=b.fagsak_id " +
            "where f.ytelse_type = 'OMP' " +
                "and b.behandling_status in ('OPPRE', 'UTRED')",
            Behandling.class);
        final List<Behandling> behandlinger = q.getResultList();

        //opprett oppfrisk-tasker for alle behandlingene
        log.info("Starter asynk oppfrisking av " + behandlinger.size() + " behandlinger");
        for (Behandling behandling : behandlinger) {
            opprettTaskForOppfrisking(behandling);
        }

        //opprett ny oppfrisk-alle task til neste måned
        final ProsessTaskData nesteKjøringTask = ProsessTaskData.forProsessTask(OppfriskAlleOMPSakerTask.class);
        nesteKjøringTask.setNesteKjøringEtter(LocalDate.now().plusMonths(1).atTime(23, 30));
        prosessTaskTjeneste.lagre(nesteKjøringTask);
    }

    private void opprettTaskForOppfrisking(Behandling behandling) {
        if (!behandling.erStatusFerdigbehandlet() && !behandling.isBehandlingPåVent() && !harPågåendeEllerFeiletTask(behandling)) {
            final ProsessTaskData oppfriskTaskData = OppfriskTask.create(behandling, false);
            prosessTaskTjeneste.lagre(oppfriskTaskData);
        }
    }

    private boolean harPågåendeEllerFeiletTask(Behandling behandling) {
        Map<String, ProsessTaskData> nesteTask = prosesseringAsynkTjeneste.sjekkProsessTaskPågårForBehandling(behandling, null);
        return !nesteTask.isEmpty();
    }
}
