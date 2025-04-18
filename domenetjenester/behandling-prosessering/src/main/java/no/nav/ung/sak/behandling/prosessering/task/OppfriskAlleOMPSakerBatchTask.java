package no.nav.ung.sak.behandling.prosessering.task;

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
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.sak.behandling.prosessering.ProsesseringAsynkTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;

@ApplicationScoped
@ProsessTask(value = OppfriskAlleOMPSakerBatchTask.TASKTYPE, cronExpression = "0 0 20 * * MON-FRI")
public class OppfriskAlleOMPSakerBatchTask implements ProsessTaskHandler {

    private static final Logger log = LoggerFactory.getLogger(OppfriskAlleOMPSakerBatchTask.class);

    public static final String TASKTYPE = "omp.oppfriskalle";

    private EntityManager entityManager;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private ProsesseringAsynkTjeneste prosesseringAsynkTjeneste;

    public OppfriskAlleOMPSakerBatchTask() {}

    @Inject
    public OppfriskAlleOMPSakerBatchTask(EntityManager entityManager,
                                         ProsessTaskTjeneste prosessTaskTjeneste,
                                         ProsesseringAsynkTjeneste prosesseringAsynkTjeneste) {
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.prosesseringAsynkTjeneste = prosesseringAsynkTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        //finn alle åpne behandlinger av OMP som ikke er oppdatert de siste 30 dagene
        final Query q = entityManager.createNativeQuery("select b.* from behandling b " +
            "inner join fagsak f on f.id=b.fagsak_id " +
                "where f.ytelse_type = 'OMP' " +
                "and b.behandling_status = 'UTRED' " +
                "and b.opprettet_dato <= current_date - interval '30 days' " +
                "and (b.endret_tid is null or b.endret_tid <= current_date - interval '30 days')",
            Behandling.class);
        final List<Behandling> behandlinger = q.getResultList();

        //opprett oppfrisk-tasker for alle behandlingene
        if (behandlinger.isEmpty()) {
            log.info("Ingen behandlinger som skal oppfriskes");
            return;
        }
        log.info("Starter asynk oppfrisking av " + behandlinger.size() + " behandlinger");
        opprettTaskerForOppfrisking(behandlinger);
    }

    private void opprettTaskerForOppfrisking(List<Behandling> behandlinger) {
        final ProsessTaskGruppe gruppe = new ProsessTaskGruppe();
        for (Behandling behandling : behandlinger) {
            if (!behandling.isBehandlingPåVent() && !harPågåendeEllerFeiletTask(behandling)) {
                log.info("oppfrisker behandling " + behandling.getId());
                final ProsessTaskData oppfriskTaskData = OppfriskTask.create(behandling, false);
                gruppe.addNesteParallell(oppfriskTaskData);
            } else {
                log.info("oppfrisker ikke behandling " + behandling.getId());
            }
        }
        String gruppeId = prosessTaskTjeneste.lagre(gruppe);
        log.info("Lagret oppfrisk-tasker i taskgruppe [{}]", gruppeId);
    }

    private boolean harPågåendeEllerFeiletTask(Behandling behandling) {
        Map<String, ProsessTaskData> nesteTask = prosesseringAsynkTjeneste.sjekkProsessTaskPågårForBehandling(behandling, null);
        return !nesteTask.isEmpty();
    }
}
