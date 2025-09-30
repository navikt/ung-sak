package no.nav.ung.sak.behandling.prosessering.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.*;
import no.nav.ung.sak.behandling.prosessering.ProsesseringAsynkTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@ApplicationScoped
@ProsessTask(value = OppfriskInntektForBehandlingerPåVentBatchTask.TASKTYPE, cronExpression = "0 * * * *")
public class OppfriskInntektForBehandlingerPåVentBatchTask implements ProsessTaskHandler {

    private static final Logger log = LoggerFactory.getLogger(OppfriskInntektForBehandlingerPåVentBatchTask.class);

    public static final String TASKTYPE = "batch.oppfriskInntektPåVent";
    private boolean oppfriskKontrollbehandlingEnabled;

    private EntityManager entityManager;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private ProsesseringAsynkTjeneste prosesseringAsynkTjeneste;

    public OppfriskInntektForBehandlingerPåVentBatchTask() {
    }

    @Inject
    public OppfriskInntektForBehandlingerPåVentBatchTask(EntityManager entityManager,
                                                         ProsessTaskTjeneste prosessTaskTjeneste,
                                                         ProsesseringAsynkTjeneste prosesseringAsynkTjeneste,
                                                         @KonfigVerdi(value = "OPPFRISK_KONTROLLBEHANDLING_ENABLED", required = false, defaultVerdi = "false") boolean oppfriskKontrollbehandlingEnabled) {
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.prosesseringAsynkTjeneste = prosesseringAsynkTjeneste;
        this.oppfriskKontrollbehandlingEnabled = oppfriskKontrollbehandlingEnabled;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        if (!oppfriskKontrollbehandlingEnabled) {
            log.info("Oppfrisk av kontrollbehandling er ikke aktivert, avslutter task");
            return;
        }

        // finn alle behandlinger som er på vent og ikke har blitt oppfrisket de siste 24 timene
        final Query q = entityManager.createNativeQuery("select b.* " +
                    "from behandling b " +
                    "inner join behandling_arsak ba on ba.behandling_id = b.id " +
                    "inner join aksjonspunkt ap on ap.behandling_id = b.id " +
                    "where behandling_status = 'UTRED' " +
                    "and behandling_arsak_type = 'RE-KONTROLL-REGISTER-INNTEKT' " +
                    "and aksjonspunkt_def = '7040' " +
                    "and aksjonspunkt_status = 'OPPRE' " +
                    "and vent_aarsak = 'VENTER_ETTERLYS_INNTEKT_UTTALELSE'",
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
