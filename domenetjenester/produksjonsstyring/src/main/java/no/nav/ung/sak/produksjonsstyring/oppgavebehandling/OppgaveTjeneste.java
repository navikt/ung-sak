package no.nav.ung.sak.produksjonsstyring.oppgavebehandling;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import no.nav.ung.kodeverk.behandling.BehandlingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.oppgave.v1.OppgaveRestKlient;
import no.nav.k9.felles.integrasjon.oppgave.v1.OpprettOppgave;
import no.nav.k9.felles.integrasjon.oppgave.v1.Prioritet;
import no.nav.ung.kodeverk.behandling.BehandlingTema;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingStatusEvent.BehandlingAvsluttetEvent;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.task.AvsluttOppgaveTask;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.k9.sikkerhet.context.SubjectHandler;

@ApplicationScoped
public class OppgaveTjeneste {
    private static final Logger logger = LoggerFactory.getLogger(OppgaveTjeneste.class);
    private static final int DEFAULT_OPPGAVEFRIST_DAGER = 1;

    private static final String SAK_MÅ_FLYTTES_TIL_INFOTRYGD = "Sak må flyttes til Infotrygd";
    private static final String DEFAULT_OPPGAVEBESKRIVELSE = "Må behandle sak i VL";
    private static final String OPPGAVEBESKRIVELSE_ANKE = "Anke er ferdigbehandlet med utfall ";

    private static final String NØS_ANSVARLIG_ENHETID = "4151";
    private static final String NØS_BEH_TEMA = "ab0271";
    private static final String NØS_TEMA = "STO";

    private FagsakRepository fagsakRepository;

    private BehandlingRepository behandlingRepository;
    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;
    private ProsessTaskTjeneste taskTjeneste;
    private OppgaveRestKlient restKlient;

    OppgaveTjeneste() {
        // for CDI proxy
    }

    @Inject
    public OppgaveTjeneste(BehandlingRepositoryProvider repositoryProvider,
                           OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository,
                           OppgaveRestKlient restKlient,
                           ProsessTaskTjeneste taskTjeneste) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.oppgaveBehandlingKoblingRepository = oppgaveBehandlingKoblingRepository;
        this.restKlient = restKlient;
        this.taskTjeneste = taskTjeneste;
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

    public String opprettVkyOppgaveOverlappendeYtelse(BehandlingReferanse ref, String beskrivelse, String oppgavetema, String behandlingstype, String enhetId) {
        Behandling behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        Fagsak fagsak = behandling.getFagsak();

        var request = createRestRequestBuilder(
            fagsak.getSaksnummer(),
            fagsak.getAktørId(),
            behandling.getBehandlendeEnhet(),
            beskrivelse,
            Prioritet.NORM,
            DEFAULT_OPPGAVEFRIST_DAGER,
            oppgavetema
        ).medTildeltEnhetsnr(enhetId)
            .medBehandlingstype(behandlingstype)
            .medOppgavetype(OppgaveÅrsak.VURDER_KONSEKVENS_YTELSE.getKode());

        var response = restKlient.opprettetOppgave(request.build());
        return response.getId().toString();
    }

    public String opprettOppgaveFeilutbetaling(BehandlingReferanse ref, String beskrivelse) {
        Behandling behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        Fagsak fagsak = behandling.getFagsak();

        var request = createRestRequestBuilder(fagsak.getSaksnummer(), fagsak.getAktørId(), behandling.getBehandlendeEnhet(), beskrivelse, Prioritet.NORM, DEFAULT_OPPGAVEFRIST_DAGER,
            mapYtelseTypeTilTema(fagsak.getYtelseType()))
                .medOppgavetype(OppgaveÅrsak.VURDER_KONSEKVENS_YTELSE.getKode());
        var response = restKlient.opprettetOppgave(request.build());
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
        var tema = ytelseType.getOppgavetema();
        if (tema != null) {
            return tema;
        } else {
            throw new UnsupportedOperationException("Støtter ikke ytelsestype " + ytelseType);
        }
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
            ProsessTaskData avsluttOppgaveTask = opprettProsessTask(behandling);
            avsluttOppgaveTask.setOppgaveId(aktivOppgave.getOppgaveId());
            if (skalLagres) {
                avsluttOppgaveTask.setCallIdFraEksisterende();
                taskTjeneste.lagre(avsluttOppgaveTask);
            }
            return Optional.of(avsluttOppgaveTask);
        } else {
            return Optional.empty();
        }
    }

    private ProsessTaskData opprettProsessTask(Behandling behandling) {
        ProsessTaskData prosessTask = ProsessTaskData.forProsessTask(AvsluttOppgaveTask.class);
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
            throw OppgaveFeilmeldinger.FACTORY.feilVedHentingAvOppgaver(ytelseType, oppgavetype, e).toException();
        }
    }

    public String opprettMedPrioritetOgBeskrivelseBasertPåFagsakId(Long fagsakId, OppgaveÅrsak oppgaveÅrsak, String enhetsId, String beskrivelse, boolean høyPrioritet) {
        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);

        var orequest = createRestRequestBuilder(fagsak.getSaksnummer(), fagsak.getAktørId(), enhetsId, beskrivelse, høyPrioritet ? Prioritet.HOY : Prioritet.NORM, DEFAULT_OPPGAVEFRIST_DAGER,
            mapYtelseTypeTilTema(fagsak.getYtelseType()))
                .medBehandlingstema(BehandlingTema.finnForFagsakYtelseType(fagsak.getYtelseType()).getOffisiellKode())
                .medOppgavetype(oppgaveÅrsak.getKode());
        var oppgave = restKlient.opprettetOppgave(orequest.build());
        logger.info("GOSYS opprettet VURDER VL oppgave {}", oppgave);
        return oppgave.getId().toString();
    }

    public String opprettBasertPåBehandlingId(String behandlingId, OppgaveÅrsak oppgaveÅrsak) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        return opprettOppgave(behandling, oppgaveÅrsak, DEFAULT_OPPGAVEBESKRIVELSE, Prioritet.NORM, DEFAULT_OPPGAVEFRIST_DAGER);
    }

    public String opprettBehandleOppgaveForBehandling(String behandlingId) {
        return opprettBehandleOppgaveForBehandlingMedPrioritetOgFrist(behandlingId, DEFAULT_OPPGAVEBESKRIVELSE, false, DEFAULT_OPPGAVEFRIST_DAGER);
    }

    public String opprettBehandleOppgaveForBehandlingMedPrioritetOgFrist(String behandlingId, String beskrivelse, boolean høyPrioritet, int fristDager) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        OppgaveÅrsak oppgaveÅrsak = behandling.erRevurdering() ? OppgaveÅrsak.REVURDER_VL : OppgaveÅrsak.BEHANDLE_SAK_VL;
        return opprettOppgave(behandling, oppgaveÅrsak, beskrivelse, hentPrioritetKode(høyPrioritet), fristDager);
    }

    private String opprettOppgave(Behandling behandling, OppgaveÅrsak oppgaveÅrsak, String beskrivelse, Prioritet prioritet, int fristDager) {
        List<OppgaveBehandlingKobling> oppgaveBehandlingKoblinger = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandling.getId());
        if (OppgaveBehandlingKobling.getAktivOppgaveMedÅrsak(oppgaveÅrsak, oppgaveBehandlingKoblinger).isPresent()) {
            // skal ikke opprette oppgave med samme årsak når behandlingen allerede har en åpen oppgave med den årsaken knyttet til seg
            return null;
        }
        Fagsak fagsak = behandling.getFagsak();
        var orequest = createRestRequestBuilder(fagsak.getSaksnummer(), fagsak.getAktørId(), behandling.getBehandlendeEnhet(), beskrivelse, prioritet, fristDager,
            mapYtelseTypeTilTema(fagsak.getYtelseType()))
            .medBehandlingstema(BehandlingTema.finnForFagsakYtelseType(fagsak.getYtelseType()).getOffisiellKode())
            .medOppgavetype(oppgaveÅrsak.getKode());
        var oppgave = restKlient.opprettetOppgave(orequest.build());
        logger.info("Ungsak GOSYS opprettet oppgave med oppgaveid={} for behandling={}, saksnummer={}", oppgave.getId(), behandling.getId(), fagsak.getSaksnummer());
        return behandleRespons(behandling, oppgaveÅrsak, oppgave.getId().toString(), fagsak.getSaksnummer());
    }

    private Prioritet hentPrioritetKode(boolean høyPrioritet) {
        return høyPrioritet ? Prioritet.HOY : Prioritet.NORM;
    }


    /**
     * Observer endringer i BehandlingStatus og håndter oppgaver deretter.
     */
    public void observerBehandlingStatus(@Observes BehandlingAvsluttetEvent statusEvent) {
        Long behandlingId = statusEvent.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        opprettTaskAvsluttOppgave(behandling);
    }

    public String opprettOppgaveStopUtbetalingAvARENAYtelse(long behandlingId, LocalDate førsteUttaksdato) {
        final String BESKRIVELSE = "Samordning arenaytelse. Vedtak i K9 (Omsorgspenger, Pleiepenger og opplæringspenger) fra %s";
        var beskrivelse = String.format(BESKRIVELSE, førsteUttaksdato);

        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        return opprettOkonomiSettPåVent(beskrivelse, behandling);
    }

    private String opprettOkonomiSettPåVent(String beskrivelse, Behandling behandling) {
        var fagsak = behandling.getFagsak();
        var request = createRestRequestBuilder(fagsak.getSaksnummer(), fagsak.getAktørId(), behandling.getBehandlendeEnhet(), beskrivelse, Prioritet.HOY, DEFAULT_OPPGAVEFRIST_DAGER,
            mapYtelseTypeTilTema(fagsak.getYtelseType()))
                .medTildeltEnhetsnr(NØS_ANSVARLIG_ENHETID)
                .medTemagruppe(null)
                .medTema(NØS_TEMA)
                .medBehandlingstema(NØS_BEH_TEMA)
                .medOppgavetype(OppgaveÅrsak.SETTVENT.getKode());
        var oppgave = restKlient.opprettetOppgave(request.build());
        logger.info("GOSYS opprettet NØS oppgave {}", oppgave);
        return oppgave.getId().toString();
    }

    public String opprettOppgaveOmAnke(String behandlingId, OppgaveÅrsak oppgaveÅrsak, String utfall) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var fagsak = behandling.getFagsak();
        var orequest = createRestRequestBuilder(fagsak.getSaksnummer(), fagsak.getAktørId(), behandling.getBehandlendeEnhet(),
            OPPGAVEBESKRIVELSE_ANKE + utfall, Prioritet.NORM, DEFAULT_OPPGAVEFRIST_DAGER,
            mapYtelseTypeTilTema(fagsak.getYtelseType()))
            .medBehandlingstype(BehandlingType.ANKE.getOffisiellKode())
            .medOppgavetype(oppgaveÅrsak.getKode());
        var oppgave = restKlient.opprettetOppgave(orequest.build());
        logger.info("Ung sak GOSYS oppretter oppgave" + oppgave);
        return behandleRespons(behandling, oppgaveÅrsak, oppgave.getId().toString(), fagsak.getSaksnummer());
    }

    private String behandleRespons(Behandling behandling, OppgaveÅrsak oppgaveÅrsak, String oppgaveId,
                                   Saksnummer saksnummer) {
        OppgaveBehandlingKobling oppgaveBehandlingKobling = new OppgaveBehandlingKobling(oppgaveÅrsak, oppgaveId, saksnummer, behandling);
        oppgaveBehandlingKoblingRepository.lagre(oppgaveBehandlingKobling);
        return oppgaveId;
    }
}
