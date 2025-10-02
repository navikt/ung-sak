package no.nav.ung.sak.web.app.tjenester.forvaltning;

import java.time.Month;

public enum MånedForRapportering {

    JANUAR,
    FEBRUAR,
    MARS,
    APRIL,
    MAI,
    JUNI,
    JULI,
    AUGUST,
    SEPTEMBER,
    OKTOBER,
    NOVEMBER,
    DESEMBER;

    Month tilMonth() {
        return switch (this) {
            case JANUAR -> Month.JANUARY;
            case FEBRUAR -> Month.FEBRUARY;
            case MARS -> Month.MARCH;
            case APRIL -> Month.APRIL;
            case MAI -> Month.MAY;
            case JUNI -> Month.JUNE;
            case JULI -> Month.JULY;
            case AUGUST -> Month.AUGUST;
            case SEPTEMBER -> Month.SEPTEMBER;
            case OKTOBER -> Month.OCTOBER;
            case NOVEMBER -> Month.NOVEMBER;
            case DESEMBER -> Month.DECEMBER;
        };
    }


}
