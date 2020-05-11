package no.nav.k9.sak.behandling.prosessering.batch;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.prosessering.task.GjenopptaBehandlingTask;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingKandidaterRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.TaskStatus;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
public class AutomatiskGjenopptagelseTjeneste {


    private ProsessTaskRepository prosessTaskRepository;
    private BehandlingKandidaterRepository behandlingKandidaterRepository;

    @Inject
    public AutomatiskGjenopptagelseTjeneste(BehandlingKandidaterRepository behandlingKandidaterRepository,
                                            ProsessTaskRepository prosessTaskRepository) {
        this.behandlingKandidaterRepository = behandlingKandidaterRepository;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    public AutomatiskGjenopptagelseTjeneste() {
        // for CDI
    }

    public void gjenopptaBehandlinger() {
        List<Behandling> behandlingListe = behandlingKandidaterRepository.finnBehandlingerForAutomatiskGjenopptagelse();

        for (Behandling behandling : behandlingListe) {
            opprettProsessTask(behandling);
        }
    }

    private void opprettProsessTask(Behandling behandling) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(GjenopptaBehandlingTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");
        prosessTaskData.setPrioritet(100);

        prosessTaskData.setCallId(MDCOperations.generateCallId()); // ny callId per task. 

        prosessTaskRepository.lagre(prosessTaskData);
    }

    public List<TaskStatus> hentStatusForGjenopptaBehandlingGruppe(String gruppe) {

        return prosessTaskRepository.finnStatusForTaskIGruppe(GjenopptaBehandlingTask.TASKTYPE, gruppe);
    }

    public void gjenopplivBehandlinger() {
        List<Behandling> sovende = behandlingKandidaterRepository.finnÅpneBehandlingerUtenÅpneAksjonspunktEllerAutopunkt();

        for (Behandling behandling : sovende) {
            opprettProsessTask(behandling);
        }
    }
}
