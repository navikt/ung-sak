package no.nav.ung.sak.web.app.ungdomsytelse;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.kontrakt.ungdomsytelse.ytelse.UtbetalingStatus;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class UtbetalingstatusUtleder {


    /** Finner utbetalingsstatus for periodene med ytelse på behandlingen. Utledning gjøres ved å sammenligne tidslinjer for behandlinger tilbake til førstegangsbehandlingen.
     *
     * @param aktuellBehandlingAvsluttetTidspunkt Avsluttet tidspunkt for aktuell behandling som skal brukes for å finne utbetalingsstatus.
     * @param tidslinjeMap Map fra avsluttet tidspunkt for behandling til tidslinje med tilkjent ytelse verdier.
     * @param dagensDato
     * @return
     */
    static LocalDateTimeline<UtbetalingStatus> finnUtbetalingsstatusTidslinje(BehandlingAvsluttetTidspunkt aktuellBehandlingAvsluttetTidspunkt,
                                                                              Map<BehandlingAvsluttetTidspunkt, LocalDateTimeline<TilkjentYtelseVerdi>> tidslinjeMap, LocalDate dagensDato) {

        var kronologisk = sorterKronologisk(tidslinjeMap);
        LocalDateTimeline<TilkjentYtelseVerdi> gjeldendeTilkjentYtelse = LocalDateTimeline.empty();
        LocalDateTimeline<UtbetalingStatus> gjeldendeStatus = LocalDateTimeline.empty();

        var iterator = kronologisk.iterator();
        var gjeldendeEntry = iterator.hasNext() ? iterator.next() : null;

        while (gjeldendeEntry != null && gjeldendeEntry.getKey().compareTo(aktuellBehandlingAvsluttetTidspunkt) <= 0) {
            var erEndretTidslinje = finnEndretFraForrigeTidslinje(gjeldendeTilkjentYtelse, gjeldendeEntry.getValue());
            // Bruker forrige utelede status for uendret ytelse
            final var forrigeStatus = gjeldendeStatus;
            var statusForUendretYtelse = erEndretTidslinje.filterValue(Boolean.FALSE::equals).map(it -> forrigeStatus.intersection(it.getLocalDateInterval()).toSegments().stream().toList());
            var statusForEndretYtelse = finnUtbetalingstatusForEndring(dagensDato, erEndretTidslinje.filterValue(Boolean.TRUE::equals), gjeldendeEntry.getKey());
            var statusForBehandling = statusForEndretYtelse.crossJoin(statusForUendretYtelse);

            gjeldendeStatus = gjeldendeStatus.crossJoin(statusForBehandling, StandardCombinators::coalesceRightHandSide);
            gjeldendeTilkjentYtelse = gjeldendeEntry.getValue();
            gjeldendeEntry = iterator.hasNext() ? iterator.next() : null;
        }
        return gjeldendeStatus;
    }

    private static List<Map.Entry<BehandlingAvsluttetTidspunkt, LocalDateTimeline<TilkjentYtelseVerdi>>> sorterKronologisk(Map<BehandlingAvsluttetTidspunkt, LocalDateTimeline<TilkjentYtelseVerdi>> tidslinjeMap) {
        var sortert = tidslinjeMap.entrySet().stream()
            .sorted(Comparator.comparing(e -> e.getKey().avsluttetTid(), Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();
        return sortert;
    }

    private static LocalDateTimeline<UtbetalingStatus> finnUtbetalingstatusForEndring(LocalDate dagensDato, LocalDateTimeline<Boolean> endretTidslinje, BehandlingAvsluttetTidspunkt behandlingAvsluttetTidspunkt) {
        var sisteUtbetalteDag = finnSisteMuligeUtbetalteDag(dagensDato, behandlingAvsluttetTidspunkt);
        var statusForEndretYtelse = endretTidslinje.map(it -> it.getTom().isAfter(sisteUtbetalteDag) ?
            List.of(new LocalDateSegment<>(it.getFom(), it.getTom(), UtbetalingStatus.TIL_UTBETALING)) :
            List.of(new LocalDateSegment<>(it.getFom(), it.getTom(), UtbetalingStatus.UTBETALT)));
        return statusForEndretYtelse;
    }

    private static LocalDate finnSisteMuligeUtbetalteDag(LocalDate dagensDato, BehandlingAvsluttetTidspunkt behandlingAvsluttetTidspunkt) {
        var førsteVirkedagINåværendeMåned = finnFørsteVirkedagIMåned(YearMonth.from(dagensDato));


        var sisteMuligeDatoForOversendingTilOS = finnDagForOversendingAvForrigeMåned(behandlingAvsluttetTidspunkt, førsteVirkedagINåværendeMåned, dagensDato);

        var sisteMuligeDagForUtbetalingTilBruker = sisteMuligeDatoForOversendingTilOS.plusDays(3);
        var sisteUtbetalteDag = !dagensDato.isBefore(sisteMuligeDagForUtbetalingTilBruker) ?
            dagensDato.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth()) : dagensDato.minusMonths(2).with(TemporalAdjusters.lastDayOfMonth());
        return sisteUtbetalteDag;
    }

    private static LocalDate finnDagForOversendingAvForrigeMåned(BehandlingAvsluttetTidspunkt behandlingAvsluttetTidspunkt, LocalDate førsteVirkedagINåværendeMåned, LocalDate dagensDato) {
        if (!behandlingAvsluttetTidspunkt.erAvsluttet()) {
            // Hvis avsluttetDato er null, så betyr det at behandlingen ikke er avsluttet og vi antar at oversendingen skjer i dag
            return dagensDato;
        }
        return behandlingAvsluttetTidspunkt.avsluttetTid().toLocalDate().isAfter(førsteVirkedagINåværendeMåned) ? behandlingAvsluttetTidspunkt.getAvsluttetTid().toLocalDate() : førsteVirkedagINåværendeMåned;
    }

    private static LocalDateTimeline<Boolean> finnEndretFraForrigeTidslinje(LocalDateTimeline<TilkjentYtelseVerdi> gjeldendeTilkjentYtelse, LocalDateTimeline<TilkjentYtelseVerdi> tilkjentYtelseTidslinje) {
        return tilkjentYtelseTidslinje.combine(gjeldendeTilkjentYtelse, (di, lhs, rhs) -> {
            if (rhs == null) {
                return new LocalDateSegment<>(di, true);
            }
            if (!lhs.getValue().equals(rhs.getValue())) {
                return new LocalDateSegment<>(di, true);
            }
            return new LocalDateSegment<>(di, false);
        }, LocalDateTimeline.JoinStyle.LEFT_JOIN);
    }


    private static LocalDate finnFørsteVirkedagIMåned(YearMonth yearMonth) {
        // Antar her alltid at oppdragssystemet er oppe den første virkedagen i måneden. Dette vil som regel stemme, men ikke ved spesielle høytidsdager. Ok antagelse å gjøre for bruk til visning
        var dato = yearMonth.atDay(1);
        while (dato.getDayOfWeek().equals(DayOfWeek.SUNDAY) || dato.getDayOfWeek().equals(DayOfWeek.SATURDAY)) {
            // Hvis første dag i måneden er helg, så flytt dato til mandag
            dato = dato.plusDays(1);
        }
        return dato;
    }

}
