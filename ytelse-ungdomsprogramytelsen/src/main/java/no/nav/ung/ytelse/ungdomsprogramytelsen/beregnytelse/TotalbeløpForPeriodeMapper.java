package no.nav.ung.ytelse.ungdomsprogramytelsen.beregnytelse;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.domene.typer.tid.Virkedager;
import no.nav.ung.sak.ytelse.BeregnetSats;

import java.util.List;
import java.util.function.Function;

public class TotalbeløpForPeriodeMapper {


    public static <V> LocalDateTimeline<BeregnetSats> mapSatserTilTotalbeløpForPerioder(
        LocalDateTimeline<UngdomsytelseSatser> satsTidslinje,
        LocalDateTimeline<V> ytelseTidslinje) {
        final var mappetTidslinje = ytelseTidslinje.map(mapTotaltSatsbeløpForSegment(satsTidslinje));
        return mappetTidslinje;
    }

    private static <V> Function<LocalDateSegment<V>, List<LocalDateSegment<BeregnetSats>>> mapTotaltSatsbeløpForSegment(LocalDateTimeline<UngdomsytelseSatser> satsTidslinje) {
        return (inntektSegment) -> {
            var delTidslinje = satsTidslinje.intersection(inntektSegment.getLocalDateInterval());
            final BeregnetSats totatSatsbeløpForPeriode = reduser(delTidslinje);
            return List.of(new LocalDateSegment<>(inntektSegment.getFom(), inntektSegment.getTom(), totatSatsbeløpForPeriode));
        };
    }

    private static BeregnetSats reduser(LocalDateTimeline<UngdomsytelseSatser> delTidslinje) {
        return delTidslinje.stream().reduce(BeregnetSats.ZERO, TotalbeløpForPeriodeMapper::reduserSegmenterIDelTidslinje, BeregnetSats::adder);
    }

    private static BeregnetSats reduserSegmenterIDelTidslinje(BeregnetSats beregnetSats, LocalDateSegment<UngdomsytelseSatser> s2) {
        final var antallVirkedager = Virkedager.beregnAntallVirkedager(s2.getFom(), s2.getTom());
        final var bergnetForSegment = new BeregnetSats(s2.getValue().dagsats(), s2.getValue().dagsatsBarnetillegg()).multipliser(antallVirkedager);
        return beregnetSats.adder(bergnetForSegment);
    }

}
