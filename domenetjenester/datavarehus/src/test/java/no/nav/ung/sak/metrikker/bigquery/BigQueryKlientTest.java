package no.nav.ung.sak.metrikker.bigquery;

import com.google.cloud.NoCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.DatasetInfo;
import no.nav.ung.kodeverk.behandling.*;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.kodeverk.forhåndsvarsel.EtterlysningStatus;
import no.nav.ung.kodeverk.forhåndsvarsel.EtterlysningType;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.metrikker.bigquery.tabeller.aksjonspunkt.AksjonspunktRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingsresultat.BehandslingsresultatStatistikkRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingstatus.BehandlingStatusRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingsårsak.BehandlingÅrsakRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.etterlysning.EtterlysningRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.fagsakstatus.FagsakStatusRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.sats.SatsStatistikkRecord;
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BigQueryEmulatorContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Test
    void publisering_av_SATS_STATISTIKK_fungerer() {
        bigQueryKlient.publish(
            BigQueryDataset.UNG_SAK_STATISTIKK_DATASET,
            SatsStatistikkRecord.SATS_STATISTIKK_TABELL,
            List.of(
                new SatsStatistikkRecord(
                    4L,
                    2,
                    UngdomsytelseSatsType.HØY,
                    ZonedDateTime.now()
                )
            )
        );
    }

    @Test
    void publisering_av_AKSJONSPUNKT_TABELL_fungerer() {
        bigQueryKlient.publish(
            BigQueryDataset.UNG_SAK_STATISTIKK_DATASET,
            AksjonspunktRecord.AKSJONSPUNKT_TABELL,
            List.of(
                new AksjonspunktRecord(
                    FagsakYtelseType.UNGDOMSYTELSE,
                    1L,
                    AksjonspunktDefinisjon.KONTROLLER_INNTEKT,
                    AksjonspunktStatus.OPPRETTET,
                    Venteårsak.VENTER_PÅ_ETTERLYST_INNTEKT_UTTALELSE,
                    ZonedDateTime.now()
                )
            )
        );
    }

    @Test
    void publisering_av_BEHANDLINGSRESULTAT_STATISTIKK_TABELL_fungerer() {
        bigQueryKlient.publish(
            BigQueryDataset.UNG_SAK_STATISTIKK_DATASET,
            BehandslingsresultatStatistikkRecord.BEHANDLINGSRESULTAT_STATISTIKK_TABELL,
            List.of(
                new BehandslingsresultatStatistikkRecord(
                    BehandlingType.FØRSTEGANGSSØKNAD,
                    BehandlingResultatType.INNVILGET,
                    1L,
                    1L,
                    0L,
                    ZonedDateTime.now()
                )
            )
        );
    }

    @Test
    void publisering_av_ETTERLYSING_fungerer() {
        bigQueryKlient.publish(
            BigQueryDataset.UNG_SAK_STATISTIKK_DATASET,
            EtterlysningRecord.ETTERLYSNING_TABELL,
            List.of(
                new EtterlysningRecord(
                    new Saksnummer("SAKEN"),
                    EtterlysningType.UTTALELSE_ENDRET_SLUTTDATO,
                    EtterlysningStatus.SKAL_AVBRYTES,
                    DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(10)),
                    ZonedDateTime.now(),
                    ZonedDateTime.now()
                )
            )
        );
    }

    @Test
    void publisering_av_BEHANDLING_ÅRSAK_fungerer() {
        bigQueryKlient.publish(
            BigQueryDataset.UNG_SAK_STATISTIKK_DATASET,
            BehandlingÅrsakRecord.BEHANDLING_ÅRSAK_TABELL,
            List.of(
                new BehandlingÅrsakRecord(
                    BigDecimal.TEN,
                    BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT,
                    true,
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
