package no.nav.ung.sak.domene.behandling.steg.parallellebehandlinger;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.task.FagsakProsessTask;
import no.nav.ung.sak.domene.behandling.steg.iverksettevedtak.HenleggBehandlingTjeneste;
import no.nav.ung.sak.kontroll.RelevanteKontrollperioderUtleder;

/**
 * Henlegger en kontrollbehandling som ikke har måneder å kontrollere etter synkronisering med nytt vedtak, for eksempel
 * når et opphør har fjernet alle månedene.
 * <p>
 * Tasken legges først i taskgruppen fra synkroniseringen, foran gjenoppta, diff og fortsett. Månedene sjekkes på nytt
 * her, fordi en ny måned kan ha kommet til etter synkroniseringen. Henleggelsen sletter resten av gruppen med
 * {@code ryddProsessTasks}. Tasken selv er knyttet til fagsaken uten behandling, så den slettes ikke. Har
 * kontrollbehandlingen måneder, gjør tasken ingenting, og gruppen fortsetter.
 * <p>
 * Henleggelsen skjer ikke i synkroniseringen, fordi avbryting av etterlysninger kaller ung-brukerdialog-api, og kallet
 * rulles ikke tilbake sammen med databasen.
 */
@ApplicationScoped
@ProsessTask(HenleggKontrollbehandlingUtenMånederTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class HenleggKontrollbehandlingUtenMånederTask extends FagsakProsessTask {

    /** Maks 50 tegn ({@code prosess_task.task_type}). */
    public static final String TASKTYPE = "behandlingskontroll.henleggKontrollbehandling";

    static final String KONTROLLBEHANDLING_ID = "kontrollbehandlingId";
    static final String BEGRUNNELSE = "Ingen måneder å kontrollere etter oppdatering med nytt vedtak";

    /** Samme årsaker som {@code RapporteringsfristAutopunktUtleder}: en mottatt rapportering skal også kontrolleres. */
    static final Set<BehandlingÅrsakType> KONTROLLÅRSAKER = Set.of(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT);

    private static final Logger log = LoggerFactory.getLogger(HenleggKontrollbehandlingUtenMånederTask.class);

    private BehandlingRepository behandlingRepository;
    private RelevanteKontrollperioderUtleder relevanteKontrollperioderUtleder;
    private HenleggBehandlingTjeneste henleggBehandlingTjeneste;

    HenleggKontrollbehandlingUtenMånederTask() {
        // for CDI proxy
    }

    @Inject
    public HenleggKontrollbehandlingUtenMånederTask(FagsakLåsRepository fagsakLåsRepository,
                                                    BehandlingLåsRepository behandlingLåsRepository,
                                                    BehandlingRepository behandlingRepository,
                                                    RelevanteKontrollperioderUtleder relevanteKontrollperioderUtleder,
                                                    HenleggBehandlingTjeneste henleggBehandlingTjeneste) {
        super(fagsakLåsRepository, behandlingLåsRepository);
        this.behandlingRepository = behandlingRepository;
        this.relevanteKontrollperioderUtleder = relevanteKontrollperioderUtleder;
        this.henleggBehandlingTjeneste = henleggBehandlingTjeneste;
    }

    /**
     * Tasken knyttes bare til fagsaken. Med behandlingId ville henleggelsen slettet tasken selv.
     */
    static ProsessTaskData lagTask(Long fagsakId, Long kontrollbehandlingId) {
        var task = ProsessTaskData.forProsessTask(HenleggKontrollbehandlingUtenMånederTask.class);
        task.setFagsakId(Objects.requireNonNull(fagsakId, "fagsakId"));
        task.setProperty(KONTROLLBEHANDLING_ID, Objects.requireNonNull(kontrollbehandlingId, "kontrollbehandlingId").toString());
        task.setCallIdFraEksisterende();
        return task;
    }

    static boolean harMånederÅKontrollere(RelevanteKontrollperioderUtleder utleder, Long behandlingId) {
        return !utleder.utledPerioderForKontrollAvInntekt(behandlingId, KONTROLLÅRSAKER).isEmpty();
    }

    @Override
    protected List<String> identifiserBehandling(ProsessTaskData prosessTaskData) {
        return List.of(prosessTaskData.getPropertyValue(KONTROLLBEHANDLING_ID));
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        Long behandlingId = Long.valueOf(prosessTaskData.getPropertyValue(KONTROLLBEHANDLING_ID));
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        logContext(behandling.getFagsak());

        if (!behandling.erKontrollbehandling()) {
            throw new IllegalStateException("Behandling " + behandlingId + " er ikke en kontrollbehandling");
        }
        if (behandling.erSaksbehandlingAvsluttet()) {
            log.info("Kontrollbehandling {} er allerede avsluttet eller henlagt", behandlingId);
            return;
        }
        if (harMånederÅKontrollere(relevanteKontrollperioderUtleder, behandlingId)) {
            log.info("Kontrollbehandling {} har måneder å kontrollere og fortsetter", behandlingId);
            return;
        }
        log.info("Henlegger kontrollbehandling {} som ikke har måneder å kontrollere", behandlingId);
        henleggBehandlingTjeneste.henleggBehandlingOgAksjonspunkter(behandlingId.toString(), BehandlingResultatType.HENLAGT_MASKINELT, BEGRUNNELSE);
    }
}
