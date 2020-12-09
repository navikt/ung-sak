package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;

import java.util.Objects;

public final class TimelineMerger {

    private TimelineMerger() {
    }

    public static LocalDateSegment<VurdertSøktPeriode> mergeSegments(LocalDateInterval di,
                                                                     LocalDateSegment<VurdertSøktPeriode> førsteVersjon,
                                                                     LocalDateSegment<VurdertSøktPeriode> sisteVersjon) {
        if ((førsteVersjon == null || førsteVersjon.getValue() == null) && sisteVersjon != null) {
            return lagSegment(di, sisteVersjon.getValue());
        } else if ((sisteVersjon == null || sisteVersjon.getValue() == null) && førsteVersjon != null) {
            return lagSegment(di, førsteVersjon.getValue());
        }

        var første = førsteVersjon.getValue();
        var siste = sisteVersjon.getValue();

        // Antar her at alt har samme arbeidsforhold info innenfor samme
        if (Objects.equals(første.getUtfall(), siste.getUtfall())) {
            return lagSegment(di, sisteVersjon.getValue());
        } else {
            if (Objects.equals(første.getUtfall(), Utfall.OPPFYLT)) {
                return lagSegment(di, første);
            } else {
                return lagSegment(di, siste);
            }
        }
    }

    private static LocalDateSegment<VurdertSøktPeriode> lagSegment(LocalDateInterval di, VurdertSøktPeriode siste) {
        if (siste == null) {
            return new LocalDateSegment<>(di, null);
        }
        var aktivitetPeriode = new VurdertSøktPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(di.getFomDato(), di.getTomDato()), siste.getType(), siste.getArbeidsgiver(), siste.getArbeidsforholdRef(), siste.getUtfall());
        return new LocalDateSegment<>(di, aktivitetPeriode);
    }
}
