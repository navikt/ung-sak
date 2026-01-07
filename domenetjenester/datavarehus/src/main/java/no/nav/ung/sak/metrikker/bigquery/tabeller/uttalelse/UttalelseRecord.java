package no.nav.ung.sak.metrikker.bigquery.tabeller.uttalelse;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import no.nav.ung.kodeverk.varsel.EndringType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.metrikker.bigquery.BigQueryRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.BigQueryTabell;
import no.nav.ung.sak.metrikker.bigquery.tabeller.DateTimeUtils;
import no.nav.ung.sak.typer.Saksnummer;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public record UttalelseRecord(

    Saksnummer saksnummer,
    boolean harUttalelse,
    EndringType endringType,
    DatoIntervallEntitet periode,
    ZonedDateTime opprettetTidspunkt
) implements BigQueryRecord {

    public static final BigQueryTabell<UttalelseRecord> UTTALELSE_TABELL =
        new BigQueryTabell<>(
            "uttalelse",
            Schema.of(
                Field.of("saksnummer", StandardSQLTypeName.STRING),
                Field.of("harUttalelse", StandardSQLTypeName.BOOL),
                Field.of("type", StandardSQLTypeName.STRING),
                Field.of("fom", StandardSQLTypeName.DATE),
                Field.of("tom", StandardSQLTypeName.DATE),
                Field.of("opprettetTidspunkt", StandardSQLTypeName.DATETIME)
            ),
            UttalelseRecord.class,
            rec -> Map.of(
                "saksnummer", rec.saksnummer.getVerdi(),
                "harUttalelse", rec.harUttalelse,
                "type", rec.endringType().getKode(),
                "fom", rec.periode().getFomDato().format(DateTimeFormatter.ISO_LOCAL_DATE),
                "tom", rec.periode().getTomDato().format(DateTimeFormatter.ISO_LOCAL_DATE),
                "opprettetTidspunkt", rec.opprettetTidspunkt().format(DateTimeFormatter.ofPattern(DateTimeUtils.DATE_TIME_FORMAT_PATTERN))
            )
        );
}
