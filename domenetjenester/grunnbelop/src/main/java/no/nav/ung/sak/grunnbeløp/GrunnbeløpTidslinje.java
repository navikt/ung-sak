package no.nav.ung.sak.grunnbeløp;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class GrunnbeløpTidslinje {
    /**
     * Tidslinje for grunnbeløpsatser
     */
    private static final LocalDateTimeline<BigDecimal> GRUNNBELØP_TIDSLINJE = new LocalDateTimeline<>(
        List.of(
            new LocalDateSegment<>(LocalDate.of(2024, 5, 1), LocalDate.of(2099, 12, 31), BigDecimal.valueOf(124028)),
            new LocalDateSegment<>(LocalDate.of(2023, 5, 1), LocalDate.of(2024, 4, 30), BigDecimal.valueOf(118620))
        ));


    public static LocalDateTimeline<BigDecimal> hentTidslinje() {
        return GRUNNBELØP_TIDSLINJE;
    }
}
