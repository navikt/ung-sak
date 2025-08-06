package no.nav.ung.sak.metrikker.bigquery;

import com.google.cloud.NoCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.DatasetInfo;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingstatus.BehandlingStatusRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.fagsakstatus.FagsakStatusRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BigQueryEmulatorContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

@Testcontainers
class BigQueryKlientTest {

    @Container
    private static final BigQueryEmulatorContainer BIG_QUERY_EMULATOR_CONTAINER =
        new BigQueryEmulatorContainer("ghcr.io/goccy/bigquery-emulator:0.4.3");

    private static BigQueryKlient bigQueryKlient;

    @BeforeAll
    static void setUp() {
        BigQuery bigQuery = opprettBigQuery();
        opprettDatasett(bigQuery);

        bigQueryKlient = new BigQueryKlient(true, bigQuery);
    }

    @Test
    void publisering_av_FAGSAK_STATUS_V2_fungerer() {
        bigQueryKlient.publish(
            BigQueryDataset.UNG_SAK_STATISTIKK_DATASET,
            FagsakStatusRecord.FAGSAK_STATUS_TABELL_V2,
            List.of(new FagsakStatusRecord(BigDecimal.ONE, FagsakStatus.OPPRETTET, ZonedDateTime.now()))
        );
    }

    @Test
    void publisering_av_BEHANDLING_STATUS_fungerer() {
        bigQueryKlient.publish(
            BigQueryDataset.UNG_SAK_STATISTIKK_DATASET,
            BehandlingStatusRecord.BEHANDLING_STATUS_TABELL,
            List.of(
                new BehandlingStatusRecord(
                    BigDecimal.TEN,
                    FagsakYtelseType.UNGDOMSYTELSE,
                    BehandlingType.FØRSTEGANGSSØKNAD,
                    BehandlingStatus.UTREDES,
                    ZonedDateTime.now()
                )
            )
        );
    }

    private static BigQuery opprettBigQuery() {
        return BigQueryOptions.newBuilder()
            .setProjectId("test-project")
            .setHost(BIG_QUERY_EMULATOR_CONTAINER.getEmulatorHttpEndpoint())
            .setCredentials(NoCredentials.getInstance())
            .build()
            .getService();
    }

    private static void opprettDatasett(BigQuery bigQuery) {
        bigQuery.create(DatasetInfo.newBuilder(BigQueryDataset.UNG_SAK_STATISTIKK_DATASET.getDatasetNavn()).build());
    }
}
