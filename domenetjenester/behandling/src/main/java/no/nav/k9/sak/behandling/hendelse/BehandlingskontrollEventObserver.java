package no.nav.k9.sak.behandling.hendelse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.hendelse.EventHendelse;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.hendelse.produksjonsstyring.PubliserEventTask;
import no.nav.k9.sak.behandling.hendelse.produksjonsstyring.PubliserEventTaskImpl;
import no.nav.k9.sak.behandling.hendelse.produksjonsstyring.PubliserProduksjonsstyringHendelseTask;
import no.nav.k9.sak.behandling.hendelse.produksjonsstyring.PubliserProduksjonsstyringHendelseTaskImpl;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.k9.sak.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.k9.sak.behandlingskontroll.events.BehandlingskontrollEvent;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.k9.sak.kontrakt.aksjonspunkt.AksjonspunktTilstandDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingProsessHendelse;
import no.nav.k9.sak.kontrakt.produksjonsstyring.los.ProduksjonsstyringAksjonspunktHendelse;
import no.nav.k9.sak.kontrakt.produksjonsstyring.los.ProduksjonsstyringBehandlingAvsluttetHendelse;
import no.nav.k9.sak.kontrakt.produksjonsstyring.los.ProduksjonsstyringBehandlingOpprettetHendelse;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.perioder.VurdertSøktPeriode.SøktPeriodeData;

@ApplicationScoped
public class BehandlingskontrollEventObserver {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private ProsessTaskTjeneste prosessTaskRepository;
    private BehandlingRepository behandlingRepository;
    private Instance<VurderSøknadsfristTjeneste<?>> søknadsfristTjenester;

    public BehandlingskontrollEventObserver() {
    }

    @Inject
    public BehandlingskontrollEventObserver(ProsessTaskTjeneste prosessTaskRepository,
            BehandlingRepository behandlingRepository,
            @Any Instance<VurderSøknadsfristTjeneste<?>> søknadsfristTjenester) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.behandlingRepository = behandlingRepository;
        this.søknadsfristTjenester = søknadsfristTjenester;
    }

    public void observerStoppetEvent(@Observes BehandlingskontrollEvent.StoppetEvent event) {
        try {
            ProsessTaskData prosessTaskData = opprettProsessTaskBehandlingprosess(event.getBehandlingId(), EventHendelse.BEHANDLINGSKONTROLL_EVENT);
            prosessTaskRepository.lagre(prosessTaskData);
        } catch (Exception ex) {
            log.warn("Publisering av StoppetEvent feilet", ex);
        }
    }

    // Lytter på AksjonspunkterFunnetEvent, filtrer ut når behandling er satt manuelt på vent og legger melding på kafka
    public void observerAksjonspunkterFunnetEvent(@Observes AksjonspunktStatusEvent event) {
        if (event.getAksjonspunkter().stream().anyMatch(e -> e.erOpprettet() && AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT.equals(e.getAksjonspunktDefinisjon()))) {
            try {
                ProsessTaskData prosessTaskData = opprettProsessTaskBehandlingprosess(event.getBehandlingId(), EventHendelse.AKSJONSPUNKT_OPPRETTET);
                prosessTaskRepository.lagre(prosessTaskData);
            } catch (Exception ex) {
                log.warn("Publisering av AksjonspunkterFunnetEvent feilet", ex);
            }
        }

        try {
            ProsessTaskData prosessTaskData = opprettProsessTaskAksjonspunkthendelse(event.getBehandlingId(), event);
            prosessTaskRepository.lagre(prosessTaskData);
        } catch (Exception ex) {
            log.warn("Publisering av Aksjonspunkthendelse feilet", ex);
        }
    }

    public void observerBehandlingOpprettetEvent(@Observes BehandlingStatusEvent.BehandlingOpprettetEvent event) {
        try {
            ProsessTaskData prosessTaskData = opprettProsessTaskBehandlingOpprettetEvent(event.getBehandlingId());
            prosessTaskRepository.lagre(prosessTaskData);
        } catch (Exception ex) {
            log.warn("Publisering av BehandlingOpprettetHendelse feilet", ex);
        }
    }


    public void observerBehandlingAvsluttetEvent(@Observes BehandlingStatusEvent.BehandlingAvsluttetEvent event) {
        try {
            ProsessTaskData prosessTaskData = opprettProsessTaskBehandlingprosess(event.getBehandlingId(), EventHendelse.AKSJONSPUNKT_AVBRUTT);
            prosessTaskRepository.lagre(prosessTaskData);
        } catch (Exception ex) {
            log.warn("Publisering av BehandlingAvsluttetEvent feilet", ex);
        }

        try {
            ProsessTaskData prosessTaskData = opprettProsessTaskBehandlingAvsluttetEvent(event.getBehandlingId());
            prosessTaskRepository.lagre(prosessTaskData);
        } catch (Exception ex) {
            log.warn("Publisering av BehandlingAvsluttetHendelse for produksjonsstyring feilet", ex);
        }
    }

    public void observerAksjonspunktHarEndretBehandlendeEnhetEvent(@Observes BehandlingEnhetEvent event) {
        try {
            ProsessTaskData prosessTaskData = opprettProsessTaskBehandlingprosess(event.getBehandlingId(), EventHendelse.AKSJONSPUNKT_HAR_ENDRET_BEHANDLENDE_ENHET);
            prosessTaskRepository.lagre(prosessTaskData);
        } catch (Exception ex) {
            log.warn("Publisering av AksjonspunktHarEndretBehandlendeEnhetEvent feilet", ex);
        }
    }

    private ProsessTaskData opprettProsessTaskBehandlingprosess(Long behandlingId, EventHendelse eventHendelse) throws IOException {
        ProsessTaskData taskData = ProsessTaskData.forProsessTask(PubliserEventTaskImpl.class);
        taskData.setCallIdFraEksisterende();
        taskData.setPrioritet(50);

        Optional<Behandling> behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingId);

        var dto = getProduksjonstyringEventDto(eventHendelse, behandling.get());
        var aksjonspunkter = dto.getAksjonspunktKoderMedStatusListe();
        taskData.setPayload(JsonObjectMapper.getJson(dto));
        taskData.setProperty(PubliserEventTask.PROPERTY_KEY, behandlingId.toString());
        taskData.setProperty(PubliserEventTask.BESKRIVELSE, String.valueOf(aksjonspunkter));
        return taskData;
    }


    private ProsessTaskData opprettProsessTaskBehandlingOpprettetEvent(Long behandlingId) throws IOException {
        ProsessTaskData taskData = ProsessTaskData.forProsessTask(PubliserProduksjonsstyringHendelseTaskImpl.class);
        taskData.setCallIdFraEksisterende();
        taskData.setPrioritet(50);

        Behandling behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingId).orElseThrow();
        Fagsak fagsak = behandling.getFagsak();

        ProduksjonsstyringBehandlingOpprettetHendelse dto = new ProduksjonsstyringBehandlingOpprettetHendelse(
            behandling.getUuid(),
            behandling.getOpprettetTidspunkt(),
            fagsak.getSaksnummer().getVerdi(),
            behandling.getFagsakYtelseType(),
            behandling.getType(),
            behandling.getBehandlingstidFrist(),
            fagsak.getPeriode().tilPeriode(),
            fagsak.getAktørId(),
            fagsak.getPleietrengendeAktørId(),
            fagsak.getRelatertPersonAktørId()
        );

        taskData.setPayload(JsonObjectMapper.getJson(dto));
        taskData.setProperty(PubliserProduksjonsstyringHendelseTask.PROPERTY_KEY, behandlingId.toString());
        taskData.setProperty(PubliserProduksjonsstyringHendelseTask.BESKRIVELSE, "BehandlingOpprettetHendelseTask");
        return taskData;
    }

    private ProsessTaskData opprettProsessTaskBehandlingAvsluttetEvent(Long behandlingId) throws IOException {
        ProsessTaskData taskData = ProsessTaskData.forProsessTask(PubliserProduksjonsstyringHendelseTaskImpl.class);
        taskData.setCallIdFraEksisterende();
        taskData.setPrioritet(50);

        Behandling behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingId).orElseThrow();
        ProduksjonsstyringBehandlingAvsluttetHendelse dto = new ProduksjonsstyringBehandlingAvsluttetHendelse(
            behandling.getUuid(),
            behandling.getOpprettetTidspunkt(),
            behandling.getBehandlingResultatType()
        );

        taskData.setPayload(JsonObjectMapper.getJson(dto));
        taskData.setProperty(PubliserProduksjonsstyringHendelseTask.PROPERTY_KEY, behandlingId.toString());
        taskData.setProperty(PubliserProduksjonsstyringHendelseTask.BESKRIVELSE, "BehandlingOpprettetHendelseTask");
        return taskData;
    }


    private ProsessTaskData opprettProsessTaskAksjonspunkthendelse(Long behandlingId, AksjonspunktStatusEvent hendelse) throws IOException {
        ProsessTaskData taskData = ProsessTaskData.forProsessTask(PubliserProduksjonsstyringHendelseTaskImpl.class);
        taskData.setCallIdFraEksisterende();
        taskData.setPrioritet(50);

        Behandling behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingId).orElseThrow();
        ProduksjonsstyringAksjonspunktHendelse dto = new ProduksjonsstyringAksjonspunktHendelse(
            behandling.getUuid(),
            LocalDateTime.now(),
            lagAksjonspunkttilstander(hendelse.getAksjonspunkter())
        );

        taskData.setPayload(JsonObjectMapper.getJson(dto));
        taskData.setProperty(PubliserProduksjonsstyringHendelseTask.PROPERTY_KEY, behandlingId.toString());
        taskData.setProperty(PubliserProduksjonsstyringHendelseTask.BESKRIVELSE, String.valueOf(dto.aksjonspunktTilstander));
        return taskData;
    }

    private List<AksjonspunktTilstandDto> lagAksjonspunkttilstander(Collection<Aksjonspunkt> aksjonspunkter) {
        return aksjonspunkter.stream().map(it ->
            new AksjonspunktTilstandDto(
                it.getAksjonspunktDefinisjon().getKode(),
                it.getStatus(),
                it.getVenteårsak(),
                it.getAnsvarligSaksbehandler(),
                it.getFristTid())
        ).toList();
    }

    private BehandlingProsessHendelse getProduksjonstyringEventDto(EventHendelse eventHendelse, Behandling behandling) {
        Map<String, String> aksjonspunktKoderMedStatusListe = new HashMap<>();
        var fagsak = behandling.getFagsak();
        behandling.getAksjonspunkter().forEach(aksjonspunkt -> aksjonspunktKoderMedStatusListe.put(aksjonspunkt.getAksjonspunktDefinisjon().getKode(), aksjonspunkt.getStatus().getKode()));
        
        final boolean nyeKrav = sjekkOmDetHarKommetNyeKrav(behandling);
        
        return BehandlingProsessHendelse.builder()
            .medEksternId(behandling.getUuid())
            .medEventTid(LocalDateTime.now())
            .medFagsystem(Fagsystem.K9SAK)
            .medSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi())
            .medAktørId(behandling.getAktørId().getId())
            .getBehandlingstidFrist(behandling.getBehandlingstidFrist())
            .medEventHendelse(eventHendelse)
            .medBehandlingStatus(behandling.getStatus().getKode())
            .medBehandlingSteg(behandling.getAktivtBehandlingSteg() == null ? null : behandling.getAktivtBehandlingSteg().getKode())
            .medYtelseTypeKode(behandling.getFagsakYtelseType().getKode())
            .medBehandlingTypeKode(behandling.getType().getKode())
            .medOpprettetBehandling(behandling.getOpprettetDato())
            .medBehandlingResultat(behandling.getBehandlingResultatType())
            .medAksjonspunktKoderMedStatusListe(aksjonspunktKoderMedStatusListe)
            .medAnsvarligSaksbehandlerForTotrinn(behandling.getAnsvarligSaksbehandler())
            .medBehandlendeEnhet(behandling.getBehandlendeEnhet())
            .medFagsakPeriode(fagsak.getPeriode().tilPeriode())
            .medPleietrengendeAktørId(fagsak.getPleietrengendeAktørId())
            .medRelatertPartAktørId(fagsak.getRelatertPersonAktørId())
            .medAnsvarligBeslutterForTotrinn(behandling.getAnsvarligBeslutter())
            .medAksjonspunktTilstander(lagAksjonspunkttilstander(behandling.getAksjonspunkter()))
            .medNyeKrav(nyeKrav)
            .build();
    }
    
    public boolean sjekkOmDetHarKommetNyeKrav(Behandling behandling) {
        final var behandlingRef = BehandlingReferanse.fra(behandling);
        final var søknadsfristTjeneste = finnVurderSøknadsfristTjeneste(behandlingRef);
        if (søknadsfristTjeneste == null) {
            return false;
        }
        
        final Set<KravDokument> kravdokumenter = søknadsfristTjeneste.relevanteKravdokumentForBehandling(behandlingRef);
        if (kravdokumenter.isEmpty()) {
            return false;
        }
        
        final LocalDateTimeline<KravDokument> eldsteKravTidslinje = hentKravdokumenterMedEldsteKravFørst(behandlingRef, søknadsfristTjeneste);
        
        return eldsteKravTidslinje
                .stream()
                .anyMatch(it -> kravdokumenter.stream()
                    .anyMatch(at -> at.getJournalpostId().equals(it.getValue().getJournalpostId())));
    }

    private LocalDateTimeline<KravDokument> hentKravdokumenterMedEldsteKravFørst(BehandlingReferanse behandlingRef,
            VurderSøknadsfristTjeneste<SøktPeriodeData> søknadsfristTjeneste) {
        final Map<KravDokument, List<SøktPeriode<SøktPeriodeData>>> kravdokumenterMedPeriode = søknadsfristTjeneste.hentPerioderTilVurdering(behandlingRef);
        final var kravdokumenterMedEldsteFørst = kravdokumenterMedPeriode.keySet()
                .stream()
                .sorted(Comparator.comparing(KravDokument::getInnsendingsTidspunkt))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        
        LocalDateTimeline<KravDokument> eldsteKravTidslinje = LocalDateTimeline.empty();
        for (KravDokument kravdokument : kravdokumenterMedEldsteFørst) {
            final List<SøktPeriode<SøktPeriodeData>> perioder = kravdokumenterMedPeriode.get(kravdokument);
            final var tidslinje = new LocalDateTimeline<>(perioder.stream()
                    .map(it -> new LocalDateSegment<>(it.getPeriode().getFomDato(), it.getPeriode().getTomDato(), kravdokument))
                    .collect(Collectors.toList()));
            eldsteKravTidslinje = eldsteKravTidslinje.union(tidslinje, StandardCombinators::coalesceLeftHandSide);
        }
        return eldsteKravTidslinje;
    }
    
    private VurderSøknadsfristTjeneste<VurdertSøktPeriode.SøktPeriodeData> finnVurderSøknadsfristTjeneste(BehandlingReferanse ref) {
        final FagsakYtelseType ytelseType = ref.getFagsakYtelseType();
        
        @SuppressWarnings("unchecked")
        final var tjeneste = (VurderSøknadsfristTjeneste<VurdertSøktPeriode.SøktPeriodeData>) FagsakYtelseTypeRef.Lookup.find(søknadsfristTjenester, ytelseType).orElse(null);
        return tjeneste;
    }
}
