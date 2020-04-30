package no.nav.k9.sak.metrikker;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.prosessering.task.FortsettBehandlingTaskProperties;
import no.nav.vedtak.felles.integrasjon.sensu.SensuKlient;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(FortsettBehandlingTaskProperties.TASKTYPE)
public class SensuMetrikkTask implements ProsessTaskHandler {
    static final String TASKTYPE = "sensu.metrikk.task";

    private SensuKlient sensuKlient;

    private StatistikkRepository statistikkRepository;

    SensuMetrikkTask() {
        // for proxyd
    }

    @Inject
    public SensuMetrikkTask(SensuKlient sensuKlient, StatistikkRepository statistikkRepository) {
        this.sensuKlient = sensuKlient;
        this.statistikkRepository = statistikkRepository;
    }

    @Override
    public void doTask(ProsessTaskData data) {

        statistikkRepository.aksjonspunktStatistikk().forEach(e -> sensuKlient.logMetrics(e));
        
        statistikkRepository.aksjonspunktVenteårsakStatistikk().forEach(e -> sensuKlient.logMetrics(e));
        
    }
}
