package no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingsårsak;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.metrikker.bigquery.BigQueryRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.BigQueryTabell;
import no.nav.ung.sak.metrikker.bigquery.tabeller.DateTimeUtils;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public record BehandlingÅrsakRecord(
    BigDecimal totaltAntall,
    BehandlingÅrsakType behandlingÅrsakType,
    boolean erFerdigbehandlet,
    ZonedDateTime opprettetTidspunkt
) implements BigQueryRecord {

    public static final BigQueryTabell<BehandlingÅrsakRecord> BEHANDLING_ÅRSAK_TABELL =
        new BigQueryTabell<>(
            "behandling_årsak",
            Schema.of(
                Field.of("antall", StandardSQLTypeName.BIGNUMERIC),
                Field.of("behandling_årsak", StandardSQLTypeName.STRING),
                Field.of("erFerdigbehandlet", StandardSQLTypeName.BOOL),
                Field.of("opprettetTidspunkt", StandardSQLTypeName.DATETIME)
            ),
            BehandlingÅrsakRecord.class,
            rec -> Map.of(
                "antall", rec.totaltAntall(),
                "behandling_årsak", rec.behandlingÅrsakType().getKode(),
                "erFerdigbehandlet", rec.erFerdigbehandlet(),
                "opprettetTidspunkt", rec.opprettetTidspunkt().format(DateTimeFormatter.ofPattern(DateTimeUtils.DATE_TIME_FORMAT_PATTERN))
            )
        );
}

