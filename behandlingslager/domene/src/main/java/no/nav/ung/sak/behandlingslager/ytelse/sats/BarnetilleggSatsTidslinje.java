package no.nav.ung.sak.behandlingslager.ytelse.sats;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static no.nav.fpsak.tidsserie.LocalDateInterval.TIDENES_BEGYNNELSE;
import static no.nav.fpsak.tidsserie.LocalDateInterval.TIDENES_ENDE;

public class BarnetilleggSatsTidslinje {
    /**
     * Tidslinje for barnetilleggsatser
     */
    public static final LocalDateTimeline<BigDecimal> BARNETILLEGG_DAGSATS = new LocalDateTimeline<>(
        List.of(
            new LocalDateSegment<>(TIDENES_BEGYNNELSE, LocalDate.of(2024, 12, 31), BigDecimal.valueOf(36)),
            new LocalDateSegment<>(LocalDate.of(2025, 1, 1), TIDENES_ENDE, BigDecimal.valueOf(37))
        ));
}
