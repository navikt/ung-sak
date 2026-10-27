package no.nav.ung.sak.metrikker.bigquery;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.BatchProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.impl.cron.CronExpression;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;

/**
 * Task for publisering av daglig rapporterte metrikker for aktivitetspenger til BigQuery.
 * Det er satt opp en cron-jobb som kjører denne tasken kl. 07:00 hver dag.
 * Det er også satt en grense for maksimalt antall mislykkede kjøringer til 20.
 * <p>
 * Hver ytelse har egen task, se {@link BigQueryMetrikkPubliserer}. Tasken må startes manuelt én gang per miljø
 * (<code>POST /prosesstask/create</code>), deretter planlegger den seg selv.
 */
@ApplicationScoped
@ProsessTask(value = AktivitetspengerBigQueryMetrikkDagligTask.TASKTYPE, maxFailedRuns = 20, firstDelay = 60)
public class AktivitetspengerBigQueryMetrikkDagligTask implements BatchProsessTaskHandler {

    static final String TASKTYPE = "bigquery.metrikk.daglig.aktivitetspenger.task";

    private BigQueryMetrikkPubliserer publiserer;

    AktivitetspengerBigQueryMetrikkDagligTask() {
        // for proxyd
    }

    @Inject
    public AktivitetspengerBigQueryMetrikkDagligTask(BigQueryMetrikkPubliserer publiserer) {
        this.publiserer = publiserer;
    }

    @Override
    public void doTask(ProsessTaskData data) {
        publiserer.publiserDagligRapporterte(FagsakYtelseType.AKTIVITETSPENGER);
    }

    @Override
    public CronExpression getCron() {
        return CronExpression.create("0 0 7 * * *");
    }
}
