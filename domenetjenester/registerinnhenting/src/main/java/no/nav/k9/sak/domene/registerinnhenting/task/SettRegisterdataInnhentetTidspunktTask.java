package no.nav.k9.sak.domene.registerinnhenting.task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.k9.sak.domene.registerinnhenting.RegisterdataInnhenter;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(SettRegisterdataInnhentetTidspunktTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class SettRegisterdataInnhentetTidspunktTask extends BehandlingProsessTask {

    public static final String TASKTYPE = "innhentsaksopplysninger.oppdaterttidspunkt";
    private static final Logger LOGGER = LoggerFactory.getLogger(SettRegisterdataInnhentetTidspunktTask.class);
    private BehandlingRepository behandlingRepository;
    private RegisterdataInnhenter registerdataInnhenter;

    SettRegisterdataInnhentetTidspunktTask() {
        // for CDI proxy
    }

    @Inject
    public SettRegisterdataInnhentetTidspunktTask(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                                  RegisterdataInnhenter registerdataInnhenter) {
        super(behandlingRepositoryProvider.getBehandlingLåsRepository());
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.registerdataInnhenter = registerdataInnhenter;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        Behandling behandling = behandlingRepository.hentBehandling(prosessTaskData.getBehandlingId());
        LOGGER.info("Oppdaterer registerdata innhentet tidspunkt behandling med id={} og uuid={}", behandling.getId(), behandling.getUuid());
        registerdataInnhenter.oppdaterSistOppdatertTidspunkt(behandling);
    }
}
