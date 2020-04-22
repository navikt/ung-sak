package no.nav.k9.sak.produksjonsstyring.oppgavebehandling;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.events.BehandlingStatusEvent.BehandlingAvsluttetEvent;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.task.AvsluttOppgaveTaskProperties;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.meldinger.WSOpprettOppgaveResponse;
import no.nav.tjeneste.virksomhet.oppgave.v3.informasjon.oppgave.Oppgave;
import no.nav.tjeneste.virksomhet.oppgave.v3.meldinger.FinnOppgaveListeResponse;
import no.nav.vedtak.felles.integrasjon.behandleoppgave.BehandleoppgaveConsumer;
import no.nav.vedtak.felles.integrasjon.behandleoppgave.BrukerType;
import no.nav.vedtak.felles.integrasjon.behandleoppgave.FagomradeKode;
import no.nav.vedtak.felles.integrasjon.behandleoppgave.FerdigstillOppgaveRequestMal;
import no.nav.vedtak.felles.integrasjon.behandleoppgave.PrioritetKode;
import no.nav.vedtak.felles.integrasjon.behandleoppgave.opprett.OpprettOppgaveRequest;
import no.nav.vedtak.felles.integrasjon.oppgave.FinnOppgaveListeFilterMal;
import no.nav.vedtak.felles.integrasjon.oppgave.FinnOppgaveListeRequestMal;
import no.nav.vedtak.felles.integrasjon.oppgave.FinnOppgaveListeSokMal;
import no.nav.vedtak.felles.integrasjon.oppgave.OppgaveConsumer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.sikkerhet.context.SubjectHandler;

@ApplicationScoped
public class OppgaveTjeneste {
    private static final int DEFAULT_OPPGAVEFRIST_DAGER = 1;

    //FIXME(k9) hva skal fristen være?
    private static final int DEFAULT_OPPGAVEFRIST_OMSORGSPENGER_DAGER = 21;

    private static final String DEFAULT_OPPGAVEBESKRIVELSE = "Må behandle sak i VL!";

    private static final String OMSORGSPENGERSAK_MÅ_FLYTTES_TIL_INFOTRYGD = "Omsorgspengersak må flyttes til Infotrygd";

    private static final String NØS_ANSVARLIG_ENHETID = "4151";
    private static final String NØS_OMS_UNDERKATEGORI = "OMS_STO";

    /* 
     * TODO K9: Denne koden var "FEILUTB_FOR". Tror ikke det er opprettet en kode for OMS ennå,
     *          men bruker "FEILUTB_OMS" (bedre at det feiler grunnet manglende kode enn at
     *          oppgaven ligger feil.
     */
    private static final String FEILUTBETALING_UNDERKATEGORI = "FEILUTB_OMS";

    private Logger logger = LoggerFactory.getLogger(OppgaveTjeneste.class);
    private FagsakRepository fagsakRepository;

    private BehandlingRepository behandlingRepository;
    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;
    private BehandleoppgaveConsumer service;
    private ProsessTaskRepository prosessTaskRepository;
    private TpsTjeneste tpsTjeneste;
    private OppgaveConsumer oppgaveConsumer;

    OppgaveTjeneste() {
        // for CDI proxy
    }

    @Inject
    public OppgaveTjeneste(BehandlingRepositoryProvider repositoryProvider,
                           OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository,
                           BehandleoppgaveConsumer service, OppgaveConsumer oppgaveConsumer,
                           ProsessTaskRepository prosessTaskRepository, TpsTjeneste tpsTjeneste) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.oppgaveBehandlingKoblingRepository = oppgaveBehandlingKoblingRepository;
        this.service = service;
        this.oppgaveConsumer = oppgaveConsumer;
        this.prosessTaskRepository = prosessTaskRepository;
        this.tpsTjeneste = tpsTjeneste;
    }


    /**
     * Observer endringer i BehandlingStatus og håndter oppgaver deretter.
     */
    public void observerBehandlingStatus(@Observes BehandlingAvsluttetEvent statusEvent) {
        Long behandlingId = statusEvent.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        opprettTaskAvsluttOppgave(behandling);
    }

    public String opprettOppgaveFeilutbetaling(BehandlingReferanse ref, String beskrivelse) {
        Behandling behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        Fagsak fagsak = behandling.getFagsak();
        Personinfo personSomBehandles = hentPersonInfo(behandling.getAktørId());
        OpprettOppgaveRequest request = createRequest(fagsak, personSomBehandles, OppgaveÅrsak.VURDER_KONS_OMS_YTELSE, behandling.getBehandlendeEnhet(),
            beskrivelse, hentPrioritetKode(false), DEFAULT_OPPGAVEFRIST_DAGER, FEILUTBETALING_UNDERKATEGORI);
        WSOpprettOppgaveResponse response = service.opprettOppgave(request);
        return response.getOppgaveId();
    }

    public String opprettMedPrioritetOgBeskrivelseBasertPåFagsakId(Long fagsakId, OppgaveÅrsak oppgaveÅrsak, String enhetsId, String beskrivelse, boolean høyPrioritet) {
        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
        Personinfo personSomBehandles = hentPersonInfo(fagsak.getAktørId());
        OpprettOppgaveRequest request = createRequest(fagsak, personSomBehandles, oppgaveÅrsak, enhetsId, beskrivelse,
            hentPrioritetKode(høyPrioritet), DEFAULT_OPPGAVEFRIST_DAGER);
        WSOpprettOppgaveResponse response = service.opprettOppgave(request);
        return response.getOppgaveId();
    }

    public String opprettOppgaveStopUtbetalingAvARENAYtelse(long behandlingId, LocalDate førsteUttaksdato) {
        final String BESKRIVELSE = "Samordning arenaytelse. Vedtak på %s fra %s";
        final Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        final String ytelsesnavn = behandling.getFagsakYtelseType().getNavn();
        final Saksnummer saksnummer = behandling.getFagsak().getSaksnummer();
        
        OpprettOppgaveRequest request = OpprettOppgaveRequest.builder()
            .medBeskrivelse(String.format(BESKRIVELSE, ytelsesnavn, førsteUttaksdato))
            .medOpprettetAvEnhetId(Integer.parseInt(behandling.getBehandlendeEnhet()))
            .medAnsvarligEnhetId(NØS_ANSVARLIG_ENHETID)
            .medFagomradeKode(FagomradeKode.STO.getKode())
            .medOppgavetypeKode(OppgaveÅrsak.SETT_ARENA_UTBET_VENT.getKode())
            .medUnderkategoriKode(NØS_OMS_UNDERKATEGORI)
            .medPrioritetKode(PrioritetKode.HOY_STO.name())
            .medLest(false)
            .medAktivFra(LocalDate.now())
            .medAktivTil(helgeJustertFrist(LocalDate.now().plusDays(DEFAULT_OPPGAVEFRIST_DAGER)))
            .medBrukerTypeKode(BrukerType.PERSON)
            .medFnr(hentPersonInfo(behandling.getAktørId()).getPersonIdent().getIdent())
            .medSaksnummer(saksnummer.getVerdi())
            .build();

        WSOpprettOppgaveResponse response = service.opprettOppgave(request);
        return response.getOppgaveId();
    }

    public String opprettOppgaveSettUtbetalingPåVentPrivatArbeidsgiver(long behandlingId,
                                                                       LocalDate førsteUttaksdato,
                                                                       LocalDate vedtaksdato,
                                                                       AktørId arbeidsgiverAktørId) {

        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Saksnummer saksnummer = behandling.getFagsak().getSaksnummer();
        String arbeidsgiverIdent = hentPersonInfo(arbeidsgiverAktørId).getPersonIdent().getIdent();

        final String beskrivelse = String.format("Refusjon til privat arbeidsgiver," +
            "Saksnummer: %s," +
            "Vedtaksdato: %s," +
            "Dato for første utbetaling: %s," +
            "Fødselsnummer arbeidsgiver: %s", saksnummer.getVerdi(), vedtaksdato, førsteUttaksdato, arbeidsgiverIdent);

        OpprettOppgaveRequest request = OpprettOppgaveRequest.builder()
            .medBeskrivelse(beskrivelse)
            .medOpprettetAvEnhetId(Integer.parseInt(behandling.getBehandlendeEnhet()))
            .medAnsvarligEnhetId(NØS_ANSVARLIG_ENHETID)
            .medFagomradeKode(FagomradeKode.STO.getKode())
            .medOppgavetypeKode(OppgaveÅrsak.SETT_ARENA_UTBET_VENT.getKode())
            .medUnderkategoriKode(NØS_OMS_UNDERKATEGORI)
            .medPrioritetKode(PrioritetKode.HOY_STO.name())
            .medLest(false)
            .medAktivFra(LocalDate.now())
            .medAktivTil(helgeJustertFrist(LocalDate.now().plusDays(DEFAULT_OPPGAVEFRIST_DAGER)))
            .medBrukerTypeKode(BrukerType.PERSON)
            .medFnr(hentPersonInfo(behandling.getAktørId()).getPersonIdent().getIdent())
            .medSaksnummer(saksnummer.getVerdi())
            .build();
        WSOpprettOppgaveResponse response = service.opprettOppgave(request);
        return response.getOppgaveId();
    }

    public String opprettOppgaveSakSkalTilInfotrygd(String behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Saksnummer saksnummer = behandling.getFagsak().getSaksnummer();
        OpprettOppgaveRequest request = OpprettOppgaveRequest.builder()
            .medOpprettetAvEnhetId(Integer.parseInt(behandling.getBehandlendeEnhet()))
            .medAnsvarligEnhetId(behandling.getBehandlendeEnhet())
            .medFagomradeKode(FagomradeKode.OMS.getKode())
            .medFnr(hentPersonInfo(behandling.getAktørId()).getPersonIdent().getIdent())
            .medAktivFra(LocalDate.now())
            .medAktivTil(helgeJustertFrist(LocalDate.now().plusDays(DEFAULT_OPPGAVEFRIST_OMSORGSPENGER_DAGER)))
            .medOppgavetypeKode(OppgaveÅrsak.BEHANDLE_SAK_INFOTRYGD_OMS.getKode())
            .medSaksnummer(saksnummer.getVerdi())
            .medPrioritetKode(PrioritetKode.NORM_OMS.toString())
            .medBeskrivelse(OMSORGSPENGERSAK_MÅ_FLYTTES_TIL_INFOTRYGD)
            .medLest(false)
            .build();

        WSOpprettOppgaveResponse response = service.opprettOppgave(request);
        return response.getOppgaveId();
    }

    private PrioritetKode hentPrioritetKode(boolean høyPrioritet) {
        return høyPrioritet ? PrioritetKode.HOY_OMS : PrioritetKode.NORM_OMS;
    }

    public void avslutt(Long behandlingId, OppgaveÅrsak oppgaveÅrsak) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        List<OppgaveBehandlingKobling> oppgaveBehandlingKoblinger = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandlingId);
        Optional<OppgaveBehandlingKobling> oppgave = OppgaveBehandlingKobling.getAktivOppgaveMedÅrsak(oppgaveÅrsak, oppgaveBehandlingKoblinger);
        if (oppgave.isPresent()) {
            avsluttOppgave(behandling, oppgave.get());
        } else {
            OppgaveFeilmeldinger.FACTORY.oppgaveMedÅrsakIkkeFunnet(oppgaveÅrsak.getKode(), behandlingId).log(logger);
        }
    }

    public void avslutt(String behandlingId, String oppgaveId) {
        Optional<OppgaveBehandlingKobling> oppgave = oppgaveBehandlingKoblingRepository.hentOppgaveBehandlingKobling(oppgaveId);
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        if (oppgave.isPresent()) {
            avsluttOppgave(behandling, oppgave.get());
        } else {
            OppgaveFeilmeldinger.FACTORY.oppgaveMedIdIkkeFunnet(oppgaveId, behandlingId).log(logger);
        }
    }

    private void avsluttOppgave(Behandling behandling, OppgaveBehandlingKobling aktivOppgave) {
        if (!aktivOppgave.isFerdigstilt()) {
            ferdigstillOppgaveBehandlingKobling(aktivOppgave);
        }
        FerdigstillOppgaveRequestMal request = createFerdigstillRequest(behandling, aktivOppgave.getOppgaveId());
        service.ferdigstillOppgave(request);
    }


    private void ferdigstillOppgaveBehandlingKobling(OppgaveBehandlingKobling aktivOppgave) {
        aktivOppgave.ferdigstillOppgave(SubjectHandler.getSubjectHandler().getUid());
        oppgaveBehandlingKoblingRepository.lagre(aktivOppgave);
    }

    public void avsluttOppgaveOgStartTask(Behandling behandling, OppgaveÅrsak oppgaveÅrsak, String taskType) {
        ProsessTaskGruppe taskGruppe = new ProsessTaskGruppe();
        taskGruppe.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        opprettTaskAvsluttOppgave(behandling, oppgaveÅrsak, false).ifPresent(taskGruppe::addNesteSekvensiell);
        taskGruppe.addNesteSekvensiell(opprettProsessTask(behandling, taskType));

        taskGruppe.setCallIdFraEksisterende();

        prosessTaskRepository.lagre(taskGruppe);
    }

    public List<Oppgaveinfo> hentOppgaveListe(AktørId aktørId, List<String> oppgaveÅrsaker) {
        PersonIdent personIdent = hentPersonInfo(aktørId).getPersonIdent();
        FinnOppgaveListeRequestMal.Builder requestMalBuilder = new FinnOppgaveListeRequestMal.Builder();
        FinnOppgaveListeSokMal sokMal = FinnOppgaveListeSokMal.builder().medBrukerId(personIdent.getIdent()).build();
        FinnOppgaveListeFilterMal filterMal = FinnOppgaveListeFilterMal.builder().medOppgavetypeKodeListe(oppgaveÅrsaker).build();
        FinnOppgaveListeRequestMal requestMal = requestMalBuilder.medSok(sokMal).medFilter(filterMal).build();
        FinnOppgaveListeResponse finnOppgaveListeResponse = oppgaveConsumer.finnOppgaveListe(requestMal);
        List<Oppgave> oppgaveListe = finnOppgaveListeResponse.getOppgaveListe();
        return oppgaveListe.stream().map(ol -> new Oppgaveinfo(ol.getOppgavetype().getKode(), ol.getStatus().getKode())).collect(Collectors.toList());
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
            if (OppgaveÅrsak.BEHANDLE_SAK_INFOTRYGD_OMS.equals(aktivOppgave.getOppgaveÅrsak())) {
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
        return prosessTask;
    }

    private FerdigstillOppgaveRequestMal createFerdigstillRequest(Behandling behandling, String oppgaveId) {
        FerdigstillOppgaveRequestMal.Builder builder = FerdigstillOppgaveRequestMal.builder().medOppgaveId(oppgaveId);
        if (behandling.getBehandlendeEnhet() != null) {
            builder.medFerdigstiltAvEnhetId(Integer.parseInt(behandling.getBehandlendeEnhet()));
        }
        return builder.build();
    }

    private Personinfo hentPersonInfo(AktørId aktørId) {
        return tpsTjeneste.hentBrukerForAktør(aktørId)
            .orElseThrow(() -> OppgaveFeilmeldinger.FACTORY.identIkkeFunnet(aktørId).toException());
    }

    private OpprettOppgaveRequest createRequest(Fagsak fagsak, Personinfo personinfo, OppgaveÅrsak oppgaveÅrsak,
                                                String enhetsId, String beskrivelse, PrioritetKode prioritetKode,
                                                int fristDager) {
        return createRequest(fagsak, personinfo, oppgaveÅrsak, enhetsId, beskrivelse, prioritetKode, fristDager, finnUnderkategoriKode(fagsak.getYtelseType()));
    }

    private String finnUnderkategoriKode(FagsakYtelseType fagsakYtelseType) {
        switch (fagsakYtelseType) {
        case PLEIEPENGER_SYKT_BARN: return "PLEIEPENGERSY_OMS";
        case OMSORGSPENGER: return "OMSORGSPE_OMS";
        case FRISINN: throw new IllegalStateException("Frisinn har ikke Gosyskode -- skal dette bestilles?");
        case OPPLÆRINGSPENGER: return "OPPLARINGSPE_OMS";
        case PLEIEPENGER_NÆRSTÅENDE: return "PLEIEPENGERPA_OMS";
        default: throw OppgaveFeilmeldinger.FACTORY.underkategoriIkkeFunnetForFagsakYtelseType(fagsakYtelseType).toException();
        }
    }

    private OpprettOppgaveRequest createRequest(Fagsak fagsak, Personinfo personinfo, OppgaveÅrsak oppgaveÅrsak,
                                                String enhetsId, String beskrivelse, PrioritetKode prioritetKode,
                                                int fristDager, String underkategoriKode) {

        OpprettOppgaveRequest.Builder builder = OpprettOppgaveRequest.builder();

        return builder
            .medOpprettetAvEnhetId(Integer.parseInt(enhetsId))
            .medAnsvarligEnhetId(enhetsId)
            .medFagomradeKode(FagomradeKode.OMS.getKode())
            .medFnr(personinfo.getPersonIdent().getIdent())
            .medBrukerTypeKode(BrukerType.PERSON)
            .medAktivFra(LocalDate.now())
            .medAktivTil(helgeJustertFrist(LocalDate.now().plusDays(fristDager)))
            .medOppgavetypeKode(oppgaveÅrsak.getKode())
            .medSaksnummer(fagsak.getSaksnummer() != null ? fagsak.getSaksnummer().getVerdi() : fagsak.getId().toString()) // Mer iht PK-38815
            .medPrioritetKode(prioritetKode.toString())
            .medBeskrivelse(beskrivelse)
            .medLest(false)
            .medUnderkategoriKode(underkategoriKode)
            .build();
    }

    // Sett frist til mandag hvis fristen er i helgen.
    private LocalDate helgeJustertFrist(LocalDate dato) {
        if (dato.getDayOfWeek().getValue() > DayOfWeek.FRIDAY.getValue()) {
            return dato.plusDays(1L + DayOfWeek.SUNDAY.getValue() - dato.getDayOfWeek().getValue());
        }
        return dato;
    }
}
