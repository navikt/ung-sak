package no.nav.ung.sak.ytelse.ung.beregning;

import static no.nav.ung.sak.ytelse.ung.beregning.Sats.HØY;
import static no.nav.ung.sak.ytelse.ung.beregning.Sats.LAV;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

public class LagGrunnbeløpFaktorTidslinje {

    static LocalDateTimeline<Sats> lagGrunnbeløpFaktorTidslinje(LocalDate fødselsdato) {
        var datoForEndringAvSats = fødselsdato.plusYears(25).with(TemporalAdjusters.lastDayOfMonth()).plusDays(1);
        return new LocalDateTimeline<>(
            List.of(
                new LocalDateSegment<>(
                    fødselsdato.plusYears(18).with(TemporalAdjusters.lastDayOfMonth()).plusDays(1),
                    datoForEndringAvSats.minusDays(1),
                    LAV),
                new LocalDateSegment<>(
                    datoForEndringAvSats,
                    fødselsdato.plusYears(29).with(TemporalAdjusters.lastDayOfMonth()).plusDays(1),
                    HØY)

            ));
    }


}
