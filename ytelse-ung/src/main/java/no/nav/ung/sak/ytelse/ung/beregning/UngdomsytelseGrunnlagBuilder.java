package no.nav.ung.sak.ytelse.ung.beregning;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.diff.DiffEntity;
import no.nav.ung.sak.ytelse.ung.uttak.UngdomsytelseUttakPerioder;

class UngdomsytelseGrunnlagBuilder {

    private UngdomsytelseSatsPerioder satsPerioder;
    private UngdomsytelseUttakPerioder uttakPerioder;

    private boolean built = false;

    UngdomsytelseGrunnlagBuilder(UngdomsytelseGrunnlag kladd) {
        if (kladd != null && kladd.getSatsPerioder() != null) {
            this.satsPerioder = kladd.getSatsPerioder();
            this.uttakPerioder = kladd.getUttakPerioder();
        }
    }

    UngdomsytelseGrunnlagBuilder medSatsPerioder(LocalDateTimeline<UngdomsytelseSatser> nyePerioder, String regelInput, String regelSporing) {
        var entitetPerioder = nyePerioder.toSegments().stream().map(UngdomsytelseGrunnlagBuilder::mapTilPeriode).toList();
        this.satsPerioder = new UngdomsytelseSatsPerioder(entitetPerioder, regelInput, regelSporing);
        return this;
    }

    UngdomsytelseGrunnlagBuilder medUttakPerioder(UngdomsytelseUttakPerioder uttakPerioder) {
        this.uttakPerioder = uttakPerioder;
        return this;
    }


    UngdomsytelseGrunnlag build() {
        validerState();
        this.built = true;
        return repeatableBuild();
    }

    private UngdomsytelseGrunnlag repeatableBuild() {
        UngdomsytelseGrunnlag resultat = new UngdomsytelseGrunnlag();
        resultat.setSatsPerioder(satsPerioder);
        resultat.setUttakPerioder(uttakPerioder);
        return resultat;
    }

    private static UngdomsytelseSatsPeriode mapTilPeriode(LocalDateSegment<UngdomsytelseSatser> segment) {
        return new UngdomsytelseSatsPeriode(segment.getLocalDateInterval(), segment.getValue());
    }

    boolean erForskjellig(UngdomsytelseGrunnlag grunnlag, DiffEntity differ) {
        return differ.areDifferent(grunnlag, repeatableBuild());
    }

    private void validerState() {
        if (built) {
            throw new IllegalStateException("[Utviklerfeil] Skal ikke gjenbruke builder!");
        }
    }


}
