package no.nav.k9.sak.ytelse.ung.beregning;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

public class LagGrunnbeløpFaktorTidslinje {

    static LocalDateTimeline<BigDecimal> lagGrunnbeløpFaktorTidslinje(LocalDate fødselsdato) {
        var datoForEndringAvSats = fødselsdato.plusYears(25).with(TemporalAdjusters.lastDayOfMonth()).plusDays(1);
        return new LocalDateTimeline<>(
            List.of(
                new LocalDateSegment<>(
                    fødselsdato.plusYears(18).with(TemporalAdjusters.lastDayOfMonth()).plusDays(1),
                    datoForEndringAvSats.minusDays(1),
                    finnGrunnbeløpFaktorUnderTjuefem()),
                new LocalDateSegment<>(
                    datoForEndringAvSats,
                    fødselsdato.plusYears(29).with(TemporalAdjusters.lastDayOfMonth()).plusDays(1),
                    finnGrunnbeløpFaktorOverTjuefem())

            ));
    }

    private static BigDecimal finnGrunnbeløpFaktorOverTjuefem() {
        return BigDecimal.valueOf(2);
    }

    private static BigDecimal finnGrunnbeløpFaktorUnderTjuefem() {
        return BigDecimal.valueOf(4).divide(BigDecimal.valueOf(3), 5, BigDecimal.ROUND_HALF_UP);
    }


}
