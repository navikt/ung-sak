package no.nav.ung.sak.grunnbeløp;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GrunnbeløpTidslinje {
    /**
     * Tidslinje for grunnbeløpsatser
     */
    private static final LocalDateTimeline<Grunnbeløp> GRUNNBELØP_TIDSLINJE = new LocalDateTimeline<>(
        List.of(
            new LocalDateSegment<>(LocalDate.of(2025, 5, 1), LocalDate.of(2099, 12, 31), new Grunnbeløp(BigDecimal.valueOf(130160))),
            new LocalDateSegment<>(LocalDate.of(2024, 5, 1), LocalDate.of(2025, 4, 30), new Grunnbeløp(BigDecimal.valueOf(124028))),
            new LocalDateSegment<>(LocalDate.of(2023, 5, 1), LocalDate.of(2024, 4,30), new Grunnbeløp(BigDecimal.valueOf(118620))),
            new LocalDateSegment<>(LocalDate.of(2022, 5, 1), LocalDate.of(2023, 4,30), new Grunnbeløp(BigDecimal.valueOf(111477)))
        ));


    public static LocalDateTimeline<Grunnbeløp> hentTidslinje() {
        return GRUNNBELØP_TIDSLINJE;
    }

    /**
     * Returnerer en tidslinje med gjennomsnittlige grunnbeløp per år, vektet med antall måneder satsene gjelder for.
     * Forutsetter at det det siste grunnbeløpet strekkes til årsskiftet for å slippe å telle totalt antall måneder i året
     *
     * @return Tidslinje med ett segment per år med vektet gjennomsnitt
     */
    public static LocalDateTimeline<Grunnbeløp> hentGrunnbeløpSnittTidslinje() {
        final BigDecimal antallMånederIÅret = BigDecimal.valueOf(12);

        LocalDate startDato = GRUNNBELØP_TIDSLINJE.getMinLocalDate().withDayOfYear(1);
        LocalDate sluttDato = Year.now().atMonth(12).atEndOfMonth();
        LocalDateTimeline<Grunnbeløp> splittetTidslinje = GRUNNBELØP_TIDSLINJE.splitAtRegular(startDato, sluttDato, java.time.Period.ofYears(1));

        Map<Year, List<LocalDateSegment<Grunnbeløp>>> segmenterPerÅr = splittetTidslinje.stream()
            .collect(Collectors.groupingBy(segment -> Year.of(segment.getFom().getYear())));

        List<LocalDateSegment<Grunnbeløp>> gsnittSegmenter = segmenterPerÅr.entrySet().stream()
            .map(entry -> {
                BigDecimal sumVektetVerdi = entry.getValue().stream().map(segment -> {
                    long antallMåneder = ChronoUnit.MONTHS.between(segment.getFom(), segment.getTom().plusDays(1));
                    return segment.getValue().verdi().multiply(BigDecimal.valueOf(antallMåneder));
                }).reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal gjennomsnitt = sumVektetVerdi.divide(antallMånederIÅret, 0, RoundingMode.HALF_EVEN);

                Year år = entry.getKey();
                return new LocalDateSegment<>(år.atDay(1), år.atDay(år.length()), new Grunnbeløp(gjennomsnitt));
            })
            .toList();

        return new LocalDateTimeline<>(gsnittSegmenter);
    }

    public static LocalDateTimeline<BigDecimal> lagOppjusteringsfaktorTidslinje(Year sisteÅr, int antallÅrTilbake) {
        var avgrensningsTimeline = new LocalDateInterval(
            sisteÅr.minusYears(antallÅrTilbake).atDay(1),
            sisteÅr.atMonth(12).atEndOfMonth()
        );

        var gsnittTidslinje = hentGrunnbeløpSnittTidslinje().intersection(avgrensningsTimeline);
        var gsnittForEtterspurtÅr = gsnittTidslinje.toSegments().last().getValue();

        return gsnittTidslinje.mapValue(it -> gsnittForEtterspurtÅr.verdi().divide(it.verdi(), 10, RoundingMode.HALF_EVEN));
    }
}
