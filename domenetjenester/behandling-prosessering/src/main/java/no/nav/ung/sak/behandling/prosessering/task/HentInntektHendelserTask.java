package no.nav.ung.sak.behandling.prosessering.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@ProsessTask(value = HentInntektHendelserTask.TASKTYPE)
public class HentInntektHendelserTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "registerinnhenting.hentInntektHendelser";
    public static final String SEKVENSNUMMER_KEY = "sekvensnummer";

    private static final Logger log = LoggerFactory.getLogger(HentInntektHendelserTask.class);

    private InntektAbonnentTjeneste inntektAbonnentTjeneste;
    private FagsakTjeneste fagsakTjeneste;
    private BehandlingRepository behandlingRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private boolean oppfriskKontrollbehandlingEnabled;
    private Duration ventetidFørNesteKjøring;
    private boolean hentInntektHendelserUtenOppfriskingEnabled;

    public HentInntektHendelserTask() {
        // For CDI
    }

    @Inject
    public HentInntektHendelserTask(InntektAbonnentTjeneste inntektAbonnentTjeneste,
                                    FagsakTjeneste fagsakTjeneste,
                                    BehandlingRepository behandlingRepository,
                                    ProsessTaskTjeneste prosessTaskTjeneste,
                                    @KonfigVerdi(value = "HENT_INNTEKT_HENDELSER_UTEN_OPPFRISKING_ENABLED", required = false, defaultVerdi = "true") boolean hentInntektHendelserUtenOppfriskingEnabled,
                                    @KonfigVerdi(value = "HENT_INNTEKT_HENDElSER_INTERVALL", required = false, defaultVerdi = "PT1M") String ventetidFørNesteKjøring){
        this.inntektAbonnentTjeneste = inntektAbonnentTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.hentInntektHendelserUtenOppfriskingEnabled = hentInntektHendelserUtenOppfriskingEnabled;
        this.ventetidFørNesteKjøring = Duration.parse(ventetidFørNesteKjøring);
    }


    @Override
    public void doTask(ProsessTaskData prosessTaskData) {

        var fraSekvensnummer = hentEllerInitialiserSekvensnummer(prosessTaskData);
        if (fraSekvensnummer.isEmpty()) {
            log.info("Ingen hendelser tilgjengelig fra inntektskomponenten, hopper over henting. Prøver igjen senere.");
            opprettNesteTask(null);
            return;
        }

        log.info("Starter henting av inntektshendelser fra sekvensnummer={}", fraSekvensnummer);

        var nyeInntektHendelser = inntektAbonnentTjeneste.hentNyeInntektHendelser(fraSekvensnummer.get());
        if (nyeInntektHendelser.isEmpty()) {
            log.info("Ingen nye inntektshendelser funnet");
            opprettNesteTask(fraSekvensnummer.get());
            return;
        }

        behandleHendelser(nyeInntektHendelser);

        var sisteSekvensnummer = nyeInntektHendelser.stream()
            .mapToLong(InntektAbonnentTjeneste.InntektHendelse::sekvensnummer)
            .max()
            .orElseThrow();
        opprettNesteTask(sisteSekvensnummer + 1);
    }

    private void behandleHendelser(List<InntektAbonnentTjeneste.InntektHendelse> nyeInntektHendelser) {
        log.info("Hentet {} nye inntektshendelser", nyeInntektHendelser.size());

        var relevanteBehandlinger = finnRelevanteBehandlinger(nyeInntektHendelser);

        if (relevanteBehandlinger.isEmpty()) {
            log.info("Ingen hendelser matchet aktive behandlinger");
        }

        log.info("Fant {} relevante behandlinger fra {} hendelser", relevanteBehandlinger.size(), nyeInntektHendelser.size());


        if (!hentInntektHendelserUtenOppfriskingEnabled) {
            var oppfriskTasker = opprettOppfriskTaskerForBehandlinger(relevanteBehandlinger);
            if (oppfriskTasker.isEmpty()) {
                log.info("Ingen oppfrisk-tasker å opprette etter behandling av inntektshendelser");
            } else {
                opprettOppfriskTaskGruppe(oppfriskTasker);
            }
        } else {
            for (Behandling behandling : relevanteBehandlinger) {
                log.info("Mottatt inntektshendelse for behandling={} saksnummer={} men oppfrisking er deaktivert", behandling.getId(), behandling.getFagsak().getSaksnummer());
            }
        }
    }

    private List<Behandling> finnRelevanteBehandlinger(List<InntektAbonnentTjeneste.InntektHendelse> nyeInntektHendelser) {
        return nyeInntektHendelser.stream()
            .flatMap(hendelse -> fagsakTjeneste.finnFagsakerForAktør(hendelse.aktørId()).stream()
                .filter(Fagsak::erÅpen)
                .filter(fagsak -> fagsak.getPeriode().overlapper(hendelse.periode().getFom(), hendelse.periode().getTom())))
                .flatMap(fagsak -> behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId()).stream())
                .filter(this::venterPåInntektUttalelse)
            .distinct()
            .toList();
    }

    private Optional<Long> hentEllerInitialiserSekvensnummer(ProsessTaskData prosessTaskData) {
        String sekvensnummerVerdi = prosessTaskData.getPropertyValue(SEKVENSNUMMER_KEY);

        if (sekvensnummerVerdi == null) {
            log.info("Første kjøring av task for henting av inntektshendelser. Henter første sekvensnummer fra inntektskomponenten.");
            return inntektAbonnentTjeneste.hentFørsteSekvensnummer();
        }

        return Optional.of(Long.parseLong(sekvensnummerVerdi));
    }

    private List<ProsessTaskData> opprettOppfriskTaskerForBehandlinger(List<Behandling> behandlinger) {
        return behandlinger.stream()
            .map(behandling -> {
                log.info("Oppretter oppfrisk-task for behandling={} saksnummer={}", behandling.getId(), behandling.getFagsak().getSaksnummer());
                return OppfriskTask.create(behandling, true);
            })
            .toList();
    }

    private boolean venterPåInntektUttalelse(Behandling behandling) {
        return


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

    private void opprettNesteTask(Long nesteSekvensnummer) {
        var nesteTask = ProsessTaskData.forProsessTask(HentInntektHendelserTask.class);
        nesteTask.setNesteKjøringEtter(LocalDateTime.now().plus(ventetidFørNesteKjøring));
        if (nesteSekvensnummer != null) {
            nesteTask.setProperty(SEKVENSNUMMER_KEY, String.valueOf(nesteSekvensnummer));
            log.info("Opprettet neste task med sekvensnummer={}", nesteSekvensnummer);
        } else {
            log.info("Opprettet neste task uten sekvensnummer");
        }
        prosessTaskTjeneste.lagre(nesteTask);
    }
}
