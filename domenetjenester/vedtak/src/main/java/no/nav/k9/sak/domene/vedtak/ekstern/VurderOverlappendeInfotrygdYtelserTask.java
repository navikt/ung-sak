package no.nav.k9.sak.domene.vedtak.ekstern;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;

@ApplicationScoped
@ProsessTask(VurderOverlappendeInfotrygdYtelserTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VurderOverlappendeInfotrygdYtelserTask extends BehandlingProsessTask {

    public static final String TASKTYPE = "iverksetteVedtak.vkyOppgaveInfotrygdYtelser";
    private VurderOverlappendeInfotrygdYtelser vurderOverlappendeInfotrygdYtelser;

    private BehandlingRepository behandlingRepository;
    private Boolean lansert;

    VurderOverlappendeInfotrygdYtelserTask() {
        // for CDI proxy
    }

    @Inject
    public VurderOverlappendeInfotrygdYtelserTask(BehandlingRepositoryProvider repositoryProvider,
                                                  VurderOverlappendeInfotrygdYtelser vurderOverlappendeInfotrygdYtelser,
                                                  @KonfigVerdi(value = "VKY_VED_OVERLAPP_INFOTRYGD", defaultVerdi = "true") Boolean lansert) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vurderOverlappendeInfotrygdYtelser = vurderOverlappendeInfotrygdYtelser;
        this.lansert = lansert;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        if (!lansert) {
            return;
        }
        Long behandlingId = Long.valueOf(prosessTaskData.getBehandlingId());
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        logContext(behandling);

        vurderOverlappendeInfotrygdYtelser.vurder(behandling);
    }
}
