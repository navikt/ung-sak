package no.nav.ung.sak.metrikker.bigquery.tabeller.sats;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.sak.metrikker.bigquery.BigQueryRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.BigQueryTabell;
import no.nav.ung.sak.metrikker.bigquery.tabeller.DateTimeUtils;
import no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingstatus.BehandlingStatusRecord;

import java.time.format.DateTimeFormatter;
import java.util.Map;

public record SatsStatistikkRecord(
    Long antall,
    Integer antallBarn,
    UngdomsytelseSatsType satsType,
    java.time.ZonedDateTime opprettetTidspunkt
) implements BigQueryRecord {

    // BigQuery tabell
    public static final BigQueryTabell<SatsStatistikkRecord> SATS_STATISTIKK_TABELL = new BigQueryTabell<>(
        "sats_statistikk",
        Schema.of(
            Field.of("antall", StandardSQLTypeName.BIGNUMERIC),
            Field.of("antall_barn", StandardSQLTypeName.INT64),
            Field.of("sats_type", StandardSQLTypeName.STRING),
            Field.of("opprettetTidspunkt", StandardSQLTypeName.DATETIME)
        ),
        SatsStatistikkRecord.class,
        rec -> Map.of(
            "antall", rec.antall(),
            "antall_barn", rec.antallBarn(),
            "sats_type", rec.satsType().getNavn(),
            "opprettetTidspunkt", rec.opprettetTidspunkt().format(DateTimeFormatter.ofPattern(DateTimeUtils.DATE_TIME_FORMAT_PATTERN))
        )
    );
}
