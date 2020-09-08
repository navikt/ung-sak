package no.nav.k9.sak.web.app.tjenester.behandling;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.k9.sak.behandling.prosessering.ProsesseringAsynkTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.AsyncPollingStatus;
import no.nav.k9.sak.web.app.tjenester.VurderProsessTaskStatusForPollingApi;
import no.nav.k9.sak.web.app.tjenester.VurderProsessTaskStatusForPollingApi.ProsessTaskFeilmelder;
import no.nav.k9.sak.web.app.util.LdapUtil;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;
import no.nav.vedtak.felles.integrasjon.ldap.LdapBruker;
import no.nav.vedtak.felles.integrasjon.ldap.LdapBrukeroppslag;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.konfig.KonfigVerdi;
import no.nav.vedtak.sikkerhet.context.SubjectHandler;

@Dependent
public class SjekkProsessering {

    private static final ProsesseringFeil FEIL = FeilFactory.create(ProsesseringFeil.class);

    private ProsesseringAsynkTjeneste asynkTjeneste;

    private BehandlingRepository behandlingRepository;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private ProsessTaskRepository prosessTaskRepository;
    private String gruppenavnSaksbehandler;

    SjekkProsessering(ProsesseringAsynkTjeneste asynkTjeneste) {
        this.asynkTjeneste = asynkTjeneste;
    }

    @Inject
    public SjekkProsessering(ProsesseringAsynkTjeneste asynkTjeneste,
                             BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                             @KonfigVerdi(value = "bruker.gruppenavn.saksbehandler", defaultVerdi = "dummyGruppe") String gruppenavnSaksbehandler,
                             BehandlingRepository behandlingRepository,
                             ProsessTaskRepository prosessTaskRepository) {
        this.asynkTjeneste = asynkTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.gruppenavnSaksbehandler = gruppenavnSaksbehandler;
        this.behandlingRepository = behandlingRepository;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    public Behandling hentBehandling(UUID behandlingUuid) {
        return behandlingRepository.hentBehandling(behandlingUuid);
    }

    /** Sjekker om det pågår åpne prosess tasks (for angitt gruppe). Returnerer eventuelt task gruppe for eventuell åpen prosess task gruppe. */
    public Optional<AsyncPollingStatus> sjekkProsessTaskPågårForBehandling(Behandling behandling, String gruppe) {

        Long behandlingId = behandling.getId();

        Map<String, ProsessTaskData> nesteTask = asynkTjeneste.sjekkProsessTaskPågårForBehandling(behandling, gruppe);
        return new VurderProsessTaskStatusForPollingApi(FEIL, behandlingId).sjekkStatusNesteProsessTask(gruppe, nesteTask);
    }

    /** Hvorvidt betingelser for å hente inn registeropplysninger på nytt er oppfylt. */
    private boolean skalInnhenteRegisteropplysningerPåNytt(Behandling behandling) {
        BehandlingStatus behandlingStatus = behandling.getStatus();
        return BehandlingStatus.UTREDES.equals(behandlingStatus)
            && !behandling.isBehandlingPåVent()
            && behandlingProsesseringTjeneste.skalInnhenteRegisteropplysningerPåNytt(behandling);
    }

    private boolean harRolleSaksbehandler() {
        String ident = SubjectHandler.getSubjectHandler().getUid();
        LdapBruker ldapBruker = new LdapBrukeroppslag().hentBrukerinformasjon(ident);
        Collection<String> grupper = LdapUtil.filtrerGrupper(ldapBruker.getGroups());
        return grupper.contains(gruppenavnSaksbehandler);
    }

    /**
     * Betinget sjekk om innhent registeropplysninger (conditionally) og kjør prosess. Alt gjøres asynkront i form av prosess tasks.
     * Intern sjekk på om hvorvidt registeropplysninger må reinnhentes.
     * @param sjekkSaksbehandler 
     *
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
        return Optional.of(asynkInnhentingAvRegisteropplysningerOgKjørProsess(behandling));
    }
    
    public boolean opprettTaskForOppfrisking(Behandling behandling) {
        if (!skalInnhenteRegisteropplysningerPåNytt(behandling)) {
            return false;
        }

        if (pågårEllerFeiletTasks(behandling)) {
            return false;
        }

        final ProsessTaskData oppfriskTaskData = OppfriskTask.create(behandling);
        prosessTaskRepository.lagre(oppfriskTaskData);
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
    String asynkInnhentingAvRegisteropplysningerOgKjørProsess(Behandling behandling) {
        ProsessTaskGruppe gruppe = behandlingProsesseringTjeneste.lagOppdaterFortsettTasksForPolling(behandling);
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
