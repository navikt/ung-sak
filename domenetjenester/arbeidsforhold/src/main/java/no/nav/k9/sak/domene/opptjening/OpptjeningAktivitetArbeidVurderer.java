package no.nav.k9.sak.domene.opptjening;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.PermisjonPerYrkesaktivitet;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.VurderStatusInput;


class OpptjeningAktivitetArbeidVurderer {
    private final Logger log = LoggerFactory.getLogger(OpptjeningAktivitetArbeidVurderer.class);

    private MellomliggendeHelgUtleder mellomliggendeHelgUtleder = new MellomliggendeHelgUtleder();

    VurderingsStatus vurderArbeid(VurderStatusInput input) {
        var yrkesaktivitet = input.getRegisterAktivitet();
        if (input.getRegisterAktivitet() == null) {
            return VurderingsStatus.TIL_VURDERING;
        }

        var tidslinjePerYtelse = input.getTidslinjePerYtelse();
        var vilkårsperiode = input.getVilkårsperiode();
        // Permisjoner på yrkesaktivitet
        LocalDateTimeline<Boolean> tidslinjeTilVurdering = PermisjonPerYrkesaktivitet.utledPermisjonPerYrkesaktivitet(yrkesaktivitet, tidslinjePerYtelse, vilkårsperiode, input.getErMigrertSkjæringstidspunkt());

        // Vurder kun permisjonsperioder som overlapper aktivitetens lengde

        // Legg til mellomliggende periode dersom helg mellom permisjonsperioder
        LocalDateTimeline<Boolean> mellomliggendePerioder = mellomliggendeHelgUtleder.beregnMellomliggendeHelg(tidslinjeTilVurdering);
        for (LocalDateSegment<Boolean> mellomliggendePeriode : mellomliggendePerioder) {
            tidslinjeTilVurdering = tidslinjeTilVurdering.combine(mellomliggendePeriode, this::mergePerioder, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }

        // Underkjent vurderingsstatus dersom sammenhengende permisjonsperiode > 14 dager og overlapper med aktivitetsperiode
        var permisjonOver14Dager = tidslinjeTilVurdering.compress().stream()
            .filter(segment -> segment.getValue() == Boolean.TRUE &&
                segment.getLocalDateInterval().days() > 14 &&
                segment.getLocalDateInterval().overlaps(input.getAktivitetPeriode().toLocalDateInterval()) &&
                segment.getLocalDateInterval().overlaps(input.getOpptjeningsperiode().toLocalDateInterval()))
            .findFirst();
        if (permisjonOver14Dager.isPresent()) {
            log.info("Opptjeningsaktivitet for virksomhet={} underkjennes pga permisjoner som overstiger 14 dager. Permitteringsperiode={}", yrkesaktivitet.getArbeidsgiver(), permisjonOver14Dager.get().getLocalDateInterval());
            return VurderingsStatus.UNDERKJENT;
        }
        return VurderingsStatus.TIL_VURDERING;
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
