package no.nav.k9.sak.domene.vedtak.intern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.FastsettPGIPeriodeTjeneste;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;

@ApplicationScoped
@ProsessTask(FastsettPGIPerioderTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class FastsettPGIPerioderTask extends BehandlingProsessTask {

    public static final String TASKTYPE = "iverksetteVedtak.fastsettNæringsinntekt";
    private FastsettPGIPeriodeTjeneste fastsettPGIPeriodeTjeneste;


    FastsettPGIPerioderTask() {
        // for CDI proxy
    }

    @Inject
    public FastsettPGIPerioderTask(BehandlingRepositoryProvider repositoryProvider,
                                   FastsettPGIPeriodeTjeneste fastsettPGIPeriodeTjeneste) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.fastsettPGIPeriodeTjeneste = fastsettPGIPeriodeTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        var behandlingId = prosessTaskData.getBehandlingId();
        fastsettPGIPeriodeTjeneste.fastsettPGIDersomRelevant(Long.valueOf(behandlingId));
    }
}
