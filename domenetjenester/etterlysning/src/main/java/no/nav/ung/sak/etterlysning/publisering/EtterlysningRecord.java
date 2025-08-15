package no.nav.ung.sak.etterlysning.publisering;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.metrikker.bigquery.BigQueryRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.BigQueryTabell;
import no.nav.ung.sak.metrikker.bigquery.tabeller.DateTimeUtils;
import no.nav.ung.sak.typer.Saksnummer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import static no.nav.ung.kodeverk.uttak.Tid.TIDENES_ENDE;

public record EtterlysningRecord(
    Saksnummer saksnummer,
    UUID eksternreferanse,
    EtterlysningType etterlysningType,
    EtterlysningStatus etterlysningStatus,
    DatoIntervallEntitet periode,
    LocalDateTime frist,
    LocalDateTime opprettetTidspunkt
) implements BigQueryRecord {

    public static final BigQueryTabell<EtterlysningRecord> ETTERLYSNING_TABELL =
        new BigQueryTabell<>(
            "etterlysning_v2",
            Schema.of(
                Field.of("saksnummer", StandardSQLTypeName.STRING),
                Field.of("eksternreferanse", StandardSQLTypeName.STRING),
                Field.of("type", StandardSQLTypeName.STRING),
                Field.of("status", StandardSQLTypeName.STRING),
                Field.of("fom", StandardSQLTypeName.DATE),
                Field.of("tom", StandardSQLTypeName.DATE),
                Field.of("frist", StandardSQLTypeName.DATETIME),
                Field.of("opprettetTidspunkt", StandardSQLTypeName.DATETIME)
            ),
            EtterlysningRecord.class,
            rec -> Map.of(
                "saksnummer", rec.saksnummer.getVerdi(),
                "eksternreferanse", rec.eksternreferanse.toString(),
                "type", rec.etterlysningType().getKode(),
                "status", rec.etterlysningStatus().getNavn(),
                "fom", rec.periode().getFomDato().format(DateTimeFormatter.ISO_LOCAL_DATE),
                "tom", rec.periode().getTomDato().format(DateTimeFormatter.ISO_LOCAL_DATE),
                "frist", rec.frist() != null ? rec.frist().format(DateTimeFormatter.ofPattern(DateTimeUtils.DATE_TIME_FORMAT_PATTERN)) :
                    TIDENES_ENDE.atStartOfDay().format(DateTimeFormatter.ofPattern(DateTimeUtils.DATE_TIME_FORMAT_PATTERN)),
                "opprettetTidspunkt", rec.opprettetTidspunkt().format(DateTimeFormatter.ofPattern(DateTimeUtils.DATE_TIME_FORMAT_PATTERN))
            )
        );
}
