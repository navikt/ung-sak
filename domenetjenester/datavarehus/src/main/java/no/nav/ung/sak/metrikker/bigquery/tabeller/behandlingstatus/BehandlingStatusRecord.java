package no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingstatus;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.sak.metrikker.bigquery.BigQueryRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.BigQueryTabell;
import no.nav.ung.sak.metrikker.bigquery.tabeller.DateTimeUtils;

public record BehandlingStatusRecord(
    BigDecimal totaltAntall,
    FagsakYtelseType ytelseType,
    BehandlingType behandlingType,
    BehandlingStatus behandlingStatus,
    ZonedDateTime opprettetTidspunkt
) implements BigQueryRecord {

    public static final BigQueryTabell<BehandlingStatusRecord> BEHANDLING_STATUS_TABELL =
        new BigQueryTabell<>(
            "behandling_status",
            Schema.of(
                Field.of("antall", StandardSQLTypeName.BIGNUMERIC),
                Field.of("ytelse_type", StandardSQLTypeName.STRING),
                Field.of("behandling_type", StandardSQLTypeName.STRING),
                Field.of("behandling_status", StandardSQLTypeName.STRING),
                Field.of("opprettetTidspunkt", StandardSQLTypeName.DATETIME)
            ),
            BehandlingStatusRecord.class,
            rec -> Map.of(
                "antall", rec.totaltAntall(),
                "ytelse_type", rec.ytelseType().getNavn(),
                "behandling_type", rec.behandlingType().getNavn(),
                "behandling_status", rec.behandlingStatus().getNavn(),
                "opprettetTidspunkt", rec.opprettetTidspunkt().format(DateTimeFormatter.ofPattern(DateTimeUtils.DATE_TIME_FORMAT_PATTERN))
            )
        );
}

