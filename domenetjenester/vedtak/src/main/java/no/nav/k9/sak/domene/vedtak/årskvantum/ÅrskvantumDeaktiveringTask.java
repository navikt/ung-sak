package no.nav.k9.sak.domene.vedtak.årskvantum;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.PubliserInfotrygdFeedElementTask;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
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

    private Instance<ÅrskvantumDeaktiveringTjeneste> årskvantumDeaktiveringTjeneste;
    BehandlingRepositoryProvider repositoryProvider;

    public ÅrskvantumDeaktiveringTask() {
        // CDI
    }

    @Inject
    public ÅrskvantumDeaktiveringTask(@Any Instance<ÅrskvantumDeaktiveringTjeneste> årskvantumDeaktiveringTjeneste,
                                      BehandlingRepositoryProvider repositoryProvider) {
        this.årskvantumDeaktiveringTjeneste = årskvantumDeaktiveringTjeneste;
        this.repositoryProvider = repositoryProvider;
    }

    @Override
    public void doTask(ProsessTaskData pd) {

        Behandling behandling = repositoryProvider.getBehandlingRepository().hentBehandling(pd.getBehandlingId());

        logger.info("Setter uttak til inaktivt. behandlingUUID: '{}'", behandling.getUuid());

        this.hentDeaktiveringstjeneste(behandling).deaktiverUttakForBehandling(behandling.getUuid());
    }

    private ÅrskvantumDeaktiveringTjeneste hentDeaktiveringstjeneste(Behandling behandling) {
        FagsakYtelseType ytelseType = behandling.getFagsak().getYtelseType();

        return FagsakYtelseTypeRef.Lookup.find(årskvantumDeaktiveringTjeneste, ytelseType)
            .orElseThrow(() -> new IllegalArgumentException("kan ikke deaktivere uttak(årskvantum) for ytelse: " + ytelseType));
    }

}
