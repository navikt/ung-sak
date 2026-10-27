package no.nav.ung.sak.metrikker.bigquery.tabeller.soknad;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.sak.metrikker.bigquery.BigQueryRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.BigQueryTabell;
import no.nav.ung.sak.metrikker.bigquery.tabeller.DateTimeUtils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public record SøknadStatistikkRecord(
    DokumentStatus dokumentStatus,
    Long antallTotalt,
    Long antallMottattIDag,
    ZonedDateTime opprettetTidspunkt
) implements BigQueryRecord {

    public static final BigQueryTabell<SøknadStatistikkRecord> AKTIVITETSPENGER_SØKNAD_TABELL =
        new BigQueryTabell<>(
            "aktivitetspenger_soknad_mottatt",
            Schema.of(
                Field.of("dokument_status", StandardSQLTypeName.STRING),
                Field.of("antall_totalt", StandardSQLTypeName.BIGNUMERIC),
                Field.of("antall_mottatt_i_dag", StandardSQLTypeName.BIGNUMERIC),
                Field.of("opprettetTidspunkt", StandardSQLTypeName.DATETIME)
            ),
            SøknadStatistikkRecord.class,
            rec -> Map.of(
                "dokument_status", rec.dokumentStatus().getKode(),
                "antall_totalt", rec.antallTotalt(),
                "antall_mottatt_i_dag", rec.antallMottattIDag(),
                "opprettetTidspunkt", rec.opprettetTidspunkt().format(DateTimeFormatter.ofPattern(DateTimeUtils.DATE_TIME_FORMAT_PATTERN))
            )
        );
}
