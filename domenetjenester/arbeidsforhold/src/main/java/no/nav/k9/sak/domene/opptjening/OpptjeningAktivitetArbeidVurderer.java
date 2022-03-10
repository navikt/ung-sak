package no.nav.k9.sak.domene.opptjening;

import java.util.List;

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

        // Vurder kun permisjonsperioder som overlapper opptjeningsperiode && aktivitetens lengde
        LocalDateTimeline<Boolean> tidslinjeTilVurdering = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(opptjeningsperiode.getFomDato(), opptjeningsperiode.getTomDato(), Boolean.TRUE)));
        tidslinjeTilVurdering = tidslinjeTilVurdering.intersection(aktivPermisjonTidslinje.compress());
        var aktivitetsTidslinje = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(input.getAktivitetPeriode().toLocalDateInterval(), Boolean.TRUE)));
        tidslinjeTilVurdering = tidslinjeTilVurdering.intersection(aktivitetsTidslinje.compress());

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
}
