package no.nav.foreldrepenger.behandling.revurdering.etterkontroll.task;

import java.util.List;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.FagsakProsessTask;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderKonsekvensTask;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

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
    private ProsessTaskRepository prosessTaskRepository;

    AutomatiskEtterkontrollTask() {
        // for CDI proxy
    }

    @Inject
    public AutomatiskEtterkontrollTask(BehandlingRepositoryProvider repositoryProvider,// NOSONAR
                                       ProsessTaskRepository prosessTaskRepository) {
        super(repositoryProvider.getFagsakLåsRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.prosessTaskRepository = prosessTaskRepository;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        var fagsakId = prosessTaskData.getFagsakId();
        log.info("Etterkontrollerer fagsak med fagsakId = {}", fagsakId);
        var behandlingId = prosessTaskData.getBehandlingId();

        Behandling behandlingForRevurdering = behandlingRepository.hentBehandling(behandlingId);

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
        ProsessTaskData prosessTaskData = new ProsessTaskData(OpprettOppgaveVurderKonsekvensTask.TASKTYPE);
        prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_BEHANDLENDE_ENHET, behandlendeEnhetsId);
        prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_BESKRIVELSE, OpprettOppgaveVurderKonsekvensTask.STANDARD_BESKRIVELSE);
        prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_PRIORITET, OpprettOppgaveVurderKonsekvensTask.PRIORITET_NORM);
        prosessTaskData.setFagsakId(fagsakId);
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }
}
