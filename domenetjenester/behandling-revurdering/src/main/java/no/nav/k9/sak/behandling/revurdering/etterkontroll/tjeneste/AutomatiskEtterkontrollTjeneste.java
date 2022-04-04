package no.nav.k9.sak.behandling.revurdering.etterkontroll.tjeneste;

import java.time.Period;
import java.util.List;

import org.slf4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.log.mdc.MDCOperations;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.task.AutomatiskEtterkontrollTask;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;

@ApplicationScoped
public class AutomatiskEtterkontrollTjeneste {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AutomatiskEtterkontrollTjeneste.class);

    private ProsessTaskTjeneste prosessTaskRepository;
    private final Period etterkontrollTidTilbake = Period.parse("P0D"); // Er allerede satt 60D fram i EK-repo
    private EtterkontrollRepository etterkontrollRepository;

    AutomatiskEtterkontrollTjeneste() {
        // For CDI?
    }

    @Inject
    public AutomatiskEtterkontrollTjeneste(ProsessTaskTjeneste prosessTaskRepository,
                                           EtterkontrollRepository etterkontrollRepository) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.etterkontrollRepository = etterkontrollRepository;
    }

    public void etterkontrollerBehandlinger() {
        List<Behandling> kontrollKandidater = etterkontrollRepository.finnKandidaterForAutomatiskEtterkontroll(etterkontrollTidTilbake);

        String callId = MDCOperations.getCallId();
        callId = (callId == null ? MDCOperations.generateCallId() : callId) + "_";

        for (Behandling kandidat : kontrollKandidater) {
            String nyCallId = callId + kandidat.getId();
            log.info("{} oppretter task med ny callId: {} ", getClass().getSimpleName(), nyCallId);
            opprettEtterkontrollTask(kandidat, nyCallId);
        }
    }

    private void opprettEtterkontrollTask(Behandling kandidat, String callId) {
        ProsessTaskData prosessTaskData = ProsessTaskData.forProsessTask(AutomatiskEtterkontrollTask.class);
        prosessTaskData.setBehandling(kandidat.getFagsakId(), kandidat.getId(), kandidat.getAktørId().getId());
        prosessTaskData.setSekvens("1");
        prosessTaskData.setPrioritet(100);

        // unik per task da det er ulike tasks for hver behandling
        prosessTaskData.setCallId(callId);

        prosessTaskRepository.lagre(prosessTaskData);
    }
}
