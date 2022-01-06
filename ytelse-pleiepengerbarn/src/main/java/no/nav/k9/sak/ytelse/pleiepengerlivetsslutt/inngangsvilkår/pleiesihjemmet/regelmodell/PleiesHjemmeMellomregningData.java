package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.pleiesihjemmet.regelmodell;

import java.util.List;
import java.util.Objects;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

class PleiesHjemmeMellomregningData {

    private final PleiesHjemmeVilkårGrunnlag grunnlag;
    private LocalDateTimeline<Pleielokasjon> pleiesHjemmetidslinje;

    PleiesHjemmeMellomregningData(PleiesHjemmeVilkårGrunnlag grunnlag) {
        Objects.requireNonNull(grunnlag);
        this.grunnlag = grunnlag;
        var fom = grunnlag.getVilkårsperiode().getFom();
        var tom = grunnlag.getVilkårsperiode().getTom();
        this.pleiesHjemmetidslinje = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(fom, tom, Pleielokasjon.HJEMME)));
    }

    PleiesHjemmeVilkårGrunnlag getGrunnlag() {
        return grunnlag;
    }

    void addInnleggelsePeriode(PleiePeriode periode) {
        Objects.requireNonNull(periode);
        final var segment = new LocalDateSegment<>(periode.getFraOgMed(), periode.getTilOgMed(), periode.getPleielokasjon());
        final var periodeTidslinje = new LocalDateTimeline<>(List.of(segment));

        pleiesHjemmetidslinje = pleiesHjemmetidslinje.combine(periodeTidslinje, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
    }

    List<PleiePeriode> getBeregnedePerioderMedPleielokasjon() {
        return pleiesHjemmetidslinje.compress()
            .toSegments()
            .stream()
            .map(segment -> new PleiePeriode(segment.getFom(), segment.getTom(), segment.getValue()))
            .toList();
    }


    void oppdaterResultat(PleiesHjemmeVilkårResultat resultatStruktur) {
        Objects.requireNonNull(resultatStruktur);

        resultatStruktur.setPleieperioder(getBeregnedePerioderMedPleielokasjon());
    }

    @Override
    public String toString() {
        return "MedisinskMellomregningData{" +
            "grunnlag=" + grunnlag +
            ", pleiesHjemmetidslinje=" + pleiesHjemmetidslinje +
            '}';
    }
}
