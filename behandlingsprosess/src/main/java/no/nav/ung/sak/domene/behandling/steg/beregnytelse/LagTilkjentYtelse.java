package no.nav.ung.sak.domene.behandling.steg.beregnytelse;

import java.math.BigDecimal;
import java.util.Set;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.ytelse.*;
import no.nav.ung.sak.ytelse.TilkjentYtelsePeriodeResultat;
import org.jetbrains.annotations.NotNull;

/**
 * `LagTilkjentYtelse` er en hjelpeklasse som brukes til å generere en tidslinje for tilkjent ytelse basert på godkjente perioder,
 * totale satser og perioder med kontrollert inntekt.
 * <p>
 * Det er i utgangspukntet kun perioder der det utført kontroll av rapportert inntekt som vil bli med i tilkjent ytelse. Unntaket er første og siste periode. I disse periodene utføres det ingen kontroll og vi sender disse til oppdrag med en gang.
 */
public class LagTilkjentYtelse {

    static LocalDateTimeline<TilkjentYtelsePeriodeResultat> lagTidslinje(LocalDateTimeline<Boolean> ytelseTidslinje,
                                                                         LocalDateTimeline<Boolean> godkjentTidslinje,
                                                                         LocalDateTimeline<BeregnetSats> totalsatsTidslinje,
                                                                         LocalDateTimeline<Set<RapportertInntekt>> rapportertInntektTidslinje) {
        if (godkjentTidslinje.isEmpty()) {
            return LocalDateTimeline.empty();
        }

        final var ikkePåkrevdKontrollTidslinje = RelevanteKontrollperioderUtleder.finnPerioderDerKontrollIkkeErPåkrevd(ytelseTidslinje);


        final var førstePerioder = ikkePåkrevdKontrollTidslinje.filterValue(RelevanteKontrollperioderUtleder.FritattForKontroll::gjelderFørstePeriode).mapValue(it -> true);

        // Begrenser tilkjent ytelse til periode med kontrollert inntekt eller første/siste periode
        var tidslinjeSomSkalHaTilkjentYtelse = rapportertInntektTidslinje.intersection(godkjentTidslinje).mapValue(it -> true);
        tidslinjeSomSkalHaTilkjentYtelse = tidslinjeSomSkalHaTilkjentYtelse.crossJoin(førstePerioder);
        final var sistePerioderSomSkalUtbetales = finnSistePerioderSomSkalLeggesTil(ikkePåkrevdKontrollTidslinje, tidslinjeSomSkalHaTilkjentYtelse);
        tidslinjeSomSkalHaTilkjentYtelse = tidslinjeSomSkalHaTilkjentYtelse.crossJoin(sistePerioderSomSkalUtbetales);


        return totalsatsTidslinje.combine(rapportertInntektTidslinje, (di, sats, rapportertInntekt) -> {
                // Dersom det ikke er rapportert inntekt settes denne til 0, ellers summeres alle inntektene
                final var rapporertinntekt = rapportertInntekt == null ? BigDecimal.ZERO : rapportertInntekt.getValue().stream().map(RapportertInntekt::beløp).reduce(BigDecimal.ZERO, BigDecimal::add);
                // Mapper verdier til TilkjentYtelsePeriodeResultat
                final var periodeResultat = TikjentYtelseBeregner.beregn(di, sats.getValue(), rapporertinntekt);
                return new LocalDateSegment<>(di.getFomDato(), di.getTomDato(), periodeResultat);
            }, LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .intersection(tidslinjeSomSkalHaTilkjentYtelse);
    }

    /** Perioder som ikke slutter ved månedsslutt legges kun til dersom foregående periode er ferdig kontrollert/behandlet
     * @param ikkePåkrevdKontrollTidslinje Tidslinje der vi ikke trenger å kontrollere inntekt
     * @param tidslinjeSomSkalHaTilkjentYtelse Tidslinje der vi har utført kontroll eller skal utbetale fordi det er første periode
     * @return Tidslinje der vi skal utbetale for siste periode
     */
    private static LocalDateTimeline<Boolean> finnSistePerioderSomSkalLeggesTil(LocalDateTimeline<RelevanteKontrollperioderUtleder.FritattForKontroll> ikkePåkrevdKontrollTidslinje,
                                                                                LocalDateTimeline<Boolean> tidslinjeSomSkalHaTilkjentYtelse) {
        final var segmenter = ikkePåkrevdKontrollTidslinje.filterValue(RelevanteKontrollperioderUtleder.FritattForKontroll::gjelderSistePeriode)
            .stream()
            .filter(it -> !tidslinjeSomSkalHaTilkjentYtelse.intersection(dagenFør(it)).isEmpty())
            .toList();
        return new LocalDateTimeline<>(segmenter).mapValue(it -> true);
    }

    private static LocalDateInterval dagenFør(LocalDateSegment<RelevanteKontrollperioderUtleder.FritattForKontroll> it) {
        return new LocalDateInterval(it.getFom().minusDays(1), it.getFom().minusDays(1));
    }

}
