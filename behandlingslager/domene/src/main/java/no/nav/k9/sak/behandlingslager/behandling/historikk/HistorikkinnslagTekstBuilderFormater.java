package no.nav.k9.sak.behandlingslager.behandling.historikk;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import no.nav.fpsak.tidsserie.LocalDateInterval;

public final class HistorikkinnslagTekstBuilderFormater {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");


    private HistorikkinnslagTekstBuilderFormater() {
    }

    public static <T> String formatString(T verdi) {
        if (verdi == null) {
            return null;
        }
        if (verdi instanceof LocalDate) {
            LocalDate localDate = (LocalDate) verdi;
            return formatDate(localDate);
        }
        if (verdi instanceof LocalDateInterval) {
            LocalDateInterval interval = (LocalDateInterval) verdi;
            return formatDate(interval.getFomDato()) + " - " + formatDate(interval.getTomDato());
        }
        if (verdi instanceof Number) {
            return String.format(Locale.US, "%,d", ((Number) verdi).intValue()).replace(",", " ");
        }
        return verdi.toString();
    }

    public static String formatDate(LocalDate localDate) {
        return DATE_FORMATTER.format(localDate);
    }
}
