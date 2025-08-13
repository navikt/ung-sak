package no.nav.ung.sak.metrikker.bigquery.tabeller.etterlysning;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.metrikker.bigquery.BigQueryRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.BigQueryTabell;
import no.nav.ung.sak.metrikker.bigquery.tabeller.DateTimeUtils;
import no.nav.ung.sak.typer.Saksnummer;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public record EtterlysningRecord(
    Saksnummer saksnummer,
    EtterlysningType etterlysningType,
    EtterlysningStatus etterlysningStatus,
    DatoIntervallEntitet periode,
    ZonedDateTime opprettetTidspunkt
) implements BigQueryRecord {

    public static final BigQueryTabell<EtterlysningRecord> ETTERLYSNING_TABELL =
        new BigQueryTabell<>(
            "aksjonspunkter_status",
            Schema.of(
                Field.of("saksnummer", StandardSQLTypeName.STRING),
                Field.of("type", StandardSQLTypeName.STRING),
                Field.of("status", StandardSQLTypeName.STRING),
                Field.of("periode", StandardSQLTypeName.RANGE),
                Field.of("opprettetTidspunkt", StandardSQLTypeName.DATETIME)
            ),
            EtterlysningRecord.class,
            rec -> Map.of(
                "saksnummer", rec.saksnummer.getVerdi(),
                "type", rec.etterlysningType().getKode(),
                "status", rec.etterlysningStatus().getNavn(),
                "periode", toRange(rec.periode()),
                "opprettetTidspunkt", rec.opprettetTidspunkt().format(DateTimeFormatter.ofPattern(DateTimeUtils.DATE_TIME_FORMAT_PATTERN))
            ),
            true
        );

    public static String toRange(DatoIntervallEntitet periode) {
        return "[" + periode.getFomDato() + "," + periode.getTomDato() + "]";
    }
}
