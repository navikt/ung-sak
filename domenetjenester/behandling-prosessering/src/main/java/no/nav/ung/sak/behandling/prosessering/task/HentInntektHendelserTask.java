package no.nav.ung.sak.behandling.prosessering.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
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
import java.util.Comparator;
import java.util.List;

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
    private Duration ventetidFørNesteKjøring;
    private boolean oppfriskVedInkommendeInntektshendelseEnabled;

    public HentInntektHendelserTask() {
        // For CDI
    }

    @Inject
    public HentInntektHendelserTask(InntektAbonnentTjeneste inntektAbonnentTjeneste,
                                    FagsakTjeneste fagsakTjeneste,
                                    BehandlingRepository behandlingRepository,
                                    ProsessTaskTjeneste prosessTaskTjeneste,
                                    @KonfigVerdi(value = "OPPFRISK_VED_INNKOMMENDE_INNTEKTSHENDELSE_ENABLED", required = false, defaultVerdi = "false") boolean oppfriskVedInkommendeInntektshendelseEnabled,
                                    @KonfigVerdi(value = "HENT_INNTEKT_HENDElSER_INTERVALL", required = false, defaultVerdi = "PT1M") String ventetidFørNesteKjøring) {
        this.inntektAbonnentTjeneste = inntektAbonnentTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.oppfriskVedInkommendeInntektshendelseEnabled = oppfriskVedInkommendeInntektshendelseEnabled;
        this.ventetidFørNesteKjøring = Duration.parse(ventetidFørNesteKjøring);
    }


    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var hendelser = hentHendelserOgReschedulerTask(prosessTaskData);
        behandleHendelser(hendelser);
    }

    private List<InntektAbonnentTjeneste.InntektHendelse> hentHendelserOgReschedulerTask(ProsessTaskData prosessTaskData) {
        String sekvensnummerFraTask = prosessTaskData.getPropertyValue(SEKVENSNUMMER_KEY);
        Long sekvensnummer = sekvensnummerFraTask != null ? Long.parseLong(sekvensnummerFraTask) : inntektAbonnentTjeneste.hentFørsteSekvensnummer().orElse(null);
        List<InntektAbonnentTjeneste.InntektHendelse> hendelser = sekvensnummer != null ? inntektAbonnentTjeneste.hentNyeInntektHendelser(sekvensnummer) : List.of();
        Long høyesteSekvensnummerIHendelser = hendelser.stream().map(InntektAbonnentTjeneste.InntektHendelse::sekvensnummer).max(Comparator.naturalOrder()).orElse(null);
        Long nesteSekvensnummer = hendelser.isEmpty() ? sekvensnummer : høyesteSekvensnummerIHendelser + 1;
        opprettNesteTask(nesteSekvensnummer);
        return hendelser;
    }

    private void opprettNesteTask(Long sekvensnummer) {
        var nesteTask = ProsessTaskData.forProsessTask(HentInntektHendelserTask.class);
        nesteTask.setNesteKjøringEtter(LocalDateTime.now().plus(ventetidFørNesteKjøring));
        if (sekvensnummer != null) {
            nesteTask.setProperty(SEKVENSNUMMER_KEY, String.valueOf(sekvensnummer));
            log.info("Opprettet neste task med sekvensnummer={}", sekvensnummer);
        } else {
            log.info("Opprettet neste task uten sekvensnummer");
        }
        prosessTaskTjeneste.lagre(nesteTask);
    }

    private void behandleHendelser(List<InntektAbonnentTjeneste.InntektHendelse> nyeInntektHendelser) {
        log.info("Hentet {} nye inntektshendelser", nyeInntektHendelser.size());

        var relevanteBehandlinger = finnRelevanteBehandlinger(nyeInntektHendelser);

        if (relevanteBehandlinger.isEmpty()) {
            log.info("Ingen hendelser matchet aktive behandlinger");
        }

        log.info("Fant {} relevante behandlinger fra {} hendelser", relevanteBehandlinger.size(), nyeInntektHendelser.size());


        if (!oppfriskVedInkommendeInntektshendelseEnabled) {
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


    private List<ProsessTaskData> opprettOppfriskTaskerForBehandlinger(List<Behandling> behandlinger) {
        return behandlinger.stream()
            .map(behandling -> OppfriskTask.create(behandling, true))
            .toList();
    }

    private boolean venterPåInntektUttalelse(Behandling behandling) {
        return behandling.getAksjonspunkter().stream()
            .anyMatch(ap ->
                ap.getAksjonspunktDefinisjon() ==
                    AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_ETTERLYST_INNTEKTUTTALELSE
                    && ap.getStatus() == AksjonspunktStatus.OPPRETTET
                    && ap.getVenteårsak() == Venteårsak.VENTER_PÅ_ETTERLYST_INNTEKT_UTTALELSE);
    }

    private void opprettOppfriskTaskGruppe(List<ProsessTaskData> oppfriskTasker) {
        var gruppe = new ProsessTaskGruppe();
        gruppe.addNesteParallell(oppfriskTasker);
        String gruppeId = prosessTaskTjeneste.lagre(gruppe);
        log.info("Lagret {} oppfrisk-tasker i taskgruppe [{}]", oppfriskTasker.size(), gruppeId);
    }


}
