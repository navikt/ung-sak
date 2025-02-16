package no.nav.ung.sak.domene.behandling.steg.beregnytelse;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;

import java.math.BigDecimal;
import java.util.Set;

/**
 * `LagTilkjentYtelse` er en hjelpeklasse som brukes til å generere en tidslinje for tilkjent ytelse basert på godkjente perioder,
 * totale satser og rapportert inntekt.
 */
public class LagTilkjentYtelse {

    static LocalDateTimeline<TilkjentYtelsePeriodeResultat> lagTidslinje(LocalDateTimeline<Boolean> godkjentTidslinje, LocalDateTimeline<BeregnetSats> totalsatsTidslinje, LocalDateTimeline<RapporterteInntekter> rapportertInntektTidslinje) {
        return totalsatsTidslinje.combine(rapportertInntektTidslinje, (di, sats, rapportertInntekt) -> {
                // Dersom det ikke er rapportert inntekt settes denne til 0, ellers summeres alle inntektene
                final var rapporertinntekt = rapportertInntekt == null ? BigDecimal.ZERO : rapportertInntekt.getValue().getRapporterteInntekter().stream().map(RapportertInntekt::beløp).reduce(BigDecimal.ZERO, BigDecimal::add);
                // Mapper verdier til TilkjentYtelsePeriodeResultat
                final var periodeResultat = TikjentYtelseBeregner.beregn(di, sats.getValue(), rapporertinntekt);
                return new LocalDateSegment<>(di.getFomDato(), di.getTomDato(), periodeResultat);
            }, LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .intersection(godkjentTidslinje);
    }

}
