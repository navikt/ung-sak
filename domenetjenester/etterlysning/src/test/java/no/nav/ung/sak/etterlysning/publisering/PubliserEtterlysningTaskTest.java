package no.nav.ung.sak.etterlysning.publisering;

import com.google.cloud.NoCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.DatasetInfo;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.metrikker.bigquery.BigQueryDataset;
import no.nav.ung.sak.metrikker.bigquery.BigQueryKlient;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.BigQueryEmulatorContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
@Testcontainers
class PubliserEtterlysningTaskTest {

    @Container
    private static final BigQueryEmulatorContainer BIG_QUERY_EMULATOR_CONTAINER =
        new BigQueryEmulatorContainer("ghcr.io/goccy/bigquery-emulator:0.4.3");


    @Inject
    private EtterlysningRepository etterlysningRepository;

    @Inject
    private EntityManager entityManager;

    private static BigQueryKlient bigQueryKlient;

    private Behandling behandling;

    @BeforeEach
    void setUp() {
        var bigQuery = opprettBigQuery();
        opprettDatasett(bigQuery);
        behandling = TestScenarioBuilder.builderMedSøknad().lagre(entityManager);
        bigQueryKlient = new BigQueryKlient(true, bigQuery);

    }

    @Test
    void skal_publisere_etterlysning() {
        var etterlysning = etterlysningRepository.lagre(new Etterlysning(behandling.getId(), UUID.randomUUID(), UUID.randomUUID(),
            DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()), EtterlysningType.UTTALELSE_ENDRET_STARTDATO, EtterlysningStatus.OPPRETTET));
        etterlysningRepository.lagre(etterlysning);
        etterlysning.vent(LocalDateTime.now());
        etterlysningRepository.lagre(etterlysning);


        var task = new PubliserEtterlysningTask(etterlysningRepository, new BehandlingRepository(entityManager), bigQueryKlient);


        var prosessTaskData = ProsessTaskData.forProsessTask(PubliserEtterlysningTask.class);
        prosessTaskData.setProperty(PubliserEtterlysningTask.ETTERLYSNING_ID, etterlysning.getId().toString());
        task.doTask(prosessTaskData);


    }

    private static BigQuery opprettBigQuery() {
        return BigQueryOptions.newBuilder()
            .setProjectId(BIG_QUERY_EMULATOR_CONTAINER.getProjectId())
            .setHost(BIG_QUERY_EMULATOR_CONTAINER.getEmulatorHttpEndpoint())
            .setCredentials(NoCredentials.getInstance())
            .build()
            .getService();
    }

    private static void opprettDatasett(BigQuery bigQuery) {
        bigQuery.create(DatasetInfo.newBuilder(BigQueryDataset.UNG_SAK_STATISTIKK_DATASET.getDatasetNavn()).build());
    }
}
