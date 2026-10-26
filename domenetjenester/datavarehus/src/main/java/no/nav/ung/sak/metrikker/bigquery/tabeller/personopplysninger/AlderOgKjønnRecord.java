package no.nav.ung.sak.metrikker.bigquery.tabeller.personopplysninger;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.kodeverk.person.NavBrukerKjønn;
import no.nav.ung.sak.behandlingslager.kodeverk.KjønnKodeverdiConverter;
import no.nav.ung.sak.metrikker.bigquery.BigQueryRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.BigQueryTabell;
import no.nav.ung.sak.metrikker.bigquery.tabeller.DateTimeUtils;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public record AlderOgKjønnRecord(
    BigDecimal antall,
    int alder,
    NavBrukerKjønn navBrukerKjønn,
    ZonedDateTime opprettetTidspunkt
) implements BigQueryRecord {

    public static final BigQueryTabell<AlderOgKjønnRecord> ALDER_OG_KJØNN_BIG_QUERY_TABELL =
        new BigQueryTabell<>(
            "alder_og_kjønn",
            Schema.of(
                Field.of("antall", StandardSQLTypeName.BIGNUMERIC),
                Field.of("alder", StandardSQLTypeName.NUMERIC),
                Field.of("kjønn", StandardSQLTypeName.STRING),
                Field.of("opprettetTidspunkt", StandardSQLTypeName.DATETIME)
            ),
            AlderOgKjønnRecord.class,
            rec -> Map.of(
                "antall", rec.antall(),
                "alder", rec.alder(),
                "kjønn", rec.navBrukerKjønn().getNavn(),
                "opprettetTidspunkt", rec.opprettetTidspunkt().format(DateTimeFormatter.ofPattern(DateTimeUtils.DATE_TIME_FORMAT_PATTERN))
            )
        );
}
