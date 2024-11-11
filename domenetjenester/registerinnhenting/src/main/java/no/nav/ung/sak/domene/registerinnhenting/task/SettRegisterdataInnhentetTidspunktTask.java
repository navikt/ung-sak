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
@ProsessTask(SettRegisterdataInnhentetTidspunktTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class SettRegisterdataInnhentetTidspunktTask extends UnderBehandlingProsessTask {

    public static final String TASKTYPE = "innhentsaksopplysninger.oppdaterttidspunkt";
    private static final Logger LOGGER = LoggerFactory.getLogger(SettRegisterdataInnhentetTidspunktTask.class);
    private RegisterdataInnhenter registerdataInnhenter;

    SettRegisterdataInnhentetTidspunktTask() {
        // for CDI proxy
    }

    @Inject
    public SettRegisterdataInnhentetTidspunktTask(BehandlingRepositoryProvider repositoryProvider,
                                                  RegisterdataInnhenter registerdataInnhenter) {
        super(repositoryProvider.getBehandlingRepository(), null);
        this.registerdataInnhenter = registerdataInnhenter;
    }

    @Override
    protected void doProsesser(ProsessTaskData prosessTaskData, Behandling behandling) {
        LOGGER.info("Oppdaterer registerdata innhentet tidspunkt behandling med id={} og uuid={}", behandling.getId(), behandling.getUuid());
        registerdataInnhenter.oppdaterSistOppdatertTidspunkt(behandling);
    }
}
