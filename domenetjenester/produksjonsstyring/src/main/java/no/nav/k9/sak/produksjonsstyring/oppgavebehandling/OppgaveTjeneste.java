package no.nav.k9.sak.produksjonsstyring.oppgavebehandling;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.BehandlingTema;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.events.BehandlingStatusEvent.BehandlingAvsluttetEvent;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.task.AvsluttOppgaveTaskProperties;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.OppgaveRestKlient;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.OpprettOppgave;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Prioritet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.sikkerhet.context.SubjectHandler;

@ApplicationScoped
public class OppgaveTjeneste {
    public static final Set<FagsakYtelseType> OMS_YTELSER = Set.of(FagsakYtelseType.OMP, FagsakYtelseType.PSB, FagsakYtelseType.OPPLÆRINGSPENGER, FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE);
    private static final Logger logger = LoggerFactory.getLogger(OppgaveTjeneste.class);
    private static final int DEFAULT_OPPGAVEFRIST_DAGER = 1;

    private static final String SAK_MÅ_FLYTTES_TIL_INFOTRYGD = "Sak må flyttes til Infotrygd";

    private static final String NØS_ANSVARLIG_ENHETID = "4151";
    private static final String NØS_BEH_TEMA = "ab0273";
    private static final String NØS_TEMA = "STO";

    private FagsakRepository fagsakRepository;

    private BehandlingRepository behandlingRepository;
    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;
    private ProsessTaskRepository prosessTaskRepository;
    private OppgaveRestKlient restKlient;

    OppgaveTjeneste() {
        // for CDI proxy
    }

    @Inject
    public OppgaveTjeneste(BehandlingRepositoryProvider repositoryProvider,
                           OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository,
                           OppgaveRestKlient restKlient,
                           ProsessTaskRepository prosessTaskRepository) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.oppgaveBehandlingKoblingRepository = oppgaveBehandlingKoblingRepository;
        this.restKlient = restKlient;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    public void avslutt(Long behandlingId, OppgaveÅrsak oppgaveÅrsak) {
        List<OppgaveBehandlingKobling> oppgaveBehandlingKoblinger = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandlingId);
        Optional<OppgaveBehandlingKobling> oppgave = OppgaveBehandlingKobling.getAktivOppgaveMedÅrsak(oppgaveÅrsak, oppgaveBehandlingKoblinger);
        if (oppgave.isPresent()) {
            avsluttOppgave(oppgave.get());
        } else {
            OppgaveFeilmeldinger.FACTORY.oppgaveMedÅrsakIkkeFunnet(oppgaveÅrsak.getKode(), behandlingId).log(logger);
        }
    }

    public String opprettOppgaveFeilutbetaling(BehandlingReferanse ref, String beskrivelse) {
        Behandling behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        Fagsak fagsak = behandling.getFagsak();

        var request = createRestRequestBuilder(fagsak.getSaksnummer(), fagsak.getAktørId(), behandling.getBehandlendeEnhet(), beskrivelse, Prioritet.NORM, DEFAULT_OPPGAVEFRIST_DAGER, mapYtelseTypeTilTema(fagsak.getYtelseType()))
            .medOppgavetype(OppgaveÅrsak.VURDER_KONSEKVENS_YTELSE.getKode());
        var response = restKlient.opprettetOppgave(request);
        return response.getId().toString();
    }

    public void avslutt(Long behandlingId, String oppgaveId) {
        Optional<OppgaveBehandlingKobling> oppgave = oppgaveBehandlingKoblingRepository.hentOppgaveBehandlingKobling(oppgaveId);
        if (oppgave.isPresent()) {
            avsluttOppgave(oppgave.get());
        } else {
            OppgaveFeilmeldinger.FACTORY.oppgaveMedIdIkkeFunnet(oppgaveId, behandlingId).log(logger);
        }
    }

    private void avsluttOppgave(OppgaveBehandlingKobling aktivOppgave) {
        if (!aktivOppgave.isFerdigstilt()) {
            ferdigstillOppgaveBehandlingKobling(aktivOppgave);
        }
        restKlient.ferdigstillOppgave(aktivOppgave.getOppgaveId());
        var oppgv = restKlient.hentOppgave(aktivOppgave.getOppgaveId());
        logger.info("GOSYS ferdigstilte oppgave {} svar {}", aktivOppgave.getOppgaveId(), oppgv);
    }

    private void ferdigstillOppgaveBehandlingKobling(OppgaveBehandlingKobling aktivOppgave) {
        aktivOppgave.ferdigstillOppgave(SubjectHandler.getSubjectHandler().getUid());
        oppgaveBehandlingKoblingRepository.lagre(aktivOppgave);
    }

    private String mapYtelseTypeTilTema(FagsakYtelseType ytelseType) {
        if (OMS_YTELSER.contains(ytelseType)) {
            return "OMS";
        } else if (FagsakYtelseType.FRISINN.equals(ytelseType)) {
            return "FRI";
        }
        throw new UnsupportedOperationException("Støtter ikke ytelsestype " + ytelseType);
    }

    public Optional<ProsessTaskData> opprettTaskAvsluttOppgave(Behandling behandling) {
        List<OppgaveBehandlingKobling> oppgaver = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandling.getId());
        Optional<OppgaveBehandlingKobling> oppgave = oppgaver.stream().filter(kobling -> !kobling.isFerdigstilt()).findFirst();
        if (oppgave.isPresent()) {
            return opprettTaskAvsluttOppgave(behandling, oppgave.get().getOppgaveÅrsak());
        } else {
            return Optional.empty();
        }
    }

    public Optional<ProsessTaskData> opprettTaskAvsluttOppgave(Behandling behandling, OppgaveÅrsak oppgaveÅrsak) {
        return opprettTaskAvsluttOppgave(behandling, oppgaveÅrsak, true);
    }

    public Optional<ProsessTaskData> opprettTaskAvsluttOppgave(Behandling behandling, OppgaveÅrsak oppgaveÅrsak, boolean skalLagres) {
        List<OppgaveBehandlingKobling> oppgaveBehandlingKoblinger = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandling.getId());
        Optional<OppgaveBehandlingKobling> oppgave = OppgaveBehandlingKobling.getAktivOppgaveMedÅrsak(oppgaveÅrsak, oppgaveBehandlingKoblinger);
        if (oppgave.isPresent()) {
            OppgaveBehandlingKobling aktivOppgave = oppgave.get();
            // skal ikke avslutte oppgave av denne typen
            if (OppgaveÅrsak.BEHANDLE_SAK_IT.equals(aktivOppgave.getOppgaveÅrsak())) {
                return Optional.empty();
            }
            ferdigstillOppgaveBehandlingKobling(aktivOppgave);
            ProsessTaskData avsluttOppgaveTask = opprettProsessTask(behandling, AvsluttOppgaveTaskProperties.TASKTYPE);
            avsluttOppgaveTask.setOppgaveId(aktivOppgave.getOppgaveId());
            if (skalLagres) {
                avsluttOppgaveTask.setCallIdFraEksisterende();
                prosessTaskRepository.lagre(avsluttOppgaveTask);
            }
            return Optional.of(avsluttOppgaveTask);
        } else {
            return Optional.empty();
        }
    }

    private ProsessTaskData opprettProsessTask(Behandling behandling, String taskType) {
        ProsessTaskData prosessTask = new ProsessTaskData(taskType);
        prosessTask.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTask.setPrioritet(50);
        return prosessTask;
    }

    private OpprettOppgave.Builder createRestRequestBuilder(Saksnummer saksnummer, AktørId aktørId, String enhet, String beskrivelse, Prioritet prioritet, int fristDager, String tema) {
        return OpprettOppgave.getBuilder()
            .medAktoerId(aktørId.getId())
            .medSaksreferanse(saksnummer != null ? saksnummer.getVerdi() : null)
            .medTildeltEnhetsnr(enhet)
            .medOpprettetAvEnhetsnr(enhet)
            .medAktivDato(LocalDate.now())
            .medFristFerdigstillelse(VirkedagUtil.fomVirkedag(LocalDate.now().plusDays(fristDager)))
            .medBeskrivelse(beskrivelse)
            .medTema(tema)
            .medPrioritet(prioritet);
    }

    /**
     * Supplerende oppgaver: Vurder Dokument og Konsekvens for Ytelse
     */
    public boolean harÅpneOppgaverAvType(AktørId aktørId, OppgaveÅrsak oppgavetype, FagsakYtelseType ytelseType) {
        try {
            var oppgaver = restKlient.finnÅpneOppgaver(aktørId.getId(), mapYtelseTypeTilTema(ytelseType), List.of(oppgavetype.getKode()));
            logger.info("GOSYS fant {} oppgaver av type {}, for ytelse {}", oppgaver.size(), oppgavetype, ytelseType);
            return !oppgaver.isEmpty();
        } catch (Exception e) {
            throw OppgaveFeilmeldinger.FACTORY.feilVedHentingAvOppgaver(ytelseType, oppgavetype, aktørId, e).toException();
        }
    }

    public String opprettMedPrioritetOgBeskrivelseBasertPåFagsakId(Long fagsakId, OppgaveÅrsak oppgaveÅrsak, String enhetsId, String beskrivelse, boolean høyPrioritet) {
        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);

        var orequest = createRestRequestBuilder(fagsak.getSaksnummer(), fagsak.getAktørId(), enhetsId, beskrivelse, høyPrioritet ? Prioritet.HOY : Prioritet.NORM, DEFAULT_OPPGAVEFRIST_DAGER, mapYtelseTypeTilTema(fagsak.getYtelseType()))
            .medBehandlingstema(BehandlingTema.finnForFagsakYtelseType(fagsak.getYtelseType()).getOffisiellKode())
            .medOppgavetype(oppgaveÅrsak.getKode());
        var oppgave = restKlient.opprettetOppgave(orequest);
        logger.info("GOSYS opprettet VURDER VL oppgave {}", oppgave);
        return oppgave.getId().toString();
    }

    /**
     * Observer endringer i BehandlingStatus og håndter oppgaver deretter.
     */
    public void observerBehandlingStatus(@Observes BehandlingAvsluttetEvent statusEvent) {
        Long behandlingId = statusEvent.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        opprettTaskAvsluttOppgave(behandling);
    }

    /*
     * Spesielle oppgavetyper - flytting til Infotrygd og behandling i NØS
     */
    public String opprettOppgaveSakSkalTilInfotrygd(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var fagsak = behandling.getFagsak();

        var orequest = createRestRequestBuilder(fagsak.getSaksnummer(), fagsak.getAktørId(), behandling.getBehandlendeEnhet(), SAK_MÅ_FLYTTES_TIL_INFOTRYGD,
            Prioritet.NORM, DEFAULT_OPPGAVEFRIST_DAGER, mapYtelseTypeTilTema(fagsak.getYtelseType()))
            .medBehandlingstema(BehandlingTema.finnForFagsakYtelseType(fagsak.getYtelseType()).getOffisiellKode())
            .medOppgavetype(OppgaveÅrsak.BEHANDLE_SAK_IT.getKode());
        var oppgave = restKlient.opprettetOppgave(orequest);
        logger.info("GOSYS opprettet BEH/IT oppgave {}", oppgave);
        return oppgave.getId().toString();
    }

    public String opprettOppgaveStopUtbetalingAvARENAYtelse(long behandlingId, LocalDate førsteUttaksdato) {
        final String BESKRIVELSE = "Samordning arenaytelse. Vedtak foreldrepenger fra %s";
        var beskrivelse = String.format(BESKRIVELSE, førsteUttaksdato);

        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        return opprettOkonomiSettPåVent(beskrivelse, behandling);
    }

    private String opprettOkonomiSettPåVent(String beskrivelse, Behandling behandling) {
        var fagsak = behandling.getFagsak();
        var orequest = createRestRequestBuilder(fagsak.getSaksnummer(), fagsak.getAktørId(), behandling.getBehandlendeEnhet(), beskrivelse, Prioritet.HOY, DEFAULT_OPPGAVEFRIST_DAGER, mapYtelseTypeTilTema(fagsak.getYtelseType()))
            .medTildeltEnhetsnr(NØS_ANSVARLIG_ENHETID)
            .medTemagruppe(null)
            .medTema(NØS_TEMA)
            .medBehandlingstema(NØS_BEH_TEMA)
            .medOppgavetype(OppgaveÅrsak.SETTVENT.getKode());
        var oppgave = restKlient.opprettetOppgave(orequest);
        logger.info("GOSYS opprettet NØS oppgave {}", oppgave);
        return oppgave.getId().toString();
    }
}
