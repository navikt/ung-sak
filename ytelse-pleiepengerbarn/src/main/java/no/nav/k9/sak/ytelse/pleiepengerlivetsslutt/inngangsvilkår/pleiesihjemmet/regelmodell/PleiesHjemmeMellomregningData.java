package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.pleiesihjemmet.regelmodell;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

class PleiesHjemmeMellomregningData {

    private final PleiesHjemmeVilkårGrunnlag grunnlag;
    private LocalDateTimeline<Pleielokasjon> pleiesHjemmetidslinje;

    PleiesHjemmeMellomregningData(PleiesHjemmeVilkårGrunnlag grunnlag) {
        Objects.requireNonNull(grunnlag);
        this.grunnlag = grunnlag;
        this.pleiesHjemmetidslinje = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(grunnlag.getVilkårsperiode().getFom(), grunnlag.getVilkårsperiode().getTom(), Pleielokasjon.HJEMME)));
    }

    private static <V> LocalDateTimeline<V> trimTidslinje(LocalDateTimeline<V> tidslinje, LocalDateInterval maxInterval) {
        return tidslinje.intersection(maxInterval);
    }

    PleiesHjemmeVilkårGrunnlag getGrunnlag() {
        return grunnlag;
    }

    void addInnleggelsePeriode(PleiePeriode periode) {
        Objects.requireNonNull(periode);
        final var segment = new LocalDateSegment<>(periode.getFraOgMed(), periode.getTilOgMed(), periode.getPleielokasjon());
        final var periodeTidslinje = new LocalDateTimeline<>(List.of(segment));

        this.pleiesHjemmetidslinje = pleiesHjemmetidslinje.combine(periodeTidslinje, this::sjekkVurdering, LocalDateTimeline.JoinStyle.CROSS_JOIN);
    }

    List<PleiePeriode> getPerioderMedPleielokasjon() {
        return trimTidslinje(pleiesHjemmetidslinje.compress(), grunnlag.getInterval())
            .toSegments()
            .stream()
            .map(segment -> new PleiePeriode(segment.getFom(), segment.getTom(), segment.getValue()))
            .collect(Collectors.toList());
    }

    List<PleiePeriode> getPerioderPleiesIHjemmet() {
        return getPerioderMedPleielokasjon()
            .stream()
            .filter(it -> Pleielokasjon.HJEMME.equals(it.getPleielokasjon()))
            .collect(Collectors.toList());
    }

    private LocalDateSegment<Pleielokasjon> sjekkVurdering(LocalDateInterval di,
                                                           LocalDateSegment<Pleielokasjon> førsteVersjon,
                                                           LocalDateSegment<Pleielokasjon> sisteVersjon) {

        if (førsteVersjon == null && sisteVersjon != null) {
            return lagSegment(di, sisteVersjon.getValue());
        } else if (sisteVersjon == null && førsteVersjon != null) {
            return lagSegment(di, førsteVersjon.getValue());
        }

        var første = førsteVersjon.getValue();
        var siste = sisteVersjon.getValue();

        if (!Pleielokasjon.INNLAGT.equals(første) && Pleielokasjon.INNLAGT.equals(siste)) {
            return lagSegment(di, siste);
        } else if (Pleielokasjon.INNLAGT.equals(første) && !Pleielokasjon.INNLAGT.equals(siste)) {
            return lagSegment(di, første);
        } else {
            return sisteVersjon;
        }
    }

    private LocalDateSegment<Pleielokasjon> lagSegment(LocalDateInterval di, Pleielokasjon siste) {
        return new LocalDateSegment<>(di, siste);
    }

    void oppdaterResultat(PleiesHjemmeVilkårResultat resultatStruktur) {
        Objects.requireNonNull(resultatStruktur);

        resultatStruktur.setPleieperioder(getPerioderMedPleielokasjon());
    }

    @Override
    public String toString() {
        return "MedisinskMellomregningData{" +
            "grunnlag=" + grunnlag +
            ", pleiesHjemmetidslinje=" + pleiesHjemmetidslinje +
            '}';
    }
}
