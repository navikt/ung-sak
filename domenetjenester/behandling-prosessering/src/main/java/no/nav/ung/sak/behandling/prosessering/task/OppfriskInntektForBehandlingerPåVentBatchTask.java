package no.nav.ung.sak.behandling.prosessering.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import no.nav.k9.prosesstask.api.*;
import no.nav.ung.sak.behandling.prosessering.ProsesseringAsynkTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@ApplicationScoped
@ProsessTask(value = OppfriskInntektForBehandlingerPåVentBatchTask.TASKTYPE, cronExpression = "0 0 20 * * MON-FRI")
public class OppfriskInntektForBehandlingerPåVentBatchTask implements ProsessTaskHandler {

    private static final Logger log = LoggerFactory.getLogger(OppfriskInntektForBehandlingerPåVentBatchTask.class);

    public static final String TASKTYPE = "ung.oppfrisk.inntekt.påvent";

    private EntityManager entityManager;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private ProsesseringAsynkTjeneste prosesseringAsynkTjeneste;

    public OppfriskInntektForBehandlingerPåVentBatchTask() {}

    @Inject
    public OppfriskInntektForBehandlingerPåVentBatchTask(EntityManager entityManager,
                                                         ProsessTaskTjeneste prosessTaskTjeneste,
                                                         ProsesseringAsynkTjeneste prosesseringAsynkTjeneste) {
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.prosesseringAsynkTjeneste = prosesseringAsynkTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        // finn alle behandlinger som er på vent og ikke har blitt oppfrisket de siste 24 timene
        final Query q = entityManager.createNativeQuery("select b.* from behandling b " +
            "inner join fagsak f on f.id = b.fagsak_id " +
            "inner join behandling_arsak ba on ba.behandling_id = b.id " +
            "inner join aksjonspunkt ap on ap.behandling_id = b.id " +
            "where b.behandling_status = 'UTRED' " +
            "and ba.behandling_arsak_type = 'RE_KONTROLL_REGISTER_INNTEKT' " +
            "and ap.aksjonspunkt_status = 'OPPRE' " +
            "and ap.vent_aarsak = 'AUTO_SATT_PÅ_VENT_ETTERLYST_INNTEKTUTTALELSE'",
            Behandling.class);
        final List<Behandling> behandlinger = q.getResultList();

        // opprett oppfrisk-tasker for alle behandlingene funnet
        if (behandlinger.isEmpty()) {
            log.info("Ingen behandlinger på vent som skal oppfriskes");
            return;
        }
        log.info("Starter asynk oppfrisk av inntekt for" + behandlinger.size() + "behandlinger på vent");
        opprettTaskerForOppfrisking(behandlinger);
    }

    private void opprettTaskerForOppfrisking(List<Behandling> behandlinger) {
        final ProsessTaskGruppe gruppe = new ProsessTaskGruppe();
        for (Behandling behandling : behandlinger) {
            if (behandling.isBehandlingPåVent() && !harPågåendeEllerFeiletTask(behandling)) {
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
