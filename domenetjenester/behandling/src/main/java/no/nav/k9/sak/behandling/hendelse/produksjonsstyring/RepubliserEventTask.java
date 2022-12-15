package no.nav.k9.sak.behandling.hendelse.produksjonsstyring;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.hendelse.EventHendelse;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.k9.sak.kontrakt.behandling.BehandlingProsessHendelse;

/**
 * Republiserer event for stoppet prosess.
 */
@ApplicationScoped
@ProsessTask(RepubliserEventTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class RepubliserEventTask implements ProsessTaskHandler {
    private static final Logger log = LoggerFactory.getLogger(RepubliserEventTask.class);    
    public static final String TASKTYPE = "oppgavebehandling.RepubliserEvent";

    private BehandlingRepository behandlingRepository;
    private BehandlingProsessHendelseMapper behandlingProsessHendelseMapper;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    RepubliserEventTask() {
        // for CDI proxy
    }

    @Inject
    public RepubliserEventTask(BehandlingRepository behandlingRepository,
            BehandlingProsessHendelseMapper behandlingProsessHendelseMapper,
            ProsessTaskTjeneste prosessTaskTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingProsessHendelseMapper = behandlingProsessHendelseMapper;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        final UUID behandlingUuid = prosessTaskData.getBehandlingUuid();
        if (behandlingUuid == null) {
            log.error("behandlingUuid er påkrevd. Kjøring avbrutt.");
            return;
        }
        
        final Optional<Behandling> behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingUuid);
        if (behandling.isEmpty()) {
            log.error("Kunne ikke finne behandling. Kjøring avbrutt.");
            return;
        }
        if (behandling.get().getÅpneAksjonspunkter().isEmpty()) {
            return;
        }

        final var dto = behandlingProsessHendelseMapper.getProduksjonstyringEventDto(EventHendelse.BEHANDLINGSKONTROLL_EVENT, behandling.get());
        final var aksjonspunkter = dto.getAksjonspunktKoderMedStatusListe();

        final ProsessTaskData nyProsessTask = ProsessTaskData.forProsessTask(PubliserEventTaskImpl.class);
        nyProsessTask.setCallIdFraEksisterende();
        nyProsessTask.setPrioritet(50);
        nyProsessTask.setPayload(toJson(dto));
        
        nyProsessTask.setProperty(PubliserEventTask.PROPERTY_KEY, behandling.get().getId().toString());
        nyProsessTask.setProperty(PubliserEventTask.BESKRIVELSE, String.valueOf(aksjonspunkter));

        prosessTaskTjeneste.lagre(nyProsessTask);
    }

    private String toJson(final BehandlingProsessHendelse dto) {
        final String json;
        try {
            json = JsonObjectMapper.getJson(dto);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return json;
    }
}
