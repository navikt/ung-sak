package no.nav.k9.sak.ytelse.omsorgspenger.beregnytelse;

import java.util.HashSet;
import java.util.Set;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;

public class TidsserieUtil {

    public static <T> LocalDateSegment<Set<T>> union(LocalDateInterval dateInterval, LocalDateSegment<Set<T>> lhs, LocalDateSegment<Set<T>> rhs) {
        Set<T> lv = lhs == null ? null : lhs.getValue();
        Set<T> rv = rhs == null ? null : rhs.getValue();

        Set<T> union;
        if (lv == null) {
            union = rv;
        } else if (rv == null) {
            union = lv;
        } else {
            union = new HashSet<>();
            union.addAll(lv);
            union.addAll(rv);
        }
        return new LocalDateSegment<>(dateInterval, union);
    }

    public static <T> LocalDateSegment<Set<T>> minus(LocalDateInterval dateInterval, LocalDateSegment<Set<T>> lhs, LocalDateSegment<Set<T>> rhs) {
        Set<T> lv = lhs == null ? null : lhs.getValue();
        Set<T> rv = rhs == null ? null : rhs.getValue();

        Set<T> resultat;
        if (lv == null) {
            resultat = null;
        } else if (rv == null) {
            resultat = lv;
        } else {
            resultat = new HashSet<>(lv);
            resultat.removeAll(rv);

        }
        return new LocalDateSegment<>(dateInterval, resultat);
    }

}
