package no.nav.foreldrepenger.økonomi;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragskontrollTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@SuppressWarnings("unused")
@ApplicationScoped
@ProsessTask(VurderOgSendØkonomiOppdragTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VurderOgSendØkonomiOppdragTask extends BehandlingProsessTask {

    public static final String TASKTYPE = "iverksetteVedtak.oppdragTilØkonomi";

    private OppdragskontrollTjeneste oppdragskontrollTjeneste;
    private ProsessTaskRepository prosessTaskRepository;

    VurderOgSendØkonomiOppdragTask() {
        // for CDI proxy
    }

    @Inject
    public VurderOgSendØkonomiOppdragTask(OppdragskontrollTjeneste oppdragskontrollTjeneste,
                                          ProsessTaskRepository prosessTaskRepository,
                                          BehandlingRepositoryProvider repositoryProvider) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.oppdragskontrollTjeneste = oppdragskontrollTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        // FIXME K9 send til økonomi
    }

}
