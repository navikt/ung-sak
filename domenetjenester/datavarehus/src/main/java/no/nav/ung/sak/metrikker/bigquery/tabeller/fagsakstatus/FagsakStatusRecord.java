package no.nav.ung.sak.metrikker.bigquery.tabeller.fagsakstatus;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.sak.metrikker.bigquery.BigQueryRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.BigQueryTabell;
import no.nav.ung.sak.metrikker.bigquery.tabeller.DateTimeUtils;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public record FagsakStatusRecord(
    BigDecimal antall,
    FagsakStatus fagsakStatus,
    ZonedDateTime opprettetTidspunkt
) implements BigQueryRecord {

    public static final BigQueryTabell<FagsakStatusRecord> FAGSAK_STATUS_TABELL_V2 =
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
                "opprettetTidspunkt", rec.opprettetTidspunkt().format(DateTimeFormatter.ofPattern(DateTimeUtils.DATE_TIME_FORMAT_PATTERN))
            )
        );
}
