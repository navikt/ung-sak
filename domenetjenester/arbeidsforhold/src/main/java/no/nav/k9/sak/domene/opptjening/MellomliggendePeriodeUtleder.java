package no.nav.k9.sak.domene.opptjening;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.NavigableSet;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

/**
 * Implementerer algoritme for å utlede tidslinje for mellomliggende perioder.
 */
public class MellomliggendePeriodeUtleder {

    public LocalDateTimeline<Boolean> beregnMellomliggendePeriode(LocalDateTimeline<Boolean> e) {
        // compress for å sikre at vi slipper å sjekke sammenhengende segmenter med samme verdi
        LocalDateTimeline<Boolean> timeline = e.compress();

        return timeline.collect(this::toPeriod, true).mapValue(v -> Boolean.TRUE);
    }

    private boolean toPeriod(@SuppressWarnings("unused") NavigableSet<LocalDateSegment<Boolean>> segmenterFør,
                             LocalDateSegment<Boolean> segmentUnderVurdering,
                             NavigableSet<LocalDateSegment<Boolean>> foregåendeSegmenter,
                             NavigableSet<LocalDateSegment<Boolean>> påfølgendeSegmenter) {

        if (foregåendeSegmenter.isEmpty() || påfølgendeSegmenter.isEmpty()) {
            // mellomliggende segmenter har ingen verdi, så skipper de som har
            return false;
        } else {
            LocalDateSegment<Boolean> foregående = foregåendeSegmenter.last();
            LocalDateSegment<Boolean> påfølgende = påfølgendeSegmenter.first();


            return segmentUnderVurdering.getValue() == null && Boolean.TRUE.equals(foregående.getValue()) && Boolean.TRUE.equals(påfølgende.getValue())
                && erKantIKantPåTversAvHelg(foregående.getLocalDateInterval(), påfølgende.getLocalDateInterval());
        }
    }

    boolean erKantIKantPåTversAvHelg(LocalDateInterval periode1, LocalDateInterval periode2) {
        return utledTomDato(periode1).equals(utledFom(periode2).minusDays(1)) || utledTomDato(periode2).equals(utledFom(periode1).minusDays(1));
    }

    private LocalDate utledFom(LocalDateInterval periode1) {
        var fomDato = periode1.getFomDato();
        if (DayOfWeek.SATURDAY.equals(fomDato.getDayOfWeek())) {
            return fomDato.plusDays(2);
        } else if (DayOfWeek.SUNDAY.equals(fomDato.getDayOfWeek())) {
            return fomDato.plusDays(1);
        }
        return fomDato;
    }

    private LocalDate utledTomDato(LocalDateInterval periode1) {
        var tomDato = periode1.getTomDato();
        if (DayOfWeek.FRIDAY.equals(tomDato.getDayOfWeek())) {
            return tomDato.plusDays(2);
        } else if (DayOfWeek.SATURDAY.equals(tomDato.getDayOfWeek())) {
            return tomDato.plusDays(1);
        }
        return tomDato;
    }
}


