package no.nav.ung.sak.metrikker.bigquery.tabeller.inntektskontroll;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import no.nav.ung.sak.metrikker.bigquery.BigQueryRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.BigQueryTabell;
import no.nav.ung.sak.metrikker.bigquery.tabeller.DateTimeUtils;
import no.nav.ung.sak.typer.Saksnummer;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public record KontrollertePerioderRecord(
    Saksnummer saksnummer,
    BigDecimal rapportertInntekt,
    BigDecimal registerInntekt,
    BigDecimal fastsattInntekt,
    Boolean fastsattManuelt,
    YearMonth månedOgÅr,
    ZonedDateTime opprettetTidspunkt
) implements BigQueryRecord {

    public static final BigQueryTabell<KontrollertePerioderRecord> KONTROLLERTE_PERIODER_TABELL =
        new BigQueryTabell<>(
            "kontrollerte_perioder",
            Schema.of(
                Field.of("saksnummer", StandardSQLTypeName.STRING),
                Field.of("rapportertInntekt", StandardSQLTypeName.BIGNUMERIC),
                Field.of("registerInntekt", StandardSQLTypeName.BIGNUMERIC),
                Field.of("fastsattInntekt", StandardSQLTypeName.BIGNUMERIC),
                Field.of("månedOgÅr", StandardSQLTypeName.STRING),
                Field.of("opprettetTidspunkt", StandardSQLTypeName.DATETIME)
            ),
            KontrollertePerioderRecord.class,
            rec -> Map.of(
                "saksnummer", rec.saksnummer.getVerdi(),
                "rapportertInntekt", rec.rapportertInntekt(),
                "registerInntekt", rec.registerInntekt(),
                "fastsattInntekt", rec.fastsattInntekt(),
                "månedOgÅr", rec.månedOgÅr().format(DateTimeFormatter.ISO_LOCAL_DATE),
                "opprettetTidspunkt", rec.opprettetTidspunkt().format(DateTimeFormatter.ofPattern(DateTimeUtils.DATE_TIME_FORMAT_PATTERN))
            )
        );
}
