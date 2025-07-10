package no.nav.ung.sak.metrikker.bigquery;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;

import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Definisjon av BigQuery-tabeller som brukes for Ung Sak.
 * Legg til nye tabeller her for å utvide BigQuery-integrasjonen.
 */
public final class Tabeller {
    private static final String DATE_TIME_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss.SSSSSS";

    private Tabeller() {
    }

    public static final BigQueryTabell<FagsakStatusRecord> FAGSAK_STATUS_V2 =
        new BigQueryTabell<>(
            "fagsak_status_v2",
            Schema.of(
                Field.of("antall", StandardSQLTypeName.BIGNUMERIC),
                Field.of("fagsak_status", StandardSQLTypeName.STRING),
                Field.of("opprettetTidspunkt", StandardSQLTypeName.DATETIME)
            ),
            FagsakStatusRecord.class,
            rec -> Map.of(
                "antall", rec.antall(),
                "fagsak_status", rec.fagsakStatus().getKode(),
                "opprettetTidspunkt", rec.opprettetTidspunkt().format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_PATTERN))
            )
        );
}
