package no.nav.k9.sak.behandling.hendelse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.hendelse.EventHendelse;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandling.hendelse.produksjonsstyring.BehandlingProsessHendelseMapper;
import no.nav.k9.sak.behandling.hendelse.produksjonsstyring.PubliserEventTask;
import no.nav.k9.sak.behandling.hendelse.produksjonsstyring.PubliserEventTaskImpl;
import no.nav.k9.sak.behandling.hendelse.produksjonsstyring.PubliserProduksjonsstyringHendelseTask;
import no.nav.k9.sak.behandling.hendelse.produksjonsstyring.PubliserProduksjonsstyringHendelseTaskImpl;
import no.nav.k9.sak.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.k9.sak.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.k9.sak.behandlingskontroll.events.BehandlingskontrollEvent;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakEvent;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.k9.sak.kontrakt.produksjonsstyring.los.ProduksjonsstyringAksjonspunktHendelse;
import no.nav.k9.sak.kontrakt.produksjonsstyring.los.ProduksjonsstyringBehandlingAvsluttetHendelse;
import no.nav.k9.sak.kontrakt.produksjonsstyring.los.ProduksjonsstyringBehandlingOpprettetHendelse;

@ApplicationScoped
public class BehandlingskontrollEventObserver {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private ProsessTaskTjeneste prosessTaskRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private BehandlingProsessHendelseMapper behandlingProsessHendelseMapper;

    public BehandlingskontrollEventObserver() {
    }

    @Inject
    public BehandlingskontrollEventObserver(ProsessTaskTjeneste prosessTaskRepository,
                                            BehandlingRepository behandlingRepository,
                                            BehandlingVedtakRepository behandlingVedtakRepository,
                                            BehandlingProsessHendelseMapper behandlingProsessHendelseMapper) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.behandlingRepository = behandlingRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.behandlingProsessHendelseMapper = behandlingProsessHendelseMapper;
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
            ProsessTaskData prosessTaskData = opprettProsessTaskVedtattBehandlingprosess(event.getVedtak())
        }
    }

    private ProsessTaskData opprettProsessTaskVedtattBehandlingprosess(BehandlingVedtak vedtak) {
        ProsessTaskData taskData = ProsessTaskData.forProsessTask(PubliserEventTaskImpl.class);
        taskData.setCallIdFraEksisterende();
        taskData.setPrioritet(50);

        Behandling behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingId).orElseThrow();
        ProduksjonsstyringAksjonspunktHendelse dto = new ProduksjonsstyringAksjonspunktHendelse(
            behandling.getUuid(),
            LocalDateTime.now(),
            behandlingProsessHendelseMapper.lagAksjonspunkttilstander(hendelse.getAksjonspunkter())
        );

        taskData.setPayload(JsonObjectMapper.getJson(dto));
        taskData.setProperty(PubliserProduksjonsstyringHendelseTask.PROPERTY_KEY, behandlingId.toString());
        taskData.setProperty(PubliserProduksjonsstyringHendelseTask.BESKRIVELSE, String.valueOf(dto.aksjonspunktTilstander));
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
            behandlingProsessHendelseMapper.lagAksjonspunkttilstander(hendelse.getAksjonspunkter())
        );

        taskData.setPayload(JsonObjectMapper.getJson(dto));
        taskData.setProperty(PubliserProduksjonsstyringHendelseTask.PROPERTY_KEY, behandlingId.toString());
        taskData.setProperty(PubliserProduksjonsstyringHendelseTask.BESKRIVELSE, String.valueOf(dto.aksjonspunktTilstander));
        return taskData;
    }
}
