package no.nav.ung.sak.metrikker.bigquery.tabeller.ungdomsprogram;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import no.nav.ung.sak.metrikker.bigquery.BigQueryRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.BigQueryTabell;
import no.nav.ung.sak.metrikker.bigquery.tabeller.DateTimeUtils;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public record DagerIProgrammetRecord(
    BigDecimal antall,
    Long dagerIProgrammet,
    ZonedDateTime opprettetTidspunkt
) implements BigQueryRecord {

    public static final BigQueryTabell<DagerIProgrammetRecord> DAGER_I_PROGRAMMET_LØPENDE_BIG_QUERY_TABELL =
        new BigQueryTabell<>(
            "dager_i_programmet_løpende",
            Schema.of(
                Field.of("antall", StandardSQLTypeName.BIGNUMERIC),
                Field.of("dagerIProgrammet", StandardSQLTypeName.NUMERIC),
                Field.of("opprettetTidspunkt", StandardSQLTypeName.DATETIME)
            ),
            DagerIProgrammetRecord.class,
            rec -> Map.of(
                "antall", rec.antall(),
                "dagerIProgrammet", rec.dagerIProgrammet(),
                "opprettetTidspunkt", rec.opprettetTidspunkt().format(DateTimeFormatter.ofPattern(DateTimeUtils.DATE_TIME_FORMAT_PATTERN))
            )
        );
}
