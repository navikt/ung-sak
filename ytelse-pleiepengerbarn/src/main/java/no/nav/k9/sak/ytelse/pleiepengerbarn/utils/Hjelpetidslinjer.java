package no.nav.k9.sak.ytelse.pleiepengerbarn.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public final class Hjelpetidslinjer {

    private Hjelpetidslinjer() {
    }

    public static <T> LocalDateTimeline<T> fjernHelger(LocalDateTimeline<T> tidslinje){
        LocalDateTimeline<Boolean> helger = lagTidslinjeMedKunHelger(tidslinje);
        return tidslinje.disjoint(helger);
    }

    /**
     * Lager en tidslinje der man kun har med helger ut fra tidslinjen som er oppgitt.
     *
     * @param tidslinje Tidslinjen som brukes for å hente helger for.
     * @return En tidslinje som av lørdager og søndager fra opprinnelig tidsserie, og ingenting annet
     */
    public static LocalDateTimeline<Boolean> lagTidslinjeMedKunHelger(LocalDateTimeline<?> tidslinje) {
        List<LocalDateSegment<Boolean>> helger = new ArrayList<>();
        for (LocalDateInterval intervall : tidslinje.getLocalDateIntervals()) {
            LocalDate d = intervall.getFomDato();
            while (!d.isAfter(intervall.getTomDato())) {
                if (d.getDayOfWeek() == DayOfWeek.SUNDAY) {
                    helger.add(new LocalDateSegment<>(d, d, true));
                    d = d.plusDays(6);
                } else if (d.getDayOfWeek() == DayOfWeek.SATURDAY) {
                    helger.add(new LocalDateSegment<>(d, d.isBefore(intervall.getTomDato()) ? d.plusDays(1) : d, true));
                    d = d.plusWeeks(1);
                } else {
                    d = d.plusDays(DayOfWeek.SATURDAY.getValue() - d.getDayOfWeek().getValue());
                }
            }
        }
        return new LocalDateTimeline<>(helger).compress();
    }


    /**
     * Lager en ukestidslinje for mandag-fredag for oppgitt intervall.
     *
     * @param fom Fra-og-med-datoen man skal generere ukestidslinje for.
     * @param tom Til-og-med-datoen man skal generere ukestidslinje for.
     * @return En tidslinje med et segment per uke. Hvert segment har maksimumsperioden
     * mandag til fredag -- og kan være kortere i hver ende hvis ikke
     * {@code fom} er en mandag og/eller {@code tom} er en fredag.
     */
    public static LocalDateTimeline<Boolean> lagUkestidslinjeForMandagTilFredag(LocalDate fom, LocalDate tom) {
        Objects.requireNonNull(fom, "fom");
        Objects.requireNonNull(tom, "tom");
        if (fom.isAfter(tom)) {
            throw new IllegalArgumentException("fom kan ikke være etter tom.");
        }

        final LocalDateTimeline<Boolean> omsluttende = new LocalDateTimeline<>(fom, tom, true);
        return fjernHelger(omsluttende);
    }

    public static <T> LocalDateTimeline<T> utledHullSomMåTettes(LocalDateTimeline<T> tidslinjen, KantIKantVurderer kantIKantVurderer) {
        var segmenter = tidslinjen.compress().toSegments();

        LocalDateSegment<T> periode = null;
        var resultat = new ArrayList<LocalDateSegment<T>>();

        for (LocalDateSegment<T> segment : segmenter) {
            if (periode != null) {
                var til = DatoIntervallEntitet.fra(segment.getLocalDateInterval());
                var fra = DatoIntervallEntitet.fra(periode.getLocalDateInterval());
                if (kantIKantVurderer.erKantIKant(til, fra) && !fra.grenserTil(til)) {
                    resultat.add(new LocalDateSegment<>(periode.getTom().plusDays(1), segment.getFom().minusDays(1), periode.getValue()));
                }
            }
            periode = segment;
        }

        return new LocalDateTimeline<>(resultat);
    }
}
