package no.nav.k9.sak.domene.vedtak.årskvantum;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.PubliserInfotrygdFeedElementTask;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(PubliserInfotrygdFeedElementTask.TASKTYPE)
public class ÅrskvantumDeaktiveringTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "iverksetteVedtak.deaktiverUttakForBehandling";

    private static final Logger logger = LoggerFactory.getLogger(ÅrskvantumDeaktiveringTask.class);

    ÅrskvantumDeaktiveringTjeneste årskvantumDeaktiveringTjeneste;
    BehandlingRepositoryProvider repositoryProvider;

    public ÅrskvantumDeaktiveringTask() {
        // CDI
    }

    @Inject
    public ÅrskvantumDeaktiveringTask(ÅrskvantumDeaktiveringTjeneste årskvantumDeaktiveringTjeneste,
                                      BehandlingRepositoryProvider repositoryProvider) {
        this.årskvantumDeaktiveringTjeneste = årskvantumDeaktiveringTjeneste;
        this.repositoryProvider = repositoryProvider;
    }

    @Override
    public void doTask(ProsessTaskData pd) {

        Behandling behandling = repositoryProvider.getBehandlingRepository().hentBehandling(pd.getBehandlingId());

        logger.info("Setter uttak til inaktivt. behandlingUUID: '{}'", behandling.getUuid());

        årskvantumDeaktiveringTjeneste.deaktiverUttakForBehandling(behandling.getUuid());
    }

}
