package no.nav.k9.sak.behandling.revurdering.etterkontroll.tjeneste;

import java.time.Period;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;

import no.nav.k9.sak.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.task.AutomatiskEtterkontrollTask;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskRepository;
import no.nav.k9.prosesstask.api.TaskStatus;
import no.nav.k9.felles.log.mdc.MDCOperations;

@ApplicationScoped
public class AutomatiskEtterkontrollTjeneste {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AutomatiskEtterkontrollTjeneste.class);

    private ProsessTaskRepository prosessTaskRepository;
    private Period etterkontrollTidTilbake = Period.parse("P0D"); // Er allerede satt 60D fram i EK-repo
    private EtterkontrollRepository etterkontrollRepository;

    AutomatiskEtterkontrollTjeneste() {
        // For CDI?
    }

    @Inject
    public AutomatiskEtterkontrollTjeneste(ProsessTaskRepository prosessTaskRepository,
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

    private String opprettEtterkontrollTask(Behandling kandidat, String callId) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(AutomatiskEtterkontrollTask.TASKTYPE);
        prosessTaskData.setBehandling(kandidat.getFagsakId(), kandidat.getId(), kandidat.getAktørId().getId());
        prosessTaskData.setSekvens("1");
        prosessTaskData.setPrioritet(100);

        // unik per task da det er ulike tasks for hver behandling
        prosessTaskData.setCallId(callId);

        prosessTaskRepository.lagre(prosessTaskData);
        //TODO(OJR) må endres i forbindelsen med at løsningen ser på task_grupper på en annet måte nå, hvis en prosess feiler i en gruppe stopper alt opp..
        return prosessTaskData.getGruppe();
    }

    public List<TaskStatus> hentStatusForEtterkontrollGruppe(String gruppe) {
        return prosessTaskRepository.finnStatusForTaskIGruppe(AutomatiskEtterkontrollTask.TASKTYPE, gruppe);
    }

}
