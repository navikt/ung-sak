package no.nav.ung.sak.web.app.tjenester.behandling;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskStatus;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sikkerhet.context.SubjectHandler;
import no.nav.k9.sikkerhet.oidc.token.internal.JwtUtil;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.ung.sak.behandling.prosessering.ProsesseringAsynkTjeneste;
import no.nav.ung.sak.behandling.prosessering.task.OppfriskTask;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.kontrakt.AsyncPollingStatus;
import no.nav.ung.sak.tilgangskontroll.tilganger.AnsattTilgangerTjeneste;
import no.nav.ung.sak.web.app.tjenester.VurderProsessTaskStatusForPollingApi;
import no.nav.ung.sak.web.app.tjenester.VurderProsessTaskStatusForPollingApi.ProsessTaskFeilmelder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Dependent
public class SjekkProsessering {

    private static final Logger logger = LoggerFactory.getLogger(SjekkProsessering.class);
    private static final ProsesseringFeil FEIL = FeilFactory.create(ProsesseringFeil.class);

    private ProsesseringAsynkTjeneste asynkTjeneste;

    private AnsattTilgangerTjeneste ansattTilgangerTjeneste;
    private BehandlingRepository behandlingRepository;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private ProsessTaskTjeneste prosessTaskRepository;

    SjekkProsessering(ProsesseringAsynkTjeneste asynkTjeneste) {
        this.asynkTjeneste = asynkTjeneste;
    }

    @Inject
    public SjekkProsessering(ProsesseringAsynkTjeneste asynkTjeneste,
                             BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                             AnsattTilgangerTjeneste ansattTilgangerTjeneste,
                             BehandlingRepository behandlingRepository,
                             ProsessTaskTjeneste prosessTaskRepository) {
        this.asynkTjeneste = asynkTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.ansattTilgangerTjeneste = ansattTilgangerTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    public Behandling hentBehandling(UUID behandlingUuid) {
        return behandlingRepository.hentBehandling(behandlingUuid);
    }

    /**
     * Sjekker om det pågår åpne prosess tasks (for angitt gruppe). Returnerer eventuelt task gruppe for eventuell åpen prosess task gruppe.
     */
    public Optional<AsyncPollingStatus> sjekkProsessTaskPågårForBehandling(Behandling behandling, String gruppe) {

        Long behandlingId = behandling.getId();

        Map<String, ProsessTaskData> nesteTask = asynkTjeneste.sjekkProsessTaskPågårForBehandling(behandling, gruppe);
        return new VurderProsessTaskStatusForPollingApi(FEIL, behandlingId).sjekkStatusNesteProsessTask(gruppe, nesteTask);
    }

    /**
     * Hvorvidt betingelser for å hente inn registeropplysninger på nytt er oppfylt.
     */
    private boolean skalInnhenteRegisteropplysningerPåNytt(Behandling behandling) {
        return erGyldigBehandlingStatus(behandling)
            && !behandling.isBehandlingPåVent()
            && behandlingProsesseringTjeneste.skalInnhenteRegisteropplysningerPåNytt(behandling);
    }

    private boolean erGyldigBehandlingStatus(Behandling behandling) {
        return BehandlingStatus.UTREDES.equals(behandling.getStatus());
    }

    private boolean harRolleSaksbehandler() {
        return ansattTilgangerTjeneste.tilgangerForInnloggetBruker().kanSaksbehandle();
    }

    /**
     * Betinget sjekk om innhent registeropplysninger (conditionally) og kjør prosess. Alt gjøres asynkront i form av prosess tasks.
     * Intern sjekk på om hvorvidt registeropplysninger må reinnhentes.
     *
     * @param sjekkSaksbehandler
     * @return optional Prosess Task gruppenavn som kan brukes til å sjekke fremdrift
     */
    public Optional<String> sjekkOgForberedAsynkInnhentingAvRegisteropplysningerOgKjørProsess(Behandling behandling, boolean sjekkSaksbehandler) {
        if (!skalInnhenteRegisteropplysningerPåNytt(behandling) || (sjekkSaksbehandler && !harRolleSaksbehandler())) {
            return Optional.empty();
        }

        if (pågårEllerFeiletTasks(behandling)) {
            return Optional.empty();
        }

        // henter alltid registeropplysninger og kjører alltid prosess
        return Optional.of(asynkInnhentingAvRegisteropplysningerOgKjørProsess(behandling, false));
    }

    public boolean opprettTaskForOppfrisking(Behandling behandling, boolean forceInnhent) {
        if (pågårEllerFeiletTasks(behandling)) {
            return false;
        }

        final ProsessTaskData oppfriskTaskData = OppfriskTask.create(behandling, forceInnhent);
        prosessTaskRepository.lagre(oppfriskTaskData);
        logger.info("Opprettet oppfriskingtask for behandligng {}", behandling.getUuid());
        return true;
    }

    private boolean pågårEllerFeiletTasks(Behandling behandling) {
        var taskStatus = sjekkProsessTaskPågårForBehandling(behandling, null);
        if (taskStatus.isPresent()) {
            var st = taskStatus.get();
            return st.isReadOnly();
        }
        return false;
    }

    /**
     * Innhent registeropplysninger og kjør prosess asynkront.
     *
     * @return Prosess Task gruppenavn som kan brukes til å sjekke fremdrift
     */
    String asynkInnhentingAvRegisteropplysningerOgKjørProsess(Behandling behandling, boolean forceInnhent) {
        ProsessTaskGruppe gruppe = behandlingProsesseringTjeneste.lagOppdaterFortsettTasksForPolling(behandling, forceInnhent);
        String gruppeNavn = asynkTjeneste.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(behandling.getFagsakId(), String.valueOf(behandling.getId()),
            gruppe);
        return gruppeNavn;
    }

    interface ProsesseringFeil extends DeklarerteFeil, ProsessTaskFeilmelder {

        String FORSINKELSE_I_TASK = "FP-193309";
        String FEIL_I_TASK = "FP-193308";
        String FORSINKELSE_VENTER_SVAR = "FP-193310";

        @Override
        @TekniskFeil(feilkode = FEIL_I_TASK, feilmelding = "[%1$s]. Forespørsel på behandling [id=%2$s] som ikke kan fortsette, Problemer med task gruppe [%3$s]. Siste prosesstask[id=%5$s, type=%4$s] status=%6$s, sistKjørt=%7$s", logLevel = LogLevel.INFO)
        Feil feilIProsessTaskGruppe(String callId, Long behandlingId, String gruppe, String taskType, Long taskId, ProsessTaskStatus taskStatus, LocalDateTime sistKjørt);

        @Override
        @TekniskFeil(feilkode = FORSINKELSE_I_TASK, feilmelding = "[%1$s]. Forespørsel på behandling [id=%2$s] som er utsatt i påvente av task [id=%5$s, type=%4$s], Gruppe [%3$s] kjøres ikke før senere. Task status=%6$s, planlagt neste kjøring=%7$s", logLevel = LogLevel.INFO)
        Feil utsattKjøringAvProsessTask(String callId, Long behandlingId, String gruppe, String taskType, Long taskId, ProsessTaskStatus taskStatus, LocalDateTime nesteKjøringEtter);

        @Override
        @TekniskFeil(feilkode = FORSINKELSE_VENTER_SVAR, feilmelding = "[%1$s]. Forespørsel på behandling [id=%2$s] som venter på svar fra annet system (task [id=%5$s, type=%4$s], gruppe [%3$s] kjøres ikke før det er mottatt). Task status=%6$s, sistKjørt=%7$s", logLevel = LogLevel.INFO)
        Feil venterPåSvar(String callId, Long entityId, String gruppe, String taskType, Long id, ProsessTaskStatus status, LocalDateTime sistKjørt);

    }
}
