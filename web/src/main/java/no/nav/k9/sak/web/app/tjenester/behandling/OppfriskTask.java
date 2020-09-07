package no.nav.k9.sak.web.app.tjenester.behandling;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(OppfriskTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class OppfriskTask extends BehandlingProsessTask {
    
    private static final Logger log = LoggerFactory.getLogger(OppfriskTask.class);

    public static final String TASKTYPE = "behandling.oppfrisk";

    private BehandlingRepository repository;
    private SjekkProsessering sjekkProsessering;

    OppfriskTask() {
        // for CDI proxy
    }

    @Inject
    public OppfriskTask(BehandlingLåsRepository behandlingLåsRepository, BehandlingRepository repository, SjekkProsessering sjekkProsessering) {
        super(behandlingLåsRepository);
        this.repository = repository;
        this.sjekkProsessering = sjekkProsessering;
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData) {
        try {
            var behandling = repository.hentBehandling(prosessTaskData.getBehandlingId());
            sjekkProsessering.asynkInnhentingAvRegisteropplysningerOgKjørProsess(behandling);
        } catch (RuntimeException e) {
            log.info("Uventet feil ved oppfrisking av behandling.", e);
        }
    }
    
    
    public static final ProsessTaskData create(Behandling behandling) {
        final ProsessTaskData taskData = new ProsessTaskData(OppfriskTask.TASKTYPE);
        taskData.setCallIdFraEksisterende();
        taskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        return taskData;
    }
}
