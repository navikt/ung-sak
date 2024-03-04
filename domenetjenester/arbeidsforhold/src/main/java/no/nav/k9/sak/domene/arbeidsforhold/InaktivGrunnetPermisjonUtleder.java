package no.nav.k9.sak.domene.arbeidsforhold;

import java.util.List;
import java.util.Map;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.opptjening.MellomliggendeHelgUtleder;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.PermisjonPerYrkesaktivitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class InaktivGrunnetPermisjonUtleder {

    private static final MellomliggendeHelgUtleder mellomliggendeHelgUtleder = new MellomliggendeHelgUtleder();


    /**
     * Finner tidslinje for permisjoner med varighet mer enn 14 dager.
     * <p>
     * Perioder der ytelse kan være opphav til permisjonen klippes bort.
     *
     * @param yrkesaktivitet     Yrkesaktivitet
     * @param tidslinjePerYtelse Tidslinje pr ytelse mottatt
     * @param vilkårsperiode     Aktuell vilkårsperiode
     * @return Tidslinje for perioder der arbeidsforholdet er ansett som inaktivt grunnet permisjon over 14 dager
     */
    public static LocalDateTimeline<Boolean> utledTidslinjeForSammengengendePermisjonOver14Dager(Yrkesaktivitet yrkesaktivitet,
                                                                                                 Map<OpptjeningAktivitetType, LocalDateTimeline<Boolean>> tidslinjePerYtelse,
                                                                                                 DatoIntervallEntitet vilkårsperiode) {
        // Permisjoner på yrkesaktivitet
        LocalDateTimeline<Boolean> tidslinjeTilVurdering = PermisjonPerYrkesaktivitet.utledPermisjonPerYrkesaktivitet(yrkesaktivitet, tidslinjePerYtelse, vilkårsperiode);

        // Legg til mellomliggende periode dersom helg mellom permisjonsperioder
        LocalDateTimeline<Boolean> mellomliggendePerioder = mellomliggendeHelgUtleder.beregnMellomliggendeHelg(tidslinjeTilVurdering);
        for (LocalDateSegment<Boolean> mellomliggendePeriode : mellomliggendePerioder) {
            tidslinjeTilVurdering = tidslinjeTilVurdering.combine(mellomliggendePeriode, InaktivGrunnetPermisjonUtleder::mergePerioder, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }

        return tidslinjeTilVurdering.compress()
            .map(segment -> List.of(new LocalDateSegment<>(segment.getLocalDateInterval(), segment.getLocalDateInterval().days() > 14 && segment.getValue())));

    }


    private static LocalDateSegment<Boolean> mergePerioder(LocalDateInterval di, LocalDateSegment<Boolean> førsteVersjon, LocalDateSegment<Boolean> sisteVersjon) {

        if ((førsteVersjon == null || førsteVersjon.getValue() == null) && sisteVersjon != null) {
            return lagSegment(di, sisteVersjon.getValue());
        } else if ((sisteVersjon == null || sisteVersjon.getValue() == null) && førsteVersjon != null) {
            return lagSegment(di, førsteVersjon.getValue());
        }

        var første = førsteVersjon.getValue();
        var siste = sisteVersjon.getValue();
        return lagSegment(di, første || siste);
    }

    private static LocalDateSegment<Boolean> lagSegment(LocalDateInterval di, Boolean siste) {
        if (siste == null) {
            return new LocalDateSegment<>(di, null);
        }
        return new LocalDateSegment<>(di, siste);
    }


}
