package no.nav.ung.sak.metrikker.bigquery;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.util.Tuple;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskStatus;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.metrikker.bigquery.tabeller.BigQueryTabell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_BEGYNNELSE;

/**
 * Henter metrikker for én ytelse ({@link YtelseStatistikk}) og publiserer dem til BigQuery.
 * <p>
 * Brukes av cron-taskene, som har én task per ytelse og frekvens. Da får hver ytelse egen retry og eget
 * sist kjørt-tidspunkt, slik at feil i én ytelse verken stopper eller sender metrikkene for en annen ytelse på nytt.
 */
@ApplicationScoped
public class BigQueryMetrikkPubliserer {

    private static final Logger log = LoggerFactory.getLogger(BigQueryMetrikkPubliserer.class);
    private static final Duration VARSLINGSGRENSE = Duration.ofSeconds(20);

    private boolean bigQueryEnabled;
    private BigQueryKlient bigQueryKlient;
    private Instance<YtelseStatistikk> ytelseStatistikk;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    BigQueryMetrikkPubliserer() {
        // for proxyd
    }

    @Inject
    public BigQueryMetrikkPubliserer(@KonfigVerdi(value = "BIGQUERY_ENABLED", required = false, defaultVerdi = "false") boolean bigQueryEnabled,
                                     BigQueryKlient bigQueryKlient,
                                     @Any Instance<YtelseStatistikk> ytelseStatistikk,
                                     ProsessTaskTjeneste prosessTaskTjeneste) {
        this.bigQueryEnabled = bigQueryEnabled;
        this.bigQueryKlient = bigQueryKlient;
        this.ytelseStatistikk = ytelseStatistikk;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    /**
     * Publiserer hyppig rapporterte metrikker. Hendelsesbaserte metrikker (f.eks. etterlysninger og uttalelser) hentes
     * fra og med forrige vellykkede kjøring av tasken med gitt tasktype.
     */
    public void publiserHyppigRapporterte(FagsakYtelseType ytelse, String taskType) {
        var sistKjørtTidspunkt = finnSistKjørtTidspunkt(taskType);
        log.info("Sist kjørt tidspunkt for {}: {}", taskType, sistKjørtTidspunkt);
        publiser(ytelse, () -> YtelseStatistikk.finn(ytelseStatistikk, ytelse).hentHyppigRapporterte(sistKjørtTidspunkt));
    }

    public void publiserDagligRapporterte(FagsakYtelseType ytelse) {
        publiser(ytelse, () -> YtelseStatistikk.finn(ytelseStatistikk, ytelse).hentDagligRapporterte());
    }

    private void publiser(FagsakYtelseType ytelse, Supplier<List<Tuple<BigQueryTabell<?>, Collection<?>>>> metrikkHenter) {
        YtelseStatistikk.validerStøttet(ytelse);
        long startTime = System.nanoTime();
        try {
            var metrikker = metrikkHenter.get();
            if (bigQueryEnabled && !metrikker.isEmpty()) {
                metrikker.forEach(this::publiser);
            } else {
                log.info("Ingen metrikker å publisere for ytelse {} eller BigQuery er ikke aktivert.", ytelse.getKode());
            }
        } finally {
            var varighet = Duration.ofNanos(System.nanoTime() - startTime);
            if (VARSLINGSGRENSE.minus(varighet).isNegative()) {
                log.warn("Generering av BigQuery metrikker for ytelse {} tok: {}", ytelse.getKode(), varighet);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void publiser(Tuple<BigQueryTabell<?>, Collection<?>> metrikk) {
        var tabell = (BigQueryTabell<BigQueryRecord>) metrikk.getElement1();
        var records = (Collection<BigQueryRecord>) metrikk.getElement2();
        bigQueryKlient.publish(BigQueryDataset.UNG_SAK_STATISTIKK_DATASET, tabell, records);
    }

    private LocalDateTime finnSistKjørtTidspunkt(String taskType) {
        return prosessTaskTjeneste.finnAlle(taskType, ProsessTaskStatus.FERDIG).stream()
            .map(ProsessTaskData::getSistKjørt)
            .max(Comparator.naturalOrder())
            .orElse(TIDENES_BEGYNNELSE.atStartOfDay());
    }
}
