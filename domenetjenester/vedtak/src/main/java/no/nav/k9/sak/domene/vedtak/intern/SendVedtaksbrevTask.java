package no.nav.k9.sak.domene.vedtak.intern;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(SendVedtaksbrevTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class SendVedtaksbrevTask extends BehandlingProsessTask {

    public static final String TASKTYPE = "iverksetteVedtak.sendVedtaksbrev";

    private static final Logger log = LoggerFactory.getLogger(SendVedtaksbrevTask.class);

    private Instance<SendVedtaksbrev> sendVedtaksbrevInstanser;

    private BehandlingRepository behandlingRepository;

    SendVedtaksbrevTask() {
        // for CDI proxy
    }

    @Inject
    public SendVedtaksbrevTask(@Any Instance<SendVedtaksbrev> sendVedtaksbrevInstanser, BehandlingRepositoryProvider repositoryProvider) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.sendVedtaksbrevInstanser = sendVedtaksbrevInstanser;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        var behandlingId = prosessTaskData.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        logContext(behandling);

        SendVedtaksbrev tjeneste = FagsakYtelseTypeRef.Lookup.find(SendVedtaksbrev.class, sendVedtaksbrevInstanser, behandling.getFagsakYtelseType())
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + SendVedtaksbrev.class.getSimpleName() + " for " + behandlingId));

        tjeneste.sendVedtaksbrev(behandlingId);
        log.info("Utført for behandling: {}", behandlingId);
    }
}
