package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell;

import java.util.List;
import java.util.Objects;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

public class MedisinskMellomregningData {

    private final MedisinskVilkårGrunnlag grunnlag;
    private LocalDateTimeline<Pleielokasjon> pleiesHjemmetidslinje;

    MedisinskMellomregningData(MedisinskVilkårGrunnlag grunnlag) {
        Objects.requireNonNull(grunnlag);
        this.grunnlag = grunnlag;
        this.pleiesHjemmetidslinje = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(grunnlag.getFom(), grunnlag.getTom(), Pleielokasjon.HJEMME)));
    }

    public MedisinskVilkårGrunnlag getGrunnlag() {
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


    void oppdaterResultat(MedisinskVilkårResultat resultatStruktur) {
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
