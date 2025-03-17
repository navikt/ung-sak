package no.nav.ung.sak.domene.behandling.steg.beregnytelse;

import java.math.BigDecimal;
import java.time.temporal.TemporalAdjusters;
import java.util.Set;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.ytelse.BeregnetSats;
import no.nav.ung.sak.ytelse.RapportertInntekt;
import no.nav.ung.sak.ytelse.RapporterteInntekter;
import no.nav.ung.sak.ytelse.TikjentYtelseBeregner;
import no.nav.ung.sak.ytelse.TilkjentYtelsePeriodeResultat;

/**
 * `LagTilkjentYtelse` er en hjelpeklasse som brukes til å generere en tidslinje for tilkjent ytelse basert på godkjente perioder,
 * totale satser og perioder med kontrollert inntekt.
 * <p>
 * Det er i utgangspukntet kun perioder der det utført kontroll av rapportert inntekt som vil bli med i tilkjent ytelse. Unntaket er første og siste periode. I disse periodene utføres det ingen kontroll og vi sender disse til oppdrag med en gang.
 */
public class LagTilkjentYtelse {

    static LocalDateTimeline<TilkjentYtelsePeriodeResultat> lagTidslinje(LocalDateTimeline<Boolean> godkjentTidslinje, LocalDateTimeline<BeregnetSats> totalsatsTidslinje, LocalDateTimeline<Set<RapportertInntekt>> rapportertInntektTidslinje) {
        if (godkjentTidslinje.isEmpty()) {
            return LocalDateTimeline.empty();
        }

        // Begrenser tilkjent ytelse til periode med kontrollert inntekt eller første/siste periode
        var tidslinjeSomSkalHaTilkjentYtelse = rapportertInntektTidslinje.intersection(godkjentTidslinje).mapValue(it -> true);
        final var førstePeriode = new LocalDateTimeline<>(godkjentTidslinje.getMinLocalDate(), godkjentTidslinje.getMinLocalDate().with(TemporalAdjusters.lastDayOfMonth()), true).intersection(godkjentTidslinje);
        tidslinjeSomSkalHaTilkjentYtelse = tidslinjeSomSkalHaTilkjentYtelse.crossJoin(førstePeriode);
        final var sistePeriode = new LocalDateTimeline<>(godkjentTidslinje.getMaxLocalDate().withDayOfMonth(1), godkjentTidslinje.getMaxLocalDate(), true).intersection(godkjentTidslinje);
        tidslinjeSomSkalHaTilkjentYtelse = tidslinjeSomSkalHaTilkjentYtelse.crossJoin(sistePeriode);


        return totalsatsTidslinje.combine(rapportertInntektTidslinje, (di, sats, rapportertInntekt) -> {
                // Dersom det ikke er rapportert inntekt settes denne til 0, ellers summeres alle inntektene
                final var rapporertinntekt = rapportertInntekt == null ? BigDecimal.ZERO : rapportertInntekt.getValue().stream().map(RapportertInntekt::beløp).reduce(BigDecimal.ZERO, BigDecimal::add);
                // Mapper verdier til TilkjentYtelsePeriodeResultat
                final var periodeResultat = TikjentYtelseBeregner.beregn(di, sats.getValue(), rapporertinntekt);
                return new LocalDateSegment<>(di.getFomDato(), di.getTomDato(), periodeResultat);
            }, LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .intersection(tidslinjeSomSkalHaTilkjentYtelse);
    }

}
