package no.nav.k9.sak.behandling.prosessering.batch;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.k9.sak.behandling.prosessering.task.GjenopptaBehandlingTask;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingKandidaterRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.TaskStatus;

@ApplicationScoped
public class AutomatiskGjenopptagelseTjeneste {

    private ProsessTaskRepository prosessTaskRepository;
    private BehandlingKandidaterRepository behandlingKandidaterRepository;
    private BehandlingProsesseringTjeneste prosesseringTjeneste;

    @Inject
    public AutomatiskGjenopptagelseTjeneste(BehandlingKandidaterRepository behandlingKandidaterRepository,
                                            BehandlingProsesseringTjeneste prosesseringTjeneste,
                                            ProsessTaskRepository prosessTaskRepository) {
        this.behandlingKandidaterRepository = behandlingKandidaterRepository;
        this.prosesseringTjeneste = prosesseringTjeneste;
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
        prosesseringTjeneste.opprettTasksForGjenopptaOppdaterFortsett(behandling);
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
