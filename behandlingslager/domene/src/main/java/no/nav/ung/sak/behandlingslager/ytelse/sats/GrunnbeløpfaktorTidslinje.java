package no.nav.ung.sak.behandlingslager.ytelse.sats;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

public class GrunnbeløpfaktorTidslinje {
    private static LocalDateTimeline<BigDecimal> LAV_GRUNNBELØPFAKTOR_TIDSLINJE = new LocalDateTimeline<>(
        List.of(
            new LocalDateSegment<>(LocalDate.of(2025, 1, 1), LocalDate.of(2099, 12, 31), BigDecimal.valueOf(4).divide(BigDecimal.valueOf(3), 5, RoundingMode.HALF_UP)),
            new LocalDateSegment<>(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), BigDecimal.valueOf(4).divide(BigDecimal.valueOf(3), 5, RoundingMode.HALF_UP))
        )
    );

    private static LocalDateTimeline<BigDecimal> HØY_GRUNNBELØPFAKTOR_TIDSLINJE = new LocalDateTimeline<>(
        List.of(
            new LocalDateSegment<>(LocalDate.of(2025, 1, 1), LocalDate.of(2099, 12, 31), BigDecimal.valueOf(2)),
            new LocalDateSegment<>(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), BigDecimal.valueOf(2))
        )
    );

    public static LocalDateTimeline<SatsOgGrunnbeløpfaktor> hentGrunnbeløpfaktorTidslinjeFor(LocalDateTimeline<Sats> sats) {
        return sats.stream()
            .map(segment -> switch (segment.getValue().getSatsType()) {
                case LAV -> LAV_GRUNNBELØPFAKTOR_TIDSLINJE
                    .intersection(segment.getLocalDateInterval())
                    .mapValue(grunnbeløpFaktor -> new SatsOgGrunnbeløpfaktor(segment.getValue().getSatsType(), grunnbeløpFaktor));

                case HØY -> HØY_GRUNNBELØPFAKTOR_TIDSLINJE
                    .intersection(segment.getLocalDateInterval())
                    .mapValue(grunnbeløpFaktor -> new SatsOgGrunnbeløpfaktor(segment.getValue().getSatsType(), grunnbeløpFaktor));
            })
            .reduce(LocalDateTimeline::crossJoin).orElse(LocalDateTimeline.empty());
    }
}
