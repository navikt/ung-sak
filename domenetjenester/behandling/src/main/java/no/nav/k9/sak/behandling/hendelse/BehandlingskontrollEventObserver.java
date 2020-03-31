package no.nav.k9.sak.behandling.hendelse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.PubliserEventTask;
import no.nav.k9.sak.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.k9.sak.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.k9.sak.behandlingskontroll.events.BehandlingskontrollEvent;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

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
            ProsessTaskData prosessTaskData = opprettProsessTask(event.getBehandlingId(), EventHendelse.BEHANDLINGSKONTROLL_EVENT);
            prosessTaskRepository.lagre(prosessTaskData);
        } catch (Exception ex) {
            log.warn("Publisering av StoppetEvent feilet", ex);
        }
    }

    // Lytter på AksjonspunkterFunnetEvent, filtrer ut når behandling er satt manuelt på vent og legger melding på kafka
    public void observerAksjonspunkterFunnetEvent(@Observes AksjonspunktStatusEvent event) {
        if (event.getAksjonspunkter().stream().anyMatch(e -> e.erOpprettet() && AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT.equals(e.getAksjonspunktDefinisjon()))) {
            try {
                ProsessTaskData prosessTaskData = opprettProsessTask(event.getBehandlingId(), EventHendelse.AKSJONSPUNKT_OPPRETTET);
                prosessTaskRepository.lagre(prosessTaskData);
            } catch (Exception ex) {
                log.warn("Publisering av AksjonspunkterFunnetEvent feilet", ex);
            }
        }
    }

    public void observerBehandlingAvsluttetEvent(@Observes BehandlingStatusEvent.BehandlingAvsluttetEvent event) {
        try {
            ProsessTaskData prosessTaskData = opprettProsessTask(event.getBehandlingId(), EventHendelse.AKSJONSPUNKT_AVBRUTT);
            prosessTaskRepository.lagre(prosessTaskData);
        } catch (Exception ex) {
            log.warn("Publisering av BehandlingAvsluttetEvent feilet", ex);
        }
    }

    public void observerAksjonspunktHarEndretBehandlendeEnhetEvent(@Observes BehandlingEnhetEvent event) {
        try {
            ProsessTaskData prosessTaskData = opprettProsessTask(event.getBehandlingId(), EventHendelse.AKSJONSPUNKT_HAR_ENDRET_BEHANDLENDE_ENHET);
            prosessTaskRepository.lagre(prosessTaskData);
        } catch (Exception ex) {
            log.warn("Publisering av AksjonspunktHarEndretBehandlendeEnhetEvent feilet", ex);
        }
    }

    private ProsessTaskData opprettProsessTask(Long behandlingId, EventHendelse eventHendelse) throws IOException {
        ProsessTaskData taskData = new ProsessTaskData(PubliserEventTask.TASKTYPE);
        taskData.setCallIdFraEksisterende();
        taskData.setPrioritet(50);

        Optional<Behandling> behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingId);

        BehandlingProsessEventDto behandlingProsessEventDto = getProduksjonstyringEventDto(eventHendelse, behandling.get());

        String json = getJson(behandlingProsessEventDto);
        taskData.setProperty(PubliserEventTask.PROPERTY_EVENT, json);
        taskData.setProperty(PubliserEventTask.PROPERTY_KEY, behandlingId.toString());
        return taskData;
    }

    private String getJson(BehandlingProsessEventDto produksjonstyringEventDto) throws IOException {
        return JsonObjectMapper.getJson(produksjonstyringEventDto);
    }

    private BehandlingProsessEventDto getProduksjonstyringEventDto(EventHendelse eventHendelse, Behandling behandling) {
        Map<String, String> aksjonspunktKoderMedStatusListe = new HashMap<>();

        behandling.getAksjonspunkter().forEach(aksjonspunkt -> aksjonspunktKoderMedStatusListe.put(aksjonspunkt.getAksjonspunktDefinisjon().getKode(), aksjonspunkt.getStatus().getKode()));
        return BehandlingProsessEventDto.builder()
            .medEksternId(behandling.getUuid())
            .medEventTid(LocalDateTime.now())
            .medFagsystem(Fagsystem.K9SAK)
            .medBehandlingId(behandling.getId())
            .medSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi())
            .medAktørId(behandling.getAktørId().getId())
            .getBehandlingstidFrist(behandling.getBehandlingstidFrist())
            .medEventHendelse(eventHendelse)
            .medBehandlinStatus(behandling.getStatus().getKode())
            .medBehandlingSteg(behandling.getAktivtBehandlingSteg() == null ? null : behandling.getAktivtBehandlingSteg().getKode())
            .medYtelseTypeKode(behandling.getFagsakYtelseType().getKode())
            .medBehandlingTypeKode(behandling.getType().getKode())
            .medOpprettetBehandling(behandling.getOpprettetDato())
            .medAksjonspunktKoderMedStatusListe(aksjonspunktKoderMedStatusListe)
            .build();
    }
}
