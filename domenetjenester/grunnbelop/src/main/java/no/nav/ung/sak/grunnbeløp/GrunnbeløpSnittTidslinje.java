package no.nav.ung.sak.grunnbeløp;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

public class GrunnbeløpSnittTidslinje {

    private static final LocalDateTimeline<Grunnbeløp> GRUNNBELØP_SNITT_TIDSLINJE = new LocalDateTimeline<>(
        List.of(
            new LocalDateSegment<>(LocalDate.of(2026, 1, 1), LocalDate.of(2099, 12, 31), new Grunnbeløp(BigDecimal.valueOf(130160))),
            new LocalDateSegment<>(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), new Grunnbeløp(BigDecimal.valueOf(128116))),
            new LocalDateSegment<>(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), new Grunnbeløp(BigDecimal.valueOf(122225))),
            new LocalDateSegment<>(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31), new Grunnbeløp(BigDecimal.valueOf(116239)))
        ));

    public static LocalDateTimeline<Grunnbeløp> hentGrunnbeløpSnittTidslinje() {
        return GRUNNBELØP_SNITT_TIDSLINJE;
    }

    public static LocalDateTimeline<BigDecimal> lagOppjusteringsfaktorTidslinje(Year sisteÅr, int antallÅrTilbake) {
        var avgrensningsTimeline = new LocalDateInterval(
            sisteÅr.minusYears(antallÅrTilbake).atDay(1),
            sisteÅr.atMonth(12).atEndOfMonth()
        );

        var gsnittTidslinje = GRUNNBELØP_SNITT_TIDSLINJE.intersection(avgrensningsTimeline);
        var gsnittForEtterspurtÅr = gsnittTidslinje.toSegments().last().getValue();

        return gsnittTidslinje.mapValue(it -> gsnittForEtterspurtÅr.verdi().divide(it.verdi(), 10, RoundingMode.HALF_EVEN));
    }
}

