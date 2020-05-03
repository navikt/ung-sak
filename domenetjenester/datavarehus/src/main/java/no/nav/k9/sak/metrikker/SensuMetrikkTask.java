package no.nav.k9.sak.metrikker;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.felles.integrasjon.sensu.SensuEvent;
import no.nav.vedtak.felles.integrasjon.sensu.SensuKlient;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(SensuMetrikkTask.TASKTYPE)
public class SensuMetrikkTask implements ProsessTaskHandler {

    static final String TASKTYPE = "sensu.metrikk.task";

    private static final Logger log = LoggerFactory.getLogger(SensuMetrikkTask.class);

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
        long startTime = System.nanoTime();

        try {
            List<SensuEvent> metrikker = new ArrayList<>();
            metrikker.addAll(statistikkRepository.prosessTaskStatistikk());
            metrikker.addAll(statistikkRepository.behandlingStatistikkUnderBehandling());
            metrikker.addAll(statistikkRepository.behandlingStatistikkStartetIDag());
            metrikker.addAll(statistikkRepository.behandlingStatistikkAvsluttetIDag());
            metrikker.addAll(statistikkRepository.aksjonspunktStatistikk());
            metrikker.addAll(statistikkRepository.aksjonspunktVenteårsakStatistikk());
            metrikker.addAll(statistikkRepository.fagsakStatistikk());
            logMetrics(metrikker);
        } finally {
            var varighet = Duration.ofNanos(System.nanoTime() - startTime);
            if (Duration.ofSeconds(20).minus(varighet).isNegative()) {
                // bruker for lang tid på logging av metrikker.
                log.warn("Generering av sensu metrikker tok : " + varighet);
            }
        }

    }

    private void logMetrics(List<SensuEvent> events) {
        sensuKlient.logMetrics(events);
    }
}
