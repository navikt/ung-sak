package no.nav.ung.sak.behandling.prosessering.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.sak.behandling.FagsakTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.domene.registerinnhenting.InntektAbonnentTjeneste;
import no.nav.ung.sak.typer.AktørId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
@ProsessTask(value = HentInntektHendelserTask.TASKTYPE)
public class HentInntektHendelserTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "registerinnhenting.hentInntektHendelser";
    public static final String SEKVENSNUMMER_KEY = "sekvensnummer";


    private static final Logger log = LoggerFactory.getLogger(HentInntektHendelserTask.class);

    private InntektAbonnentTjeneste inntektAbonnentTjeneste;
    private FagsakTjeneste fagsakTjeneste;
    private BehandlingRepository behandlingRepository;
    private EntityManager entityManager;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private boolean hentInntektHendelserEnabled;
    private boolean oppfriskKontrollbehandlingEnabled;
    private Duration ventetidFørNesteKjøring;

    public HentInntektHendelserTask() {
        // For CDI
    }

    @Inject
    public HentInntektHendelserTask(InntektAbonnentTjeneste inntektAbonnentTjeneste,
                                    FagsakTjeneste fagsakTjeneste,
                                    BehandlingRepository behandlingRepository,
                                    EntityManager entityManager,
                                    ProsessTaskTjeneste prosessTaskTjeneste,
                                    @KonfigVerdi(value = "HENT_INNTEKT_HENDELSER_ENABLED", required = false, defaultVerdi = "false") boolean hentInntektHendelserEnabled,
                                    @KonfigVerdi(value = "HENT_INNTEKT_HENDElSER_INTERVALL", required = false, defaultVerdi = "PT1M") String ventetidFørNesteKjøring,
                                    @KonfigVerdi(value = "OPPFRISK_KONTROLLBEHANDLING_ENABLED", required = false, defaultVerdi = "false") boolean oppfriskKontrollbehandlingEnabled){
        this.inntektAbonnentTjeneste = inntektAbonnentTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.hentInntektHendelserEnabled = hentInntektHendelserEnabled;
        this.oppfriskKontrollbehandlingEnabled = oppfriskKontrollbehandlingEnabled;
        this.ventetidFørNesteKjøring = Duration.parse(ventetidFørNesteKjøring);
    }


    @Override
    public void doTask(ProsessTaskData prosessTaskData) {

        if (skalHoppeOverTask()) {
            return;
        }

        long fraSekvensnummer = hentEllerInitialiserSekvensnummer(prosessTaskData);
        log.info("Starter henting av inntektshendelser fra sekvensnummer={}", fraSekvensnummer);

        var nyeInntektHendelser = inntektAbonnentTjeneste.hentNyeInntektHendelser(fraSekvensnummer).toList();
        if (nyeInntektHendelser.isEmpty()) {
            log.info("Ingen nye inntektshendelser funnet");
            opprettNesteTask(fraSekvensnummer);
            return;
        }

        behandleHendelser(nyeInntektHendelser);

        var sisteSekvensnummer = nyeInntektHendelser.get(nyeInntektHendelser.size() - 1).sekvensnummer();
        opprettNesteTask(sisteSekvensnummer + 1);
    }

    private boolean skalHoppeOverTask() {
        if (!hentInntektHendelserEnabled) {
            log.info("Henting av inntektshendelser er deaktivert. Hopper over task.");
            return true;
        }

        if (oppfriskKontrollbehandlingEnabled) {
            log.info("Oppfrisk Task av kontrollbehandlinger er aktivert. Hopper over task for henting av inntektshendelser.");
            return true;
        }

        return false;
    }

    private void behandleHendelser(List<InntektAbonnentTjeneste.InntektHendelse> nyeInntektHendelser) {
        log.info("Hentet {} nye inntektshendelser", nyeInntektHendelser.size());

        var relevanteHendelser = nyeInntektHendelser.stream()
            .filter(this::harRelevantBehandling)
            .toList();

        if (relevanteHendelser.isEmpty()) {
            log.info("Ingen hendelser matchet aktive behandlinger");
            return;
        }
        log.info("Fant {} relevante hendelser av {} totalt", relevanteHendelser.size(), nyeInntektHendelser.size());

        var unikeAktørIder = unikeAktørIdFraHendelser(relevanteHendelser);
        var oppfriskTasker = opprettOppfriskTaskerForAktører(unikeAktørIder);
        if (oppfriskTasker.isEmpty()) {
            log.info("Ingen oppfrisk-tasker å opprette etter behandling av inntektshendelser");
            return;
        }
        opprettOppfriskTaskGruppe(oppfriskTasker);
    }

    private boolean harRelevantBehandling(InntektAbonnentTjeneste.InntektHendelse hendelse) {
        return fagsakTjeneste.finnFagsakerForAktør(hendelse.aktørId()).stream()
            .filter(Fagsak::erÅpen)
            .flatMap(fagsak -> behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId()).stream())
            .filter(this::venterPåInntektUttalelse)
            .anyMatch(behandling -> behandling.getFagsak().getPeriode().overlapper(hendelse.periode().getFom(), hendelse.periode().getTom()));
    }

    private static Set<AktørId> unikeAktørIdFraHendelser(List<InntektAbonnentTjeneste.InntektHendelse> relevanteHendelser) {
        return relevanteHendelser.stream()
            .map(InntektAbonnentTjeneste.InntektHendelse::aktørId)
            .collect(Collectors.toSet());
    }

    private long hentEllerInitialiserSekvensnummer(ProsessTaskData prosessTaskData) {
        String sekvensnummerVerdi = prosessTaskData.getPropertyValue(SEKVENSNUMMER_KEY);

        if (sekvensnummerVerdi == null) {
            log.info("Første kjøring av task for henting av inntektshendelser. Henter første sekvensnummer fra inntektskomponenten.");
            return inntektAbonnentTjeneste.hentFørsteSekvensnummer();
        }

        return Long.parseLong(sekvensnummerVerdi);
    }

    private List<ProsessTaskData> opprettOppfriskTaskerForAktører(Set<AktørId> aktørIder) {
        return aktørIder.stream()
            .flatMap(aktørId -> fagsakTjeneste.finnFagsakerForAktør(aktørId).stream())
            .filter(Fagsak::erÅpen)
            .flatMap(fagsak -> behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId()).stream())
            .filter(this::venterPåInntektUttalelse)
            .map(behandling -> {
                log.info("Oppretter oppfrisk-task for behandling={} saksnummer={}", behandling.getId(), behandling.getFagsak().getSaksnummer());
                return OppfriskTask.create(behandling, true);
            })
            .toList();
    }

    private boolean venterPåInntektUttalelse(Behandling behandling) {
        return behandling.getStatus() == BehandlingStatus.UTREDES
            && behandling.getBehandlingÅrsaker().stream()
            .anyMatch(ba -> ba.getBehandlingÅrsakType() == BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT)
            && behandling.getAksjonspunkter().stream()
            .anyMatch(ap ->
                ap.getAksjonspunktDefinisjon() == AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_ETTERLYST_INNTEKTUTTALELSE
                    && ap.getStatus() == AksjonspunktStatus.OPPRETTET
                    && ap.getVenteårsak() == Venteårsak.VENTER_PÅ_ETTERLYST_INNTEKT_UTTALELSE);
    }

    private void opprettOppfriskTaskGruppe(List<ProsessTaskData> oppfriskTasker) {
        var gruppe = new ProsessTaskGruppe();
        gruppe.addNesteParallell(oppfriskTasker);
        String gruppeId = prosessTaskTjeneste.lagre(gruppe);
        log.info("Lagret {} oppfrisk-tasker i taskgruppe [{}]", oppfriskTasker.size(), gruppeId);
    }

    private void opprettNesteTask(long nesteSekvensnummer) {
        var nesteTask = ProsessTaskData.forProsessTask(HentInntektHendelserTask.class);
        nesteTask.setNesteKjøringEtter(LocalDateTime.now().plus(ventetidFørNesteKjøring));
        nesteTask.setProperty(SEKVENSNUMMER_KEY, String.valueOf(nesteSekvensnummer));
        prosessTaskTjeneste.lagre(nesteTask);
        log.info("Opprettet neste task med sekvensnummer={}", nesteSekvensnummer);
    }
}
