package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest.ÅrskvantumRestKlient;

@ApplicationScoped
@ProsessTask(ÅrskvantumDeaktiveringTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class ÅrskvantumDeaktiveringTask extends BehandlingProsessTask {
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
    protected void prosesser(ProsessTaskData prosessTaskData) {
        Behandling behandling = repositoryProvider.getBehandlingRepository().hentBehandling(prosessTaskData.getBehandlingId());

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
