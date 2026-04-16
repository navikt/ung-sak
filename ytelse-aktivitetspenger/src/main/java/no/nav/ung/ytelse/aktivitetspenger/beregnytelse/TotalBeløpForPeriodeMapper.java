package no.nav.ung.ytelse.aktivitetspenger.beregnytelse;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.domene.typer.tid.Virkedager;
import no.nav.ung.sak.ytelse.BeregnetSats;
import no.nav.ung.ytelse.aktivitetspenger.beregning.minstesats.AktivitetspengerSatsGrunnlag;

import java.time.YearMonth;
import java.util.List;
import java.util.function.Function;

public class TotalBeløpForPeriodeMapper {


    public static <V> LocalDateTimeline<BeregnetSats> mapSatserTilTotalbeløpForPerioder(
        LocalDateTimeline<AktivitetspengerSatsGrunnlag> satsTidslinje,
        LocalDateTimeline<YearMonth> ytelseTidslinje) {
        final var mappetTidslinje = ytelseTidslinje.map(mapTotaltSatsbeløpForSegment(satsTidslinje));
        return mappetTidslinje;
    }

    private static <V> Function<LocalDateSegment<V>, List<LocalDateSegment<BeregnetSats>>> mapTotaltSatsbeløpForSegment(LocalDateTimeline<AktivitetspengerSatsGrunnlag> satsTidslinje) {
        return (inntektSegment) -> {
            var delTidslinje = satsTidslinje.intersection(inntektSegment.getLocalDateInterval());
            final BeregnetSats totatSatsbeløpForPeriode = reduser(delTidslinje);
            return List.of(new LocalDateSegment<>(inntektSegment.getFom(), inntektSegment.getTom(), totatSatsbeløpForPeriode));
        };
    }

    private static BeregnetSats reduser(LocalDateTimeline<AktivitetspengerSatsGrunnlag> delTidslinje) {
        return delTidslinje.stream().reduce(BeregnetSats.ZERO, TotalBeløpForPeriodeMapper::reduserSegmenterIDelTidslinje, BeregnetSats::adder);
    }

    private static BeregnetSats reduserSegmenterIDelTidslinje(BeregnetSats beregnetSats, LocalDateSegment<AktivitetspengerSatsGrunnlag> s2) {
        final var antallVirkedager = Virkedager.beregnAntallVirkedager(s2.getFom(), s2.getTom());
        final var bergnetForSegment = new BeregnetSats(s2.getValue().dagsats(), s2.getValue().dagsatsBarnetillegg()).multipliser(antallVirkedager);
        return beregnetSats.adder(bergnetForSegment);
    }

}
