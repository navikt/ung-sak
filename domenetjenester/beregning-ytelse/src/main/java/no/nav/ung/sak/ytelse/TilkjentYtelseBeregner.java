/**
 * `TikjentYtelseBeregner` er en hjelpeklasse som brukes til å beregne verdier for tilkjent ytelse basert på
 * en gitt periode, sats og rapportert inntekt.
 */
package no.nav.ung.sak.ytelse;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.domene.typer.tid.Virkedager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;


public class TilkjentYtelseBeregner {


    public static final BigDecimal REDUKSJONS_FAKTOR = BigDecimal.valueOf(0.66);

    /**
     * Beregner verdier for tilkjent ytelse basert på en gitt periode, sats og rapportert inntekt.
     *
     * @param periode perioden som ytelsen gjelder for
     * @param sats den totale satsen for perioden
     * @param rapporertinntekt den rapporterte inntekten for perioden
     * @return en instans av `TilkjentYtelseVerdi` som representerer de beregnede verdiene
     */
    public static TilkjentYtelsePeriodeResultat beregn(LocalDateInterval periode, BeregnetSats sats, BigDecimal rapporertinntekt) {
        Objects.requireNonNull(periode, "periode");
        Objects.requireNonNull(sats, "sats");
        Objects.requireNonNull(rapporertinntekt, "rapporertinntekt");
        final var sporing = new HashMap<String, String>();
        // Uredusert beløp bergnes fra totalsats
        final var uredusertBeløp = sats.totalSats().setScale(10, RoundingMode.HALF_UP);
        sporing.put("totalSats", sats.totalSats().toString());
        // Reduserer beløp med rapportert inntekt
        final var reduksjon = rapporertinntekt.multiply(REDUKSJONS_FAKTOR);
        sporing.put("rapportertInntekt", rapporertinntekt.toString());
        sporing.put("reduksjonsfaktor", REDUKSJONS_FAKTOR.toString());
        sporing.put("reduksjon", reduksjon.toString());

        final var redusertBeløp = uredusertBeløp.subtract(reduksjon).max(BigDecimal.ZERO);
        sporing.put("redusertBeløp", redusertBeløp.toString());

        // Beregner dagsats utifra antall virkedager i perioden
        final var antallVirkedager = Virkedager.beregnAntallVirkedager(periode.getFomDato(), periode.getTomDato());
        sporing.put("antallVirkedager", String.valueOf(antallVirkedager));
        final var dagsats = antallVirkedager == 0 ?  BigDecimal.ZERO : redusertBeløp.divide(BigDecimal.valueOf(antallVirkedager), 0, RoundingMode.HALF_UP);
        sporing.put("dagsats", dagsats.toString());

        // Beregner utbetalingsgrad
        final var utbetalingsgrad = finnUtbetalingsgrad(redusertBeløp, sats.grunnsats(), dagsats);
        sporing.put("utbetalingsgrad", String.valueOf(utbetalingsgrad));

        sporing.put("periode", periode.toString());
        final var tilkjentYtelseVerdi = new TilkjentYtelseVerdi(
            uredusertBeløp,
            reduksjon,
            redusertBeløp,
            dagsats,
            utbetalingsgrad);
        return new TilkjentYtelsePeriodeResultat(tilkjentYtelseVerdi, sporing);
    }


    /**
     * Beregner utbetalingsgrad basert på redusert beløp, grunnsats og dagsats.
     *
     * @param redusertBeløp det reduserte beløpet etter fratrekk av rapportert inntekt
     * @param grunnsats grunnsatsen for ytelsen
     * @param dagsats dagsatsen beregnet utifra antall virkedager
     * @return utbetalingsgraden som en prosentverdi
     */
    private static int finnUtbetalingsgrad(BigDecimal redusertBeløp, BigDecimal grunnsats, BigDecimal dagsats) {
        // Utbetalingsgrad regnes utifra grunnsats, barnetillegg er ikke inkludert

        // Dersom ingenting utbetales er utbetalingsgrad 0
        if (dagsats.compareTo(BigDecimal.ZERO) == 0 || redusertBeløp.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        // Dersom redusert beløp er høyere enn grunnsats sier vi at det er full utbetaling (selv om det faktisk kan vere en reduksjon)
        if (redusertBeløp.compareTo(grunnsats) > 0) {
            return 100;
        }
        // Regner ut prosentvis utbetalingsgrad
        return redusertBeløp.multiply(BigDecimal.valueOf(100)).divide(grunnsats, 0, BigDecimal.ROUND_HALF_UP).intValue();
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
