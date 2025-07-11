package no.nav.ung.sak.metrikker.bigquery;

import no.nav.ung.kodeverk.behandling.FagsakStatus;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record FagsakStatusRecord(
    BigDecimal antall,
    FagsakStatus fagsakStatus,
    ZonedDateTime opprettetTidspunkt
) implements BigQueryRecord {
}
