package no.nav.ung.sak.domene.behandling.steg.parallellebehandlinger;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.ParallelleBehandlingerRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.task.FagsakProsessTask;

/**
 * Oppdaterer åpne ytelsesbehandlinger på fagsaken med vedtatte data fra siste avsluttede behandling, og tar dem av vent.
 * <p>
 * Tasken er tilstandsløs: kilden er alltid siste avsluttede, ikke-henlagte ytelsesbehandling når tasken kjører. Den
 * opprettes når en behandling avsluttes, og av ventepunktet for vedtaksprioritet når en behandling ikke bygger på siste
 * vedtak.
 */
@ApplicationScoped
@ProsessTask(SynkroniserÅpneBehandlingerTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class SynkroniserÅpneBehandlingerTask extends FagsakProsessTask {

    public static final String TASKTYPE = "behandlingskontroll.synkroniserApneBehandlinger";

    private ParallelleBehandlingerRepository parallelleBehandlingerRepository;
    private SynkroniserÅpneBehandlingerTjeneste synkroniserÅpneBehandlingerTjeneste;

    SynkroniserÅpneBehandlingerTask() {
        // for CDI proxy
    }

    @Inject
    public SynkroniserÅpneBehandlingerTask(FagsakLåsRepository fagsakLåsRepository,
                                           BehandlingLåsRepository behandlingLåsRepository,
                                           ParallelleBehandlingerRepository parallelleBehandlingerRepository,
                                           SynkroniserÅpneBehandlingerTjeneste synkroniserÅpneBehandlingerTjeneste) {
        super(fagsakLåsRepository, behandlingLåsRepository);
        this.parallelleBehandlingerRepository = parallelleBehandlingerRepository;
        this.synkroniserÅpneBehandlingerTjeneste = synkroniserÅpneBehandlingerTjeneste;
    }

    /**
     * Låser alle åpne ytelsesbehandlinger før fagsaken, i samme rekkefølge som andre tasker.
     */
    @Override
    protected List<String> identifiserBehandling(ProsessTaskData prosessTaskData) {
        return parallelleBehandlingerRepository.hentIkkeAvsluttedeYtelsesbehandlinger(prosessTaskData.getFagsakId()).stream()
            .map(b -> b.behandlingId().toString())
            .toList();
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        synkroniserÅpneBehandlingerTjeneste.synkroniser(prosessTaskData);
    }
}
