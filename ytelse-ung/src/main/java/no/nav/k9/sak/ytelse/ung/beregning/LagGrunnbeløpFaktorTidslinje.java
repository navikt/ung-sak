package no.nav.k9.sak.ytelse.ung.beregning;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.util.Tuple;

public class LagGrunnbeløpFaktorTidslinje {

    private Map<Tuple<Period, Period>, BigDecimal> satser = Map.of(
        new Tuple<>(Period.ofYears(18), Period.ofYears(25)), new BigDecimal("1.3333333"),
        new Tuple<>(Period.ofYears(25), Period.ofYears(30)), new BigDecimal("2"));


    static LocalDateTimeline<BigDecimal> lagGrunnbelpFaktorTidslinje(LocalDate fødselsdato) {
        var datoForEndringAvSats = fødselsdato.plusYears(25);
        return new LocalDateTimeline<>(
            List.of(
                new LocalDateSegment<>(
                    fødselsdato.plusYears(18),
                    datoForEndringAvSats.minusDays(1),
                    finnGrunnbeløpFaktorUnderTjuefem()),
                new LocalDateSegment<>(
                    datoForEndringAvSats,
                    fødselsdato.plusYears(29),
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
