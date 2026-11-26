package no.nav.ung.sak.metrikker.bigquery.tabeller.parallellbehandling;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.metrikker.bigquery.BigQueryRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.BigQueryTabell;
import no.nav.ung.sak.metrikker.bigquery.tabeller.DateTimeUtils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Behandlinger som venter i ventepunktet for vedtaksprioritet (steg VURDER_PARALLELLE_BEHANDLINGER, autopunkt 7050).
 * Ventetiden regnes fra behandlingen kom til steget, og nullstilles ikke når steget kjøres på nytt etter vent.
 */
public record ParallellBehandlingVentRecord(
    FagsakYtelseType ytelseType,
    BehandlingType behandlingType,
    Long antall,
    Long eldsteVentetidTimer,
    ZonedDateTime opprettetTidspunkt
) implements BigQueryRecord {

    public static final BigQueryTabell<ParallellBehandlingVentRecord> PARALLELL_BEHANDLING_VENT_TABELL =
        new BigQueryTabell<>(
            "parallelle_behandlinger_vent",
            Schema.of(
                Field.of("ytelse_type", StandardSQLTypeName.STRING),
                Field.of("behandling_type", StandardSQLTypeName.STRING),
                Field.of("antall", StandardSQLTypeName.BIGNUMERIC),
                Field.of("eldste_ventetid_timer", StandardSQLTypeName.INT64),
                Field.of("opprettetTidspunkt", StandardSQLTypeName.DATETIME)
            ),
            ParallellBehandlingVentRecord.class,
            rec -> Map.of(
                "ytelse_type", rec.ytelseType().getKode(),
                "behandling_type", rec.behandlingType().getNavn(),
                "antall", rec.antall(),
                "eldste_ventetid_timer", rec.eldsteVentetidTimer(),
                "opprettetTidspunkt", rec.opprettetTidspunkt().format(DateTimeFormatter.ofPattern(DateTimeUtils.DATE_TIME_FORMAT_PATTERN))
            )
        );
}
