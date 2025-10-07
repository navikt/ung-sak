package no.nav.ung.sak.behandling.prosessering.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.*;
import no.nav.k9.prosesstask.impl.cron.CronExpression;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.sak.behandling.prosessering.ProsesseringAsynkTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@ApplicationScoped
@ProsessTask(value = OppfriskInntektForBehandlingerPåVentBatchTask.TASKTYPE)
public class OppfriskInntektForBehandlingerPåVentBatchTask implements BatchProsessTaskHandler {

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

        TypedQuery<Behandling> query = entityManager.createQuery(
            "SELECT DISTINCT b FROM Behandling b " +
            "JOIN b.behandlingÅrsaker ba " +
            "JOIN b.aksjonspunkter ap " +
            "WHERE b.status = :status " +
            "AND ba.behandlingÅrsakType = :behandlingArsakType " +
            "AND ap.aksjonspunktDefinisjon = :aksjonspunktDef " +
            "AND ap.status = :aksjonspunktStatus " +
            "AND ap.venteårsak = :ventearsak",
            Behandling.class);

        query.setParameter("status", BehandlingStatus.UTREDES);
        query.setParameter("behandlingArsakType", BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT);
        query.setParameter("aksjonspunktDef", AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_ETTERLYST_INNTEKTUTTALELSE);
        query.setParameter("aksjonspunktStatus", AksjonspunktStatus.OPPRETTET);
        query.setParameter("ventearsak", Venteårsak.VENTER_PÅ_ETTERLYST_INNTEKT_UTTALELSE);

        final List<Behandling> behandlinger = query.getResultList();

        // opprett oppfrisk-tasker for alle behandlingene funnet
        if (behandlinger.isEmpty()) {
            log.info("Ingen behandlinger på vent som skal oppfriskes");
            return;
        }
        log.info("Starter asynk oppfrisk av inntekt for {} behandlinger på vent", behandlinger.size());
        opprettTaskerForOppfrisking(behandlinger);
    }

    private void opprettTaskerForOppfrisking(List<Behandling> behandlinger) {
        final ProsessTaskGruppe gruppe = new ProsessTaskGruppe();
        for (Behandling behandling : behandlinger) {
            if (behandling.isBehandlingPåVent() && !harPågåendeEllerFeiletTask(behandling)) {
                log.info("oppfrisker behandling={} saksnummer={}", behandling.getId(), behandling.getFagsak().getSaksnummer().getVerdi());
                final ProsessTaskData oppfriskTaskData = OppfriskTask.create(behandling, false);
                gruppe.addNesteParallell(oppfriskTaskData);
            } else {
                log.info("oppfrisker ikke behandling={} saksnummer={}", behandling.getId(), behandling.getFagsak().getSaksnummer().getVerdi());
            }
        }
        String gruppeId = prosessTaskTjeneste.lagre(gruppe);
        log.info("Lagret oppfrisk-tasker i taskgruppe [{}]", gruppeId);
    }

    private boolean harPågåendeEllerFeiletTask(Behandling behandling) {
        Map<String, ProsessTaskData> nesteTask = prosesseringAsynkTjeneste.sjekkProsessTaskPågårForBehandling(behandling, null);
        return !nesteTask.isEmpty();
    }

    @Override
    public CronExpression getCron() {
        return CronExpression.create("0 0 * * * *");
    }
}
