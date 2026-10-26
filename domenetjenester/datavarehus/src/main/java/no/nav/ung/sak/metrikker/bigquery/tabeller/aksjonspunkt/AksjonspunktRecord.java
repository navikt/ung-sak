package no.nav.ung.sak.metrikker.bigquery.tabeller.aksjonspunkt;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.sak.metrikker.bigquery.BigQueryRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.BigQueryTabell;
import no.nav.ung.sak.metrikker.bigquery.tabeller.DateTimeUtils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public record AksjonspunktRecord(
    FagsakYtelseType ytelseType,
    Long antall,
    AksjonspunktDefinisjon aksjonspunktDefinisjon,
    AksjonspunktStatus aksjonspunktStatus,
    Venteårsak ventearsak,
    ZonedDateTime opprettetTidspunkt
) implements BigQueryRecord {

    public static final BigQueryTabell<AksjonspunktRecord> AKSJONSPUNKT_TABELL =
        new BigQueryTabell<>(
            "aksjonspunkter_status",
            Schema.of(
                Field.of("ytelse_type", StandardSQLTypeName.STRING),
                Field.of("antall", StandardSQLTypeName.BIGNUMERIC),
                Field.of("aksjonspunkt_kode", StandardSQLTypeName.STRING),
                Field.of("aksjonspunkt_navn", StandardSQLTypeName.STRING),
                Field.of("aksjonspunkt_status", StandardSQLTypeName.STRING),
                Field.of("ventearsak", StandardSQLTypeName.STRING),
                Field.of("opprettetTidspunkt", StandardSQLTypeName.DATETIME)
            ),
            AksjonspunktRecord.class,
            rec -> Map.of(
                "ytelse_type", rec.ytelseType().getKode(),
                "antall", rec.antall(),
                "aksjonspunkt_kode", rec.aksjonspunktDefinisjon().getKode(),
                "aksjonspunkt_navn", rec.aksjonspunktDefinisjon().getNavn(),
                "aksjonspunkt_status", rec.aksjonspunktStatus().getNavn(),
                "ventearsak", rec.ventearsak().getNavn(),
                "opprettetTidspunkt", rec.opprettetTidspunkt().format(DateTimeFormatter.ofPattern(DateTimeUtils.DATE_TIME_FORMAT_PATTERN))
            )
        );
}
