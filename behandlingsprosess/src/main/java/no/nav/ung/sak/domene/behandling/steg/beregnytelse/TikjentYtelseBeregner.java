package no.nav.ung.sak.domene.behandling.steg.beregnytelse;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.domene.typer.tid.Virkedager;

import java.math.BigDecimal;
import java.util.Objects;

class TikjentYtelseBeregner {


    public static final BigDecimal REDUKSJONS_FAKTOR = BigDecimal.valueOf(0.66);

    static TilkjentYtelseVerdi beregn(LocalDateInterval di, BeregnetSats sats, BigDecimal rapporertinntekt) {
        Objects.requireNonNull(di, "di");
        Objects.requireNonNull(sats, "sats");
        Objects.requireNonNull(rapporertinntekt, "rapporertinntekt");
        // Uredusert beløp bergnes fra totalsats
        final var uredusertBeløp = sats.totalSats().setScale(10, BigDecimal.ROUND_HALF_UP);
        // Reduserer beløp med rapportert inntekt
        final var reduksjon = rapporertinntekt.multiply(REDUKSJONS_FAKTOR);
        final var redusertBeløp = uredusertBeløp.subtract(reduksjon).max(BigDecimal.ZERO);
        // Beregner dagsats utifra antall virkedager i perioden
        final var antallVirkedager = Virkedager.beregnAntallVirkedager(di.getFomDato(), di.getTomDato());
        final var dagsats = antallVirkedager == 0 ?  BigDecimal.ZERO : redusertBeløp.divide(BigDecimal.valueOf(antallVirkedager), 0, BigDecimal.ROUND_HALF_UP);

        // Beregner utbetalingsgrad
        final var utbetalingsgrad = finnUtbetalingsgrad(redusertBeløp, sats.grunnsats(), dagsats);

        final var tilkjentYtelseVerdi = new TilkjentYtelseVerdi(uredusertBeløp, reduksjon, redusertBeløp, dagsats, utbetalingsgrad);
        return tilkjentYtelseVerdi;
    }


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

}
