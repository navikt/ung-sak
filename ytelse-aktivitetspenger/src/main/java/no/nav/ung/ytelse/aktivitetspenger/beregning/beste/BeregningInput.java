package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.typer.Beløp;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;

public record BeregningInput(
    Beløp pgi1,
    Beløp pgi2,
    Beløp pgi3,
    LocalDate virkningsdato,
    Year sisteLignedeÅr // Året for pgi3
) {

    public LocalDateTimeline<Beløp> lagTidslinje() {

        return new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(årTilIntervall(sisteLignedeÅr), pgi3),
            new LocalDateSegment<>(årTilIntervall(sisteLignedeÅr.minusYears(1)), pgi2),
            new LocalDateSegment<>(årTilIntervall(sisteLignedeÅr.minusYears(2)), pgi1)
        ));
    }

    private LocalDateInterval årTilIntervall(Year år) {
        return new LocalDateInterval(år.atDay(1), år.atMonth(12).atEndOfMonth());
    }
}
