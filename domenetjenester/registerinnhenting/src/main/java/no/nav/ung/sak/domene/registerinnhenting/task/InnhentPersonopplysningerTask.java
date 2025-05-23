package no.nav.ung.sak.domene.registerinnhenting.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.task.UnderBehandlingProsessTask;
import no.nav.ung.sak.domene.registerinnhenting.RegisterdataInnhenter;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(InnhentPersonopplysningerTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class InnhentPersonopplysningerTask extends UnderBehandlingProsessTask {

    public static final String TASKTYPE = "innhentsaksopplysninger.personopplysninger";
    private static final Logger LOGGER = LoggerFactory.getLogger(InnhentPersonopplysningerTask.class);
    private RegisterdataInnhenter registerdataInnhenter;

    InnhentPersonopplysningerTask() {
        // for CDI proxy
    }

    @Inject
    public InnhentPersonopplysningerTask(BehandlingRepositoryProvider repositoryProvider,
                                         RegisterdataInnhenter registerdataInnhenter) {
        super(repositoryProvider.getBehandlingRepository(), null /* håndterer låsing selv på senere tidspunkt */);
        this.registerdataInnhenter = registerdataInnhenter;
    }

    @Override
    public void doProsesser(ProsessTaskData prosessTaskData, Behandling behandling) {
        LOGGER.info("Innhenter personopplysninger for behandling: {}", behandling.getId());
        registerdataInnhenter.innhentPersonopplysninger(behandling);
    }
}
