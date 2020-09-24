package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;
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

    protected ÅrskvantumDeaktiveringTask() {
        // CDI proxy
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

        precondition(behandling);

        var deaktiver = skalDeaktivere(behandling);

        if (deaktiver) {
            logger.info("Setter uttak til inaktivt. behandlingUUID: '{}'", behandling.getUuid());
            this.årskvantumRestKlient.deaktiverUttakForBehandling(behandling.getUuid());
        }
    }

    public static boolean skalDeaktivere(Behandling behandling) {
        return BehandlingResultatType.AVSLÅTT.equals(behandling.getBehandlingResultatType())
            || BehandlingResultatType.getAlleHenleggelseskoder().contains(behandling.getBehandlingResultatType());
    }

    private void precondition(Behandling behandling) {
        BehandlingProsessTask.logContext(behandling);
        if (!FagsakYtelseType.OMSORGSPENGER.equals(behandling.getFagsakYtelseType())) {
            throw new IllegalArgumentException("Utvikler-feil: Ikke tillatt å deaktivere uttak i årskvantum for andre enn omsorgspenger");
        }
    }
}
