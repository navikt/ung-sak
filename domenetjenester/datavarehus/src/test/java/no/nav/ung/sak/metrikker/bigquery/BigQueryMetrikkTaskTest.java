package no.nav.ung.sak.metrikker.bigquery;

import no.nav.k9.prosesstask.api.BatchProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Sikrer at hver støttede ytelse har egne cron-tasker, slik at feil i én ytelse ikke påvirker en annen.
 */
class BigQueryMetrikkTaskTest {

    private final BigQueryMetrikkPubliserer publiserer = mock(BigQueryMetrikkPubliserer.class);

    private final List<BatchProsessTaskHandler> hyppigeTasker = List.of(
        new BigQueryMetrikkTask(publiserer),
        new AktivitetspengerBigQueryMetrikkTask(publiserer));

    private final List<BatchProsessTaskHandler> dagligeTasker = List.of(
        new BigQueryMetrikkDagligTask(publiserer),
        new AktivitetspengerBigQueryMetrikkDagligTask(publiserer));

    @Test
    void skal_ha_én_timetask_per_støttet_ytelse_som_bruker_egen_tasktype_for_sist_kjørt() {
        hyppigeTasker.forEach(BigQueryMetrikkTaskTest::kjør);

        var ytelser = ArgumentCaptor.forClass(FagsakYtelseType.class);
        var tasktyper = ArgumentCaptor.forClass(String.class);
        verify(publiserer, times(hyppigeTasker.size())).publiserHyppigRapporterte(ytelser.capture(), tasktyper.capture());
        verifyNoMoreInteractions(publiserer);

        assertThat(ytelser.getAllValues()).containsExactlyInAnyOrderElementsOf(YtelseStatistikk.STØTTEDE_YTELSER);
        assertThat(tasktyper.getAllValues()).containsExactlyElementsOf(hyppigeTasker.stream().map(BigQueryMetrikkTaskTest::tasktype).toList());
    }

    @Test
    void skal_ha_én_daglig_task_per_støttet_ytelse() {
        dagligeTasker.forEach(BigQueryMetrikkTaskTest::kjør);

        var ytelser = ArgumentCaptor.forClass(FagsakYtelseType.class);
        verify(publiserer, times(dagligeTasker.size())).publiserDagligRapporterte(ytelser.capture());
        verifyNoMoreInteractions(publiserer);

        assertThat(ytelser.getAllValues()).containsExactlyInAnyOrderElementsOf(YtelseStatistikk.STØTTEDE_YTELSER);
    }

    @Test
    void skal_ha_unike_tasktyper() {
        var tasktyper = Stream.concat(hyppigeTasker.stream(), dagligeTasker.stream())
            .map(BigQueryMetrikkTaskTest::tasktype)
            .toList();

        assertThat(tasktyper).doesNotHaveDuplicates();
    }

    @Test
    void skal_beholde_tasktypene_for_ungdomsprogramytelsen() {
        // Endret tasktype gir ny cron-task som må startes manuelt, og sist kjørt-tidspunktet nullstilles.
        assertThat(tasktype(new BigQueryMetrikkTask(publiserer))).isEqualTo("bigquery.metrikk.task");
        assertThat(tasktype(new BigQueryMetrikkDagligTask(publiserer))).isEqualTo("bigquery.metrikk.daglig.task");
    }

    private static void kjør(BatchProsessTaskHandler task) {
        task.doTask(new ProsessTaskData(tasktype(task)));
    }

    private static String tasktype(BatchProsessTaskHandler task) {
        return task.getClass().getAnnotation(ProsessTask.class).value();
    }
}
