package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest.ÅrskvantumRestKlient;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(ÅrskvantumDeaktiveringTask.TASKTYPE)
public class ÅrskvantumDeaktiveringTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "iverksetteVedtak.deaktiverUttakForBehandling";

    private static final Logger logger = LoggerFactory.getLogger(ÅrskvantumDeaktiveringTask.class);

    BehandlingRepositoryProvider repositoryProvider;
    ÅrskvantumRestKlient årskvantumRestKlient;

    public ÅrskvantumDeaktiveringTask() {
        // CDI
    }

    @Inject
    public ÅrskvantumDeaktiveringTask(BehandlingRepositoryProvider repositoryProvider,
                                      ÅrskvantumRestKlient årskvantumRestKlient) {
        this.repositoryProvider = repositoryProvider;
        this.årskvantumRestKlient = årskvantumRestKlient;
    }

    @Override
    public void doTask(ProsessTaskData pd) {

        Behandling behandling = repositoryProvider.getBehandlingRepository().hentBehandling(pd.getBehandlingId());

        if (FagsakYtelseType.OMP.equals(behandling.getFagsakYtelseType()) || FagsakYtelseType.OMSORGSPENGER.equals(behandling.getFagsakYtelseType())) {
            logger.info("Setter uttak til inaktivt. behandlingUUID: '{}'", behandling.getUuid());

            this.årskvantumRestKlient.deaktiverUttakForBehandling(behandling.getUuid());
        }
    }
}
