package no.nav.ung.sak.behandling.hendelse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.hendelse.EventHendelse;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.sak.behandling.hendelse.produksjonsstyring.BehandlingProsessHendelseMapper;
import no.nav.ung.sak.behandling.hendelse.produksjonsstyring.LosTaskSekvensGenerator;
import no.nav.ung.sak.behandling.hendelse.produksjonsstyring.PubliserEventTask;
import no.nav.ung.sak.behandling.hendelse.produksjonsstyring.PubliserEventTaskImpl;
import no.nav.ung.sak.behandling.hendelse.produksjonsstyring.PubliserProduksjonsstyringHendelseTask;
import no.nav.ung.sak.behandling.hendelse.produksjonsstyring.PubliserProduksjonsstyringHendelseTaskImpl;
import no.nav.ung.sak.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingskontrollEvent;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakEvent;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.ung.sak.domene.typer.tid.JsonObjectMapperKodeverdiSomStringSerializer;
import no.nav.ung.sak.kontrakt.produksjonsstyring.los.ProduksjonsstyringAksjonspunktHendelse;
import no.nav.ung.sak.kontrakt.produksjonsstyring.los.ProduksjonsstyringBehandlingAvsluttetHendelse;
import no.nav.ung.sak.kontrakt.produksjonsstyring.los.ProduksjonsstyringBehandlingOpprettetHendelse;

@ApplicationScoped
public class BehandlingskontrollEventObserver {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private ProsessTaskTjeneste prosessTaskRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingProsessHendelseMapper behandlingProsessHendelseMapper;
    private boolean kodeverkSomStringTopics;

    public BehandlingskontrollEventObserver() {
    }

    @Inject
    public BehandlingskontrollEventObserver(ProsessTaskTjeneste prosessTaskRepository,
                                            BehandlingRepository behandlingRepository,
                                            BehandlingProsessHendelseMapper behandlingProsessHendelseMapper,
                                            @KonfigVerdi(value = "KODEVERK_SOM_STRING_TOPICS", defaultVerdi = "false") boolean kodeverkSomStringTopics
    ) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.behandlingRepository = behandlingRepository;
        this.behandlingProsessHendelseMapper = behandlingProsessHendelseMapper;
        this.kodeverkSomStringTopics = kodeverkSomStringTopics;
    }

    private String dtoTilJson(Object dto) throws IOException {
        if (kodeverkSomStringTopics) {
            return JsonObjectMapperKodeverdiSomStringSerializer.getJson(dto);
        } else {
            return JsonObjectMapper.getJson(dto);
        }
    }

    public void observerStoppetEvent(@Observes BehandlingskontrollEvent.StoppetEvent event) {
        try {
            ProsessTaskData prosessTaskData = opprettProsessTaskBehandlingprosess(event.getBehandlingId(), EventHendelse.BEHANDLINGSKONTROLL_EVENT);
            prosessTaskRepository.lagre(prosessTaskData);
        } catch (Exception ex) {
            throw new RuntimeException("Publisering av StoppetEvent feilet", ex);
        }
    }
    // Lytter på AksjonspunkterFunnetEvent, filtrer ut når behandling er satt manuelt på vent og legger melding på kafka

    public void observerAksjonspunkterFunnetEvent(@Observes AksjonspunktStatusEvent event) {
        if (event.getAksjonspunkter().stream().anyMatch(e -> e.erOpprettet() && AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT.equals(e.getAksjonspunktDefinisjon()))) {
            try {
                ProsessTaskData prosessTaskData = opprettProsessTaskBehandlingprosess(event.getBehandlingId(), EventHendelse.AKSJONSPUNKT_OPPRETTET);
                prosessTaskRepository.lagre(prosessTaskData);
            } catch (IOException ex) {
                throw new RuntimeException("Publisering av AksjonspunkterFunnetEvent feilet", ex);
            }
        }

        try {
            ProsessTaskData prosessTaskData = opprettProsessTaskAksjonspunkthendelse(event.getBehandlingId(), event);
            prosessTaskRepository.lagre(prosessTaskData);
        } catch (IOException ex) {
            throw new RuntimeException("Publisering av Aksjonspunkthendelse feilet", ex);
        }
    }

    public void observerBehandlingOpprettetEvent(@Observes BehandlingStatusEvent.BehandlingOpprettetEvent event) {
        try {
            ProsessTaskData prosessTaskData = opprettProsessTaskBehandlingOpprettetEvent(event.getBehandlingId());
            prosessTaskRepository.lagre(prosessTaskData);
        } catch (IOException ex) {
            throw new RuntimeException("Publisering av BehandlingOpprettetHendelse feilet", ex);
        }
    }


    public void observerBehandlingAvsluttetEvent(@Observes BehandlingStatusEvent.BehandlingAvsluttetEvent event) {
        try {
            ProsessTaskData prosessTaskData = opprettProsessTaskBehandlingprosess(event.getBehandlingId(), EventHendelse.AKSJONSPUNKT_AVBRUTT);
            prosessTaskRepository.lagre(prosessTaskData);
        } catch (IOException ex) {
            throw new RuntimeException("Publisering av BehandlingAvsluttetEvent feilet", ex);
        }

        try {
            ProsessTaskData prosessTaskData = opprettProsessTaskBehandlingAvsluttetEvent(event.getBehandlingId());
            prosessTaskRepository.lagre(prosessTaskData);
        } catch (IOException ex) {
            throw new RuntimeException("Publisering av BehandlingAvsluttetHendelse for produksjonsstyring feilet", ex);
        }
    }

    public void observerVedtakFattetEvent(@Observes BehandlingVedtakEvent event) {
        try {
            ProsessTaskData prosessTaskData = opprettProsessTaskVedtattBehandlingprosess(event.getBehandlingId(), event.getVedtak());
            prosessTaskRepository.lagre(prosessTaskData);
        } catch (IOException ex) {
            throw new RuntimeException("Publisering av BehandlingskontrollEvent feilet", ex);
        }
    }

    private ProsessTaskData opprettProsessTaskVedtattBehandlingprosess(Long behandlingId, BehandlingVedtak vedtak) throws IOException {
        ProsessTaskData taskData = ProsessTaskData.forProsessTask(PubliserEventTaskImpl.class);
        taskData.setCallIdFraEksisterende();
        taskData.setPrioritet(50);

        Optional<Behandling> behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingId);

        var dto = behandlingProsessHendelseMapper.getProduksjonstyringEventDto(LocalDateTime.now(), EventHendelse.BEHANDLINGSKONTROLL_EVENT, behandling.get(), vedtak.getVedtaksdato());
        taskData.setPayload(dtoTilJson(dto));
        taskData.setProperty(PubliserEventTask.PROPERTY_KEY, behandlingId.toString());
        taskData.setGruppe(LosTaskSekvensGenerator.gruppeForBehandling(behandlingId));
        taskData.setSekvens(LosTaskSekvensGenerator.nesteSekvens());
        return taskData;
    }

    public void observerAksjonspunktHarEndretBehandlendeEnhetEvent(@Observes BehandlingEnhetEvent event) {
        try {
            ProsessTaskData prosessTaskData = opprettProsessTaskBehandlingprosess(event.getBehandlingId(), EventHendelse.AKSJONSPUNKT_HAR_ENDRET_BEHANDLENDE_ENHET);
            prosessTaskRepository.lagre(prosessTaskData);
        } catch (IOException ex) {
            throw new RuntimeException("Publisering av AksjonspunktHarEndretBehandlendeEnhetEvent feilet", ex);
        }
    }

    private ProsessTaskData opprettProsessTaskBehandlingprosess(Long behandlingId, EventHendelse eventHendelse) throws IOException {
        ProsessTaskData taskData = ProsessTaskData.forProsessTask(PubliserEventTaskImpl.class);
        taskData.setCallIdFraEksisterende();
        taskData.setPrioritet(50);

        Optional<Behandling> behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingId);

        var dto = behandlingProsessHendelseMapper.getProduksjonstyringEventDto(eventHendelse, behandling.get());
        taskData.setPayload(dtoTilJson(dto));
        taskData.setProperty(PubliserEventTask.PROPERTY_KEY, behandlingId.toString());
        taskData.setGruppe(LosTaskSekvensGenerator.gruppeForBehandling(behandlingId));
        taskData.setSekvens(LosTaskSekvensGenerator.nesteSekvens());
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
            fagsak.getAktørId()
        );

        taskData.setPayload(dtoTilJson(dto));
        taskData.setProperty(PubliserProduksjonsstyringHendelseTask.PROPERTY_KEY, behandlingId.toString());
        taskData.setGruppe(LosTaskSekvensGenerator.gruppeForBehandling(behandlingId));
        taskData.setSekvens(LosTaskSekvensGenerator.nesteSekvens());
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

        taskData.setPayload(dtoTilJson(dto));
        taskData.setProperty(PubliserProduksjonsstyringHendelseTask.PROPERTY_KEY, behandlingId.toString());
        taskData.setGruppe(LosTaskSekvensGenerator.gruppeForBehandling(behandlingId));
        taskData.setSekvens(LosTaskSekvensGenerator.nesteSekvens());
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
            behandlingProsessHendelseMapper.lagAksjonspunkttilstander(hendelse.getAksjonspunkter())
        );

        taskData.setPayload(dtoTilJson(dto));
        taskData.setProperty(PubliserProduksjonsstyringHendelseTask.PROPERTY_KEY, behandlingId.toString());
        taskData.setGruppe(LosTaskSekvensGenerator.gruppeForBehandling(behandlingId));
        taskData.setSekvens(LosTaskSekvensGenerator.nesteSekvens());
        return taskData;
    }
}
