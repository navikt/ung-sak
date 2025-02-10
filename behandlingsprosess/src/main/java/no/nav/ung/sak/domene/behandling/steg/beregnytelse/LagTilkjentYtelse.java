package no.nav.ung.sak.domene.behandling.steg.beregnytelse;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.domene.behandling.steg.uttak.regler.RapportertInntekt;

import java.math.BigDecimal;
import java.util.Set;

public class LagTilkjentYtelse {

    static LocalDateTimeline<TilkjentYtelseVerdi> lagTidslinje(LocalDateTimeline<Boolean> godkjentTidslinje, LocalDateTimeline<BeregnetSats> totalsatsTidslinje, LocalDateTimeline<Set<RapportertInntekt>> rapportertInntektTidslinje) {
        return totalsatsTidslinje.combine(rapportertInntektTidslinje, (di, sats, rapportertInntekt) -> {
                // Dersom det ikke er rapportert inntekt settes denne til 0, ellers summeres alle inntektene
                final var rapporertinntekt = rapportertInntekt == null ? BigDecimal.ZERO : rapportertInntekt.getValue().stream().map(RapportertInntekt::beløp).reduce(BigDecimal.ZERO, BigDecimal::add);
                // Mapper verdier til TilkjentYtelseVerdi
                final var tilkjentYtelseVerdi = TikjentYtelseBeregner.beregn(di, sats.getValue(), rapporertinntekt);
                return new LocalDateSegment<>(di.getFomDato(), di.getTomDato(), tilkjentYtelseVerdi);
            }, LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .intersection(godkjentTidslinje);
    }

}
