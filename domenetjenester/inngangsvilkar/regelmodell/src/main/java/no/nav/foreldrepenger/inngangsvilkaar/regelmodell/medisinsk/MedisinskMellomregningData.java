package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medisinsk;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

class MedisinskMellomregningData {

    private final MedisinskvilkårGrunnlag grunnlag;
    private LocalDateTimeline<Pleiegrad> pleietidslinje;

    MedisinskMellomregningData(MedisinskvilkårGrunnlag grunnlag) {
        Objects.requireNonNull(grunnlag);
        this.grunnlag = grunnlag;
        this.pleietidslinje = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(grunnlag.getFom(), grunnlag.getTom(), Pleiegrad.NULL)));
    }

    MedisinskvilkårGrunnlag getGrunnlag() {
        return grunnlag;
    }

    void addBehovsPeriode(PleiePeriode periode) {
        Objects.requireNonNull(periode);
        final var segment = new LocalDateSegment<>(periode.getFraOgMed(), periode.getTilOgMed(), periode.getGrad());
        final var periodeTidslinje = new LocalDateTimeline<>(List.of(segment));

        this.pleietidslinje = pleietidslinje.combine(periodeTidslinje, this::sjekkVurdering, LocalDateTimeline.JoinStyle.CROSS_JOIN);
    }

    List<PleiePeriode> getPerioderMedPleieOgGrad() {
        return pleietidslinje.compress()
            .toSegments()
            .stream()
            .map(segment -> new PleiePeriode(segment.getFom(), segment.getTom(), segment.getValue()))
            .collect(Collectors.toList());
    }

    List<PleiePeriode> getPerioderUtenTilsynOgPleie() {
        return getPerioderMedPleieOgGrad()
            .stream()
            .filter(it -> Pleiegrad.NULL.equals(it.getGrad()))
            .collect(Collectors.toList());
    }

    List<PleiePeriode> getPerioderMedEnTilsynsperson() {
        return getPerioderMedPleieOgGrad()
            .stream()
            .filter(it -> Pleiegrad.KONTINUERLIG_TILSYN.equals(it.getGrad()))
            .collect(Collectors.toList());
    }

    List<PleiePeriode> getPerioderMedToTilsynsperson() {
        return getPerioderMedPleieOgGrad()
            .stream()
            .filter(it -> Pleiegrad.INNLEGGELSE.equals(it.getGrad()) || Pleiegrad.UTVIDET_TILSYN.equals(it.getGrad()))
            .collect(Collectors.toList());
    }

    private LocalDateSegment<Pleiegrad> sjekkVurdering(LocalDateInterval di,
                                                       LocalDateSegment<Pleiegrad> førsteVersjon,
                                                       LocalDateSegment<Pleiegrad> sisteVersjon) {

        if (førsteVersjon == null && sisteVersjon != null) {
            return lagSegment(di, sisteVersjon.getValue());
        } else if (sisteVersjon == null && førsteVersjon != null) {
            return lagSegment(di, førsteVersjon.getValue());
        }

        var første = førsteVersjon.getValue();
        var siste = sisteVersjon.getValue();

        if (!Pleiegrad.INNLEGGELSE.equals(første) && Pleiegrad.INNLEGGELSE.equals(siste)) {
            return lagSegment(di, siste);
        } else if (Pleiegrad.INNLEGGELSE.equals(første) && !Pleiegrad.INNLEGGELSE.equals(siste)) {
            return lagSegment(di, første);
        } else if (!Pleiegrad.UTVIDET_TILSYN.equals(første) && Pleiegrad.UTVIDET_TILSYN.equals(siste)) {
            return lagSegment(di, siste);
        } else if (Pleiegrad.UTVIDET_TILSYN.equals(første) && !Pleiegrad.UTVIDET_TILSYN.equals(siste)) {
            return lagSegment(di, første);
        } else if (!Pleiegrad.KONTINUERLIG_TILSYN.equals(første) && Pleiegrad.KONTINUERLIG_TILSYN.equals(siste)) {
            return lagSegment(di, siste);
        } else if (Pleiegrad.KONTINUERLIG_TILSYN.equals(første) && !Pleiegrad.KONTINUERLIG_TILSYN.equals(siste)) {
            return lagSegment(di, første);
        } else {
            return sisteVersjon;
        }
    }

    private LocalDateSegment<Pleiegrad> lagSegment(LocalDateInterval di, Pleiegrad siste) {
        return new LocalDateSegment<>(di, siste);
    }

    void oppdaterResultat(MedisinskVilkårResultat resultatStruktur) {
        Objects.requireNonNull(resultatStruktur);

        resultatStruktur.setPleieperioder(getPerioderMedPleieOgGrad());
    }

    @Override
    public String toString() {
        return "MedisinskMellomregningData{" +
            "grunnlag=" + grunnlag +
            ", pleietidslinje=" + pleietidslinje +
            '}';
    }
}
