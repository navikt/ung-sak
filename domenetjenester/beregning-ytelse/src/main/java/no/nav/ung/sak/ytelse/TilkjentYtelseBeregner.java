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
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;


public class TilkjentYtelseBeregner {

    public static TilkjentYtelsePeriodeResultat beregn(LocalDateInterval periode, BeregnetSats sats, ReduksjonBeregner reduksjonBeregner) {
        Objects.requireNonNull(periode, "periode");
        Objects.requireNonNull(sats, "sats");
        Objects.requireNonNull(reduksjonBeregner, "reduksjonBeregner");

        if (!toYearMonth(periode.getFomDato()).equals(toYearMonth(periode.getTomDato()))) {
            throw new IllegalArgumentException("Periode må være innenfor samme måned");
        }
        final var sporing = new HashMap<String, String>();

        final var uredusertBeløp = sats.totalSats().setScale(10, RoundingMode.HALF_UP);
        sporing.put("totalSats", sats.totalSats().toString());

        final var reduksjonResultat = reduksjonBeregner.beregnReduksjon();
        sporing.putAll(reduksjonResultat.sporing());

        final var reduksjon = reduksjonResultat.reduksjon();
        sporing.put("reduksjon", reduksjon.toString());

        final var redusertBeløp = uredusertBeløp.subtract(reduksjon).max(BigDecimal.ZERO);
        sporing.put("redusertBeløp", redusertBeløp.toString());

        final var antallVirkedager = reduksjonBeregner.antallVirkedager();
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

}
