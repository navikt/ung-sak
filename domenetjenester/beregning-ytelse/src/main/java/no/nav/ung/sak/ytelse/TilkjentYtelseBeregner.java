/**
 * `TikjentYtelseBeregner` er en hjelpeklasse som brukes til å beregne verdier for tilkjent ytelse basert på
 * en gitt periode, sats og rapportert inntekt.
 */
package no.nav.ung.sak.ytelse;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollerteInntekter;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.domene.typer.tid.Virkedager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;


public class TilkjentYtelseBeregner {


    /**
     * Beregner verdier for tilkjent ytelse basert på en gitt periode, sats og rapportert inntekt.
     *
     * @param periode perioden som ytelsen gjelder for
     * @param sats den totale satsen for perioden
     * @param rapporertinntekt den rapporterte inntekten for perioden
     * @return en instans av `TilkjentYtelseVerdi` som representerer de beregnede verdiene
     */
    public static TilkjentYtelsePeriodeResultat beregn(LocalDateInterval periode, BeregnetSats sats, KontrollerteInntekter rapportertInntekt) {
        Objects.requireNonNull(periode, "periode");
        Objects.requireNonNull(sats, "sats");
        Objects.requireNonNull(rapportertInntekt, "rapportertInntekt");

        var rapportertInntektReduserer = new RapportertInntektReduserer(rapportertInntekt, periode);

        if (!toYearMonth(periode.getFomDato()).equals(toYearMonth(periode.getTomDato()))) {
            throw new IllegalArgumentException("Periode må være innenfor samme måned");
        }
        final var sporing = new HashMap<String, String>();

        final var uredusertBeløp = sats.totalSats().setScale(10, RoundingMode.HALF_UP);
        sporing.put("totalSats", sats.totalSats().toString());

        final var reduksjon = rapportertInntektReduserer.beregnReduksjon(sporing);
        sporing.put("reduksjon", reduksjon.toString());

        final var redusertBeløp = uredusertBeløp.subtract(reduksjon).max(BigDecimal.ZERO);
        sporing.put("redusertBeløp", redusertBeløp.toString());

        final var antallVirkedager = rapportertInntektReduserer.antallVirkedager();
        final var avrundetDagsats = antallVirkedager.equals(BigDecimal.ZERO) ? BigDecimal.ZERO : redusertBeløp.divide(antallVirkedager, 0, RoundingMode.HALF_UP);
        BigDecimal tilkjentBeløp = avrundetDagsats.multiply(antallVirkedager);
        sporing.put("dagsats", avrundetDagsats.toString());
        sporing.put("tilkjentBeløp", tilkjentBeløp.toString());

        BigDecimal avrundetGrunnsatsMedBarnetillegg = antallVirkedager.equals(BigDecimal.ZERO) ? BigDecimal.ZERO : sats.totalSats().divide(antallVirkedager, 0, RoundingMode.HALF_UP).multiply(antallVirkedager);
        sporing.put("avrundetGrunnsatsMedBarnetillegg", avrundetGrunnsatsMedBarnetillegg.toString());

        final var utbetalingsgrad = finnUtbetalingsgrad(tilkjentBeløp, avrundetGrunnsatsMedBarnetillegg);
        sporing.put("utbetalingsgrad", String.valueOf(utbetalingsgrad));

        sporing.put("periode", periode.toString());
        final var tilkjentYtelseVerdi = new TilkjentYtelseVerdi(
            uredusertBeløp,
            reduksjon,
            redusertBeløp,
            avrundetDagsats,
            utbetalingsgrad,
            tilkjentBeløp);
        return new TilkjentYtelsePeriodeResultat(tilkjentYtelseVerdi, sporing);
    }

    private static YearMonth toYearMonth(LocalDate dato) {
        return YearMonth.of(dato.getYear(), dato.getMonth());
    }


    private static BigDecimal finnUtbetalingsgrad(BigDecimal tilkjentBeløp, BigDecimal avrundetGrunnsatsMedBarnetillegg) {
        if (avrundetGrunnsatsMedBarnetillegg.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return tilkjentBeløp.multiply(BigDecimal.valueOf(100)).divide(avrundetGrunnsatsMedBarnetillegg, 10, RoundingMode.HALF_UP);
    }

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
        return delTidslinje.stream().reduce(BeregnetSats.ZERO, TilkjentYtelseBeregner::reduserSegmenterIDelTidslinje, BeregnetSats::adder);
    }

    private static BeregnetSats reduserSegmenterIDelTidslinje(BeregnetSats beregnetSats, LocalDateSegment<UngdomsytelseSatser> s2) {
        final var antallVirkedager = Virkedager.beregnAntallVirkedager(s2.getFom(), s2.getTom());
        final var bergnetForSegment = new BeregnetSats(s2.getValue().dagsats(), s2.getValue().dagsatsBarnetillegg()).multipliser(antallVirkedager);
        return beregnetSats.adder(bergnetForSegment);
    }
}
