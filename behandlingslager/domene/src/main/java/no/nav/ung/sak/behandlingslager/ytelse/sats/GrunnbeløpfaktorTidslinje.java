package no.nav.ung.sak.behandlingslager.ytelse.sats;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

public class GrunnbeløpfaktorTidslinje {

    private static final LocalDateTimeline<BigDecimal> HØY_GRUNNBELØPFAKTOR_TIDSLINJE = new LocalDateTimeline<>(
        List.of(
            new LocalDateSegment<>(LocalDate.of(2024, 1, 1), LocalDate.of(2099, 12, 31), BigDecimal.valueOf(2.041))
        )
    );

    private static final LocalDateTimeline<BigDecimal> LAV_GRUNNBELØPFAKTOR_TIDSLINJE = HØY_GRUNNBELØPFAKTOR_TIDSLINJE.mapValue(it -> it.multiply(BigDecimal.valueOf(2).divide(BigDecimal.valueOf(3), 10, RoundingMode.HALF_UP)).setScale(5, RoundingMode.HALF_UP));

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

    public static BigDecimal finnStandardGrunnbeløpFaktorFor(LocalDateInterval periode){
        var gFaktorer = HØY_GRUNNBELØPFAKTOR_TIDSLINJE.toSegments().stream().filter(it -> periode.overlaps(it.getLocalDateInterval())).toList();
        if (gFaktorer.size() > 1) {
            throw new IllegalStateException("Kan ikke ha flere enn 1 grunnbeløpfaktor for samme periode: " + periode + ", fant: " + gFaktorer);
        }

        return gFaktorer.getFirst().getValue();
    }
}
