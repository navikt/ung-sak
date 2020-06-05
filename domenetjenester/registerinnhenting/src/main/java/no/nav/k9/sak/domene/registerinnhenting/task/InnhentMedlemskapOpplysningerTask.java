package no.nav.k9.sak.domene.registerinnhenting.task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.UnderBehandlingProsessTask;
import no.nav.k9.sak.domene.registerinnhenting.RegisterdataInnhenter;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(InnhentMedlemskapOpplysningerTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class InnhentMedlemskapOpplysningerTask extends UnderBehandlingProsessTask {

    public static final String TASKTYPE = "innhentsaksopplysninger.medlemskap";
    private static final Logger LOGGER = LoggerFactory.getLogger(InnhentMedlemskapOpplysningerTask.class);
    private BehandlingRepository behandlingRepository;
    private RegisterdataInnhenter registerdataInnhenter;

    InnhentMedlemskapOpplysningerTask() {
        // for CDI proxy
    }

    @Inject
    public InnhentMedlemskapOpplysningerTask(BehandlingRepositoryProvider repositoryProvider,
                                             RegisterdataInnhenter registerdataInnhenter) {
        super(repositoryProvider.getBehandlingRepository(), null /* håndterer låsing selv på senere tidspunkt*/);
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.registerdataInnhenter = registerdataInnhenter;
    }

    @Override
    public void doProsesser(ProsessTaskData prosessTaskData) {
        Behandling behandling = behandlingRepository.hentBehandling(prosessTaskData.getBehandlingId());
        LOGGER.info("Innhenter medlemskapsopplysninger for behandling: {}", behandling.getId());
        registerdataInnhenter.innhentMedlemskapsOpplysning(behandling);
    }
}
