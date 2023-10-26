package no.nav.k9.sak.behandling.hendelse.produksjonsstyring;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.ObjectUtils;
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
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
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
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    RepubliserEventTask() {
        // for CDI proxy
    }

    @Inject
    public RepubliserEventTask(BehandlingRepository behandlingRepository,
                               BehandlingProsessHendelseMapper behandlingProsessHendelseMapper,
                               BehandlingVedtakRepository behandlingVedtakRepository,
                               ProsessTaskTjeneste prosessTaskTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingProsessHendelseMapper = behandlingProsessHendelseMapper;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
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

        final LocalDateTime eventTid = Objects.requireNonNull(ObjectUtils.firstNonNull(
                behandling.get().getEndretTidspunkt(),
                behandling.get().getOpprettetTidspunkt()
                ), "Mangler tidspunkt for endring av behandling");
        
        final LocalDate vedtaksdato = behandlingVedtakRepository.hentBehandlingVedtakFor(behandlingUuid)
                .map(BehandlingVedtak::getVedtaksdato).orElse(null);

        final var dto = behandlingProsessHendelseMapper.getProduksjonstyringEventDto(eventTid, EventHendelse.BEHANDLINGSKONTROLL_EVENT, behandling.get(), vedtaksdato);

        final ProsessTaskData nyProsessTask = ProsessTaskData.forProsessTask(PubliserEventTaskImpl.class);
        nyProsessTask.setCallIdFraEksisterende();
        nyProsessTask.setPrioritet(50);
        nyProsessTask.setPayload(toJson(dto));

        nyProsessTask.setProperty(PubliserEventTask.PROPERTY_KEY, behandling.get().getId().toString());

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
