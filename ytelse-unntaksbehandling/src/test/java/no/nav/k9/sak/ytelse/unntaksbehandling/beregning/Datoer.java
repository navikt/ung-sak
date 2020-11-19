package no.nav.k9.sak.ytelse.unntaksbehandling.beregning;

import static java.time.temporal.TemporalQueries.localDate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Datoer {
    private static final DateTimeFormatter yyyyMMdd = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public static LocalDate dato(final String dato) {
        return yyyyMMdd.parse(dato).query(localDate());
    }
}
