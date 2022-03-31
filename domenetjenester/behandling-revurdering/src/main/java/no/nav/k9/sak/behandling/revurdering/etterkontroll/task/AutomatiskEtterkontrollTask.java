package no.nav.k9.sak.behandling.revurdering.etterkontroll.task;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.k9.sak.behandlingslager.task.FagsakProsessTask;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderKonsekvensTask;

/**
 * @Dependent scope for å hente konfig ved hver kjøring.
 */
@Dependent
@ProsessTask(AutomatiskEtterkontrollTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class AutomatiskEtterkontrollTask extends FagsakProsessTask {
    public static final String TASKTYPE = "behandlingsprosess.etterkontroll";
    private static final Logger log = LoggerFactory.getLogger(AutomatiskEtterkontrollTask.class);
    private BehandlingRepository behandlingRepository;
    private ProsessTaskTjeneste taskTjeneste;

    AutomatiskEtterkontrollTask() {
        // for CDI proxy
    }

    @Inject
    public AutomatiskEtterkontrollTask(BehandlingRepositoryProvider repositoryProvider,// NOSONAR
                                       ProsessTaskTjeneste taskTjeneste) {
        super(repositoryProvider.getFagsakLåsRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.taskTjeneste = taskTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        var fagsakId = prosessTaskData.getFagsakId();
        log.info("Etterkontrollerer fagsak med fagsakId = {}", fagsakId);
        var behandlingId = prosessTaskData.getBehandlingId();

        Behandling behandlingForRevurdering = behandlingRepository.hentBehandling(behandlingId);

        BehandlingProsessTask.logContext(behandlingForRevurdering);

        List<Behandling> åpneBehandlinger = behandlingRepository.hentBehandlingerSomIkkeErAvsluttetForFagsakId(fagsakId);
        if (åpneBehandlinger.stream().map(Behandling::getType).anyMatch(BehandlingType.REVURDERING::equals)) {
            return;
        }
        if (åpneBehandlinger.stream().map(Behandling::getType).anyMatch(BehandlingType.FØRSTEGANGSSØKNAD::equals)) {
            opprettTaskForÅVurdereKonsekvens(fagsakId, behandlingForRevurdering.getBehandlendeEnhet());
            return;
        }

        // FIXME K9 skal vi ha etterkontroll på noe mer?

    }

    private void opprettTaskForÅVurdereKonsekvens(Long fagsakId, String behandlendeEnhetsId) {
        ProsessTaskData prosessTaskData = ProsessTaskData.forProsessTask(OpprettOppgaveVurderKonsekvensTask.class);
        prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_BEHANDLENDE_ENHET, behandlendeEnhetsId);
        prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_BESKRIVELSE, OpprettOppgaveVurderKonsekvensTask.STANDARD_BESKRIVELSE);
        prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_PRIORITET, OpprettOppgaveVurderKonsekvensTask.PRIORITET_NORM);
        prosessTaskData.setFagsakId(fagsakId);
        prosessTaskData.setCallIdFraEksisterende();
        taskTjeneste.lagre(prosessTaskData);
    }
}
