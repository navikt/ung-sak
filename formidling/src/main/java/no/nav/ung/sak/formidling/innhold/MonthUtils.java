package no.nav.ung.sak.formidling.innhold;

import java.time.Month;

public class MonthUtils {
    public static String navnPåMåned(Month month) {
        return switch (month) {
            case JANUARY -> "januar";
            case FEBRUARY -> "februar";
            case MARCH -> "mars";
            case APRIL -> "april";
            case MAY -> "mai";
            case JUNE -> "juni";
            case JULY -> "juli";
            case AUGUST -> "august";
            case SEPTEMBER -> "september";
            case OCTOBER -> "oktober";
            case NOVEMBER -> "november";
            case DECEMBER -> "desember";
        };
    }
}
