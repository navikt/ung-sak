package no.nav.ung.sak.grunnbeløp;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.typer.Beløp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class GrunnbeløpTidslinje {
    /**
     * Tidslinje for grunnbeløpsatser
     */
    private static final LocalDateTimeline<Grunnbeløp> GRUNNBELØP_TIDSLINJE = new LocalDateTimeline<>(
        List.of(
            new LocalDateSegment<>(LocalDate.of(2025, 5, 1), LocalDate.of(2099, 12, 31), new Grunnbeløp(BigDecimal.valueOf(130160))),
            new LocalDateSegment<>(LocalDate.of(2024, 5, 1), LocalDate.of(2025, 4, 30), new Grunnbeløp(BigDecimal.valueOf(124028))),
            new LocalDateSegment<>(LocalDate.of(2023, 5, 1), LocalDate.of(2024, 4,30), new Grunnbeløp(BigDecimal.valueOf(118620)))
        ));


    public static LocalDateTimeline<Grunnbeløp> hentTidslinje() {
        return GRUNNBELØP_TIDSLINJE;
    }
}
