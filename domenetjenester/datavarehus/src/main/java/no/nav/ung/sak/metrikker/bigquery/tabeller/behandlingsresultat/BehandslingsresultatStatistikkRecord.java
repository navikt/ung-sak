package no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingsresultat;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.sak.metrikker.bigquery.BigQueryRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.BigQueryTabell;
import no.nav.ung.sak.metrikker.bigquery.tabeller.DateTimeUtils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public record BehandslingsresultatStatistikkRecord(
    BehandlingType behandlingType,
    BehandlingResultatType behandlingResultatType,
    Long totalAntall,
    Long manuellBehandlingAntall,
    Long totrinnsbehandlingAntall,
    ZonedDateTime opprettetTidspunkt
) implements BigQueryRecord {

    public static final BigQueryTabell<BehandslingsresultatStatistikkRecord> BEHANDLINGSRESULTAT_STATISTIKK_TABELL = new BigQueryTabell<>(
        "behandlingsresultat_statistikk",
        Schema.of(
            Field.of("antall", StandardSQLTypeName.BIGNUMERIC),
            Field.of("behandlingstype_navn", StandardSQLTypeName.STRING),
            Field.of("behandlingResultatType_navn", StandardSQLTypeName.STRING),
            Field.of("total_antall", StandardSQLTypeName.STRING),
            Field.of("manuell_behandling_antall", StandardSQLTypeName.STRING),
            Field.of("totrinn_behandling_antall", StandardSQLTypeName.STRING),
            Field.of("opprettetTidspunkt", StandardSQLTypeName.DATETIME)
        ),
        BehandslingsresultatStatistikkRecord.class,
        rec -> Map.of(
            "behandlingstype_navn", rec.behandlingType().getNavn(),
            "behandlingResultatType_navn", rec.behandlingResultatType().getNavn(),
            "total_antall", rec.totalAntall().toString(),
            "manuell_behandling_antall", rec.manuellBehandlingAntall().toString(),
            "totrinn_behandling_antall", rec.totrinnsbehandlingAntall().toString(),
            "opprettetTidspunkt", rec.opprettetTidspunkt().format(DateTimeFormatter.ofPattern(DateTimeUtils.DATE_TIME_FORMAT_PATTERN))
        )
    );
}
