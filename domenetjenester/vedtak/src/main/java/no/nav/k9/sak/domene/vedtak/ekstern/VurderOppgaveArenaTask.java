package no.nav.k9.sak.domene.vedtak.ekstern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.k9.sak.typer.AktørId;

@ApplicationScoped
@ProsessTask(VurderOppgaveArenaTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VurderOppgaveArenaTask extends BehandlingProsessTask {

    public static final String TASKTYPE = "iverksetteVedtak.oppgaveArena";
    private static final Logger log = LoggerFactory.getLogger(VurderOppgaveArenaTask.class);
    private VurderOmArenaYtelseSkalOpphøre vurdereOmArenaYtelseSkalOpphøre;

    private BehandlingRepository behandlingRepository;

    VurderOppgaveArenaTask() {
        // for CDI proxy
    }

    @Inject
    public VurderOppgaveArenaTask(BehandlingRepositoryProvider repositoryProvider,
                                  VurderOmArenaYtelseSkalOpphøre vurdereOmArenaYtelseSkalOpphøre) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.vurdereOmArenaYtelseSkalOpphøre = vurdereOmArenaYtelseSkalOpphøre;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        Long behandlingId = Long.valueOf(prosessTaskData.getBehandlingId());
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        logContext(behandling);

        AktørId aktørId = new AktørId(prosessTaskData.getAktørId());
        vurdereOmArenaYtelseSkalOpphøre.opprettOppgaveHvisArenaytelseSkalOpphøre(behandlingId, aktørId, behandling.getFagsak().getPeriode().getFomDato());
        log.info("VurderOppgaveArenaTask: Vurderer for behandling: {}", behandlingId); //$NON-NLS-1$
    }
}
