package no.nav.k9.sak.domene.vedtak.intern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.FagsakProsessTask;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(AvsluttBehandlingTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class AvsluttBehandlingTask extends FagsakProsessTask {

    public static final String TASKTYPE = "iverksetteVedtak.avsluttBehandling";
    private static final Logger log = LoggerFactory.getLogger(AvsluttBehandlingTask.class);
    private AvsluttBehandling tjeneste;

    AvsluttBehandlingTask() {
        // for CDI proxy
    }

    @Inject
    public AvsluttBehandlingTask(AvsluttBehandling tjeneste, BehandlingRepositoryProvider repositoryProvider) {
        super(repositoryProvider.getFagsakLåsRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.tjeneste = tjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        var behandlingId = prosessTaskData.getBehandlingId();
        tjeneste.avsluttBehandling(behandlingId);
        log.info("Utført for behandling: {}", behandlingId);
    }
}
