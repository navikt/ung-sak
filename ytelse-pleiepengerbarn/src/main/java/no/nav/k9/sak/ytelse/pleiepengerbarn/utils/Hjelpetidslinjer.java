package no.nav.k9.sak.ytelse.pleiepengerbarn.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public final class Hjelpetidslinjer {

    private Hjelpetidslinjer() {
    }

    /**
     * Lager en tidslinje der man kun har med helger ut fra tidslinjen som er oppgitt.
     * 
     * @param tidslinje Tidslinjen som brukes for å hente helger for.
     * @return En tidslinje med helger der man tar med alle helger som ligger inni
     *          eller kant-i-kant med minst én periode i tidslinjen. Dette betyr
     *          at man kan få helger på utsiden av tidslinjen (i hver ende).
     */
    public static LocalDateTimeline<Boolean> lagTidslinjeMedKunHelger(LocalDateTimeline<?> tidslinje) {
        final List<LocalDateSegment<Boolean>> helgesegmenter = new ArrayList<>();
        LocalDate sisteHelg = null; 
        for (LocalDateSegment<?> s : tidslinje) {
            final LocalDate førsteFom = lørdagenFørHelgEllerMandag(s.getFom());
            final LocalDate sisteTom = søndagenEtterHelgEllerFredag(s.getTom());
            
            LocalDate fom = førsteFom;
            while (fom.isBefore(sisteTom)) {
                if (sisteHelg != null && sisteHelg.equals(fom)) {
                    fom = fom.plusDays(7);
                    continue;
                }
                helgesegmenter.add(new LocalDateSegment<>(fom, fom.plusDays(1), Boolean.TRUE));
                sisteHelg = fom;
                fom = fom.plusDays(7);
            }
        }
        return new LocalDateTimeline<>(helgesegmenter);
    }
    
    private static LocalDate lørdagenFørHelgEllerMandag(LocalDate dato) {
        LocalDate førsteFom = dato;
        if (førsteFom.getDayOfWeek() == DayOfWeek.SUNDAY) {
            førsteFom = førsteFom.minusDays(1);
        } else if (førsteFom.getDayOfWeek() == DayOfWeek.MONDAY) {
            førsteFom = førsteFom.minusDays(2);
        }
        if (førsteFom.getDayOfWeek() != DayOfWeek.SATURDAY) {
            førsteFom = førsteFom.plusDays(DayOfWeek.SUNDAY.getValue() - førsteFom.getDayOfWeek().getValue() + 1);
        }
        return førsteFom;
    }

    private static LocalDate søndagenEtterHelgEllerFredag(LocalDate dato) {
        LocalDate sisteTom = dato;
        if (sisteTom.getDayOfWeek() == DayOfWeek.SATURDAY) {
            sisteTom = sisteTom.plusDays(1);
        } else if (sisteTom.getDayOfWeek() == DayOfWeek.FRIDAY) {
            sisteTom = sisteTom.plusDays(2);
        }
        if (sisteTom.getDayOfWeek() != DayOfWeek.SUNDAY) {
            sisteTom = sisteTom.minusDays(sisteTom.getDayOfWeek().getValue());
        }
        return sisteTom;
    }
    
    /**
     * Lager en ukestidslinje for mandag-fredag for oppgitt intervall.
     * 
     * @param fom Fra-og-med-datoen man skal generere ukestidslinje for.
     * @param tom Til-og-med-datoen man skal generere ukestidslinje for.
     * 
     * @return En tidslinje med et segment per uke. Hvert segment har maksimumsperioden
     *          mandag til fredag -- og kan være kortere i hver ende hvis ikke
     *          {@code fom} er en mandag og/eller {@code tom} er en fredag. 
     */
    public static LocalDateTimeline<Boolean> lagUkestidslinjeForMandagTilFredag(LocalDate fom, LocalDate tom) {
        Objects.requireNonNull(fom, "fom");
        Objects.requireNonNull(tom, "tom");
        if (fom.isAfter(tom)) {
            throw new IllegalArgumentException("fom kan ikke være etter tom.");
        }
        
        LocalDate nesteFom = fom;
        if (nesteFom.getDayOfWeek() == DayOfWeek.SATURDAY) {
            nesteFom = nesteFom.plusDays(2);
        }
        if (nesteFom.getDayOfWeek() == DayOfWeek.SUNDAY) {
            nesteFom = nesteFom.plusDays(1);
        }
        if (tom.getDayOfWeek() == DayOfWeek.SATURDAY) {
            tom = tom.minusDays(1);
        }
        if (tom.getDayOfWeek() == DayOfWeek.SUNDAY) {
            tom = tom.minusDays(2);
        }
        
        List<LocalDateSegment<Boolean>> ukesegmenter = new ArrayList<>();
        LocalDate førsteTom = nesteFom.plusDays(DayOfWeek.FRIDAY.getValue() - nesteFom.getDayOfWeek().getValue());
        if (førsteTom.isAfter(tom)) {
            if (nesteFom.isAfter(tom)) {
                // Kun helgeperiode.
                return LocalDateTimeline.empty();
            }
            return new LocalDateTimeline<Boolean>(nesteFom, tom, Boolean.TRUE);
        }
        ukesegmenter.add(new LocalDateSegment<Boolean>(nesteFom, førsteTom, Boolean.TRUE));
        nesteFom = førsteTom.plusDays(3);
        while (!nesteFom.plusDays(4).isAfter(tom)) {
            ukesegmenter.add(new LocalDateSegment<Boolean>(nesteFom, nesteFom.plusDays(4), Boolean.TRUE));
            nesteFom = nesteFom.plusDays(7); 
        }
        if (!nesteFom.isAfter(tom)) { // dvs nesteFom.plusDays(4).isAfter(tom))
            ukesegmenter.add(new LocalDateSegment<Boolean>(nesteFom, tom, Boolean.TRUE));
        }
        return new LocalDateTimeline<Boolean>(ukesegmenter);
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

        return new LocalDateTimeline<T>(resultat);
    }
}
