package no.nav.k9.sak.behandling.hendelse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.hendelse.EventHendelse;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskRepository;
import no.nav.k9.sak.behandling.hendelse.produksjonsstyring.PubliserEventTask;
import no.nav.k9.sak.behandling.hendelse.produksjonsstyring.PubliserProduksjonsstyringHendelseTask;
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
import no.nav.k9.sak.kontrakt.produksjonsstyring.los.ProduksjonsstyringBehandlingOpprettetHendelse;

@ApplicationScoped
public class BehandlingskontrollEventObserver {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private ProsessTaskRepository prosessTaskRepository;
    private BehandlingRepository behandlingRepository;

    public BehandlingskontrollEventObserver() {
    }

    @Inject
    public BehandlingskontrollEventObserver(ProsessTaskRepository prosessTaskRepository, BehandlingRepository behandlingRepository) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.behandlingRepository = behandlingRepository;
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

    public void observerBehandlingAvsluttetEvent(@Observes BehandlingStatusEvent.BehandlingAvsluttetEvent event) {
        try {
            ProsessTaskData prosessTaskData = opprettProsessTaskBehandlingprosess(event.getBehandlingId(), EventHendelse.AKSJONSPUNKT_AVBRUTT);
            prosessTaskRepository.lagre(prosessTaskData);
        } catch (Exception ex) {
            log.warn("Publisering av BehandlingAvsluttetEvent feilet", ex);
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
        ProsessTaskData taskData = new ProsessTaskData(PubliserEventTask.TASKTYPE);
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
        ProsessTaskData taskData = new ProsessTaskData(PubliserProduksjonsstyringHendelseTask.TASKTYPE);
        taskData.setCallIdFraEksisterende();
        taskData.setPrioritet(50);

        Behandling behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingId).get();
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


    private ProsessTaskData opprettProsessTaskAksjonspunkthendelse(Long behandlingId, AksjonspunktStatusEvent hendelse) throws IOException {
        ProsessTaskData taskData = new ProsessTaskData(PubliserProduksjonsstyringHendelseTask.TASKTYPE);
        taskData.setCallIdFraEksisterende();
        taskData.setPrioritet(50);

        Behandling behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingId).get();
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
            .build();
    }
}
