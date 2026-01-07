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
    boolean fastsattManuelt,
    YearMonth månedOgÅr,
    LocalDate fom,
    LocalDate tom,
    ZonedDateTime opprettetTidspunkt
) implements BigQueryRecord {

    public static final BigQueryTabell<KontrollertePerioderRecord> KONTROLLERTE_PERIODER_TABELL =
        new BigQueryTabell<>(
            "kontrollerte_inntekt_perioder_v2",
            Schema.of(
                Field.of("saksnummer", StandardSQLTypeName.STRING),
                Field.of("rapportertInntekt", StandardSQLTypeName.BIGNUMERIC),
                Field.of("registerInntekt", StandardSQLTypeName.BIGNUMERIC),
                Field.of("fastsattInntekt", StandardSQLTypeName.BIGNUMERIC),
                Field.of("månedOgÅr", StandardSQLTypeName.STRING),
                Field.of("fom", StandardSQLTypeName.DATE),
                Field.of("tom", StandardSQLTypeName.DATE),
                Field.of("erManueltFastsatt", StandardSQLTypeName.BOOL),
                Field.of("opprettetTidspunkt", StandardSQLTypeName.DATETIME)
            ),
            KontrollertePerioderRecord.class,
            rec -> Map.of(
                "saksnummer", rec.saksnummer.getVerdi(),
                "rapportertInntekt", rec.rapportertInntekt() != null ? rec.rapportertInntekt() : 0,
                "registerInntekt", rec.registerInntekt() != null ? rec.registerInntekt() : 0,
                "fastsattInntekt", rec.fastsattInntekt() != null ? rec.fastsattInntekt() : 0,
                "månedOgÅr", rec.månedOgÅr().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                "fom", rec.fom().format(DateTimeFormatter.ISO_LOCAL_DATE),
                "tom", rec.tom().format(DateTimeFormatter.ISO_LOCAL_DATE),
                "erManueltFastsatt", rec.fastsattManuelt(),
                "opprettetTidspunkt", rec.opprettetTidspunkt().format(DateTimeFormatter.ofPattern(DateTimeUtils.DATE_TIME_FORMAT_PATTERN))
            )
        );
}
