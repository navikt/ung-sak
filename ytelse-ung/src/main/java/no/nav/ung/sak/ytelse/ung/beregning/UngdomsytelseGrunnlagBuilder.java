package no.nav.ung.sak.ytelse.ung.beregning;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.sak.behandlingslager.diff.DiffEntity;
import no.nav.ung.sak.ytelse.ung.uttak.UngdomsytelseUttakPerioder;

class UngdomsytelseGrunnlagBuilder {

    private static final Logger log = LoggerFactory.getLogger(UngdomsytelseGrunnlagBuilder.class);

    private LocalDateTimeline<UngdomsytelseSatser> perioder = LocalDateTimeline.empty();
    private UngdomsytelseUttakPerioder uttakPerioder;

    private boolean built = false;

    UngdomsytelseGrunnlagBuilder(UngdomsytelseGrunnlag kladd) {
        if (kladd != null && kladd.getSatsPerioder() != null) {
            this.leggTilPerioder(kladd.getSatsPerioder().getPerioder());
            this.uttakPerioder = kladd.getUttakPerioder();
        }
    }

    UngdomsytelseGrunnlagBuilder leggTilPerioder(List<UngdomsytelseSatsPeriode> perioder) {
        validerState();
        var segmenter = perioder.stream().map(it -> new LocalDateSegment<>(it.getPeriode().toLocalDateInterval(), it.satser())).toList();
        LocalDateTimeline<UngdomsytelseSatser> nyePerioder = new LocalDateTimeline<>(segmenter);
        return leggTilPerioder(nyePerioder);

    }

    UngdomsytelseGrunnlagBuilder leggTilPerioder(LocalDateTimeline<UngdomsytelseSatser> nyePerioder){
        this.perioder = this.perioder
            .union(nyePerioder, StandardCombinators::coalesceRightHandSide)
            .compress();
        return this;
    }

    UngdomsytelseGrunnlagBuilder medUttakPerioder(UngdomsytelseUttakPerioder uttakPerioder){
        this.uttakPerioder = uttakPerioder;
        return this;
    }


    UngdomsytelseGrunnlagBuilder fjernPeriode(LocalDateInterval periode){
        this.perioder = this.perioder.disjoint(periode);
        return this;
    }

    UngdomsytelseGrunnlag build() {
        validerState();
        this.built = true;
        return repeatableBuild();
    }

    private UngdomsytelseGrunnlag repeatableBuild() {
        UngdomsytelseGrunnlag resultat = new UngdomsytelseGrunnlag();
        resultat.setSatsPerioder(new UngdomsytelseSatsPerioder(perioder.toSegments().stream().map(UngdomsytelseGrunnlagBuilder::mapTilPeriode).toList()));
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
