package no.nav.k9.sak.domene.behandling.steg.iverksettevedtak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.k9.sak.domene.iverksett.OpprettProsessTaskIverksett;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(HoldIgjenIverksettelseTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class HoldIgjenIverksettelseTask extends BehandlingProsessTask {

    public static final Logger logger = LoggerFactory.getLogger(HoldIgjenIverksettelseTask.class);
    public static final String TASKTYPE = "iverksetteVedtak.holdIgjenIverksettelse";

    private  BehandlingRepository behandlingRepository;
    private OpprettProsessTaskIverksett opprettProsessTaskIverksett;



    HoldIgjenIverksettelseTask() {
        // for CDI proxy
    }

    @Inject
    public HoldIgjenIverksettelseTask(BehandlingRepositoryProvider repositoryProvider,
                                      @FagsakYtelseTypeRef OpprettProsessTaskIverksett opprettProsessTaskIverksett) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.opprettProsessTaskIverksett = opprettProsessTaskIverksett;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        // Denne tasken starter opp etter en forsinkelse. Eneste hensikt er å forsinke iverksettingstasker.
        var behandlingId = Long.valueOf(prosessTaskData.getBehandlingId());
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        opprettProsessTaskIverksett.opprettIverksettingstasker(behandling);
    }
}
