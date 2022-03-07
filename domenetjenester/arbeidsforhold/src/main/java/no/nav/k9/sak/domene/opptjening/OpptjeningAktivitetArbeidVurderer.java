package no.nav.k9.sak.domene.opptjening;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.NavigableSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.VurderStatusInput;
import no.nav.k9.sak.typer.Stillingsprosent;


class OpptjeningAktivitetArbeidVurderer {
    private final Logger log = LoggerFactory.getLogger(OpptjeningAktivitetArbeidVurderer.class);

    private MellomliggendePeriodeUtleder mellomliggendePeriodeUtleder = new MellomliggendePeriodeUtleder();

    VurderingsStatus vurderArbeid(VurderStatusInput input) {
        var opptjeningsperiode = input.getOpptjeningsperiode();
        var yrkesaktivitet = input.getRegisterAktivitet();
        if (input.getRegisterAktivitet() == null) {
            return VurderingsStatus.TIL_VURDERING;
        }

        // Permisjoner på yrkesaktivitet
        List<LocalDateTimeline<Boolean>> aktivPermisjonTidslinjer = yrkesaktivitet.getPermisjon()
            .stream()
            .filter(permisjon -> erStørreEllerLik100Prosent(permisjon.getProsentsats()))
            .map(permisjon -> new LocalDateTimeline<>(permisjon.getFraOgMed(), permisjon.getTilOgMed(), Boolean.TRUE))
            .toList();
        LocalDateTimeline<Boolean> aktivPermisjonTidslinje = new LocalDateTimeline<>(List.of());
        for (LocalDateTimeline<Boolean> linje : aktivPermisjonTidslinjer) {
            aktivPermisjonTidslinje = aktivPermisjonTidslinje.combine(linje, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }

        // Vurder kun permisjonsperioder som overlapper opptjeningsperiode
        LocalDateTimeline<Boolean> tidslinjeTilVurdering = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(opptjeningsperiode.getFomDato(), opptjeningsperiode.getTomDato(), Boolean.TRUE)));
        tidslinjeTilVurdering = tidslinjeTilVurdering.intersection(aktivPermisjonTidslinje.compress());

        // Legg til mellomliggende periode dersom helg mellom permisjonsperioder
        LocalDateTimeline<Boolean> mellomliggendePerioder = mellomliggendePeriodeUtleder.beregnMellomliggendePeriode(tidslinjeTilVurdering.compress());
        for (LocalDateSegment<Boolean> mellomliggendePeriode : mellomliggendePerioder) {
            tidslinjeTilVurdering = tidslinjeTilVurdering.combine(mellomliggendePeriode, this::mergePerioder, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }

        // Underkjent vurderingsstatus dersom sammenhengende permisjonsperiode > 14 dager
        var permisjonOver14Dager = tidslinjeTilVurdering.compress().stream()
            .filter(segment -> segment.getValue() == Boolean.TRUE && segment.getLocalDateInterval().days() > 14)
            .findFirst();
        if (permisjonOver14Dager.isPresent()) {
            log.info("Opptjeningsaktivitet for virksomhet={} underkjennes pga permisjoner som overstiger 14 dager. Permitteringsperiode={}, opptjeningsperiode={}", yrkesaktivitet.getArbeidsgiver(), permisjonOver14Dager.get().getLocalDateInterval() ,opptjeningsperiode);
            return VurderingsStatus.UNDERKJENT;
        }
        return VurderingsStatus.TIL_VURDERING;
    }

    private boolean erStørreEllerLik100Prosent(Stillingsprosent prosentsats) {
        return Stillingsprosent.HUNDRED.getVerdi().intValue() <= prosentsats.getVerdi().intValue();
    }

    private LocalDateSegment<Boolean> mergePerioder(LocalDateInterval di, LocalDateSegment<Boolean> førsteVersjon, LocalDateSegment<Boolean> sisteVersjon) {

        if ((førsteVersjon == null || førsteVersjon.getValue() == null) && sisteVersjon != null) {
            return lagSegment(di, sisteVersjon.getValue());
        } else if ((sisteVersjon == null || sisteVersjon.getValue() == null) && førsteVersjon != null) {
            return lagSegment(di, førsteVersjon.getValue());
        }

        var første = førsteVersjon.getValue();
        var siste = sisteVersjon.getValue();
        return lagSegment(di, første || siste);
    }


    private LocalDateSegment<Boolean> lagSegment(LocalDateInterval di, Boolean siste) {
        if (siste == null) {
            return new LocalDateSegment<>(di, null);
        }
        return new LocalDateSegment<>(di, siste);
    }

    /**
     * Implementerer algoritme for å utlede tidslinje for mellomliggende perioder.
     */
    static class MellomliggendePeriodeUtleder {

        LocalDateTimeline<Boolean> beregnMellomliggendePeriode(LocalDateTimeline<Boolean> e) {
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
}
