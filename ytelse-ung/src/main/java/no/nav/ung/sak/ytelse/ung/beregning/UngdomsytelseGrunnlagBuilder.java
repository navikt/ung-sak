package no.nav.ung.sak.ytelse.ung.beregning;

import java.sql.Clob;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.sak.behandlingslager.behandling.beregning.RegelData;
import no.nav.ung.sak.behandlingslager.diff.DiffEntity;
import no.nav.ung.sak.ytelse.ung.uttak.UngdomsytelseUttakPerioder;

class UngdomsytelseGrunnlagBuilder {

    private static final Logger log = LoggerFactory.getLogger(UngdomsytelseGrunnlagBuilder.class);

    private LocalDateTimeline<UngdomsytelseSatser> perioder = LocalDateTimeline.empty();
    private Clob satsBeregningRegelInput;
    private Clob satsBeregningRegelSporing;
    private UngdomsytelseUttakPerioder uttakPerioder;

    private boolean built = false;

    UngdomsytelseGrunnlagBuilder(UngdomsytelseGrunnlag kladd) {
        if (kladd != null && kladd.getSatsPerioder() != null) {
            var satsPerioder = kladd.getSatsPerioder();
            this.leggTilPerioder(satsPerioder.getPerioder(), satsPerioder.getRegelInput().getClob(), satsPerioder.getRegelSporing().getClob());
            this.uttakPerioder = kladd.getUttakPerioder();
        }
    }

    UngdomsytelseGrunnlagBuilder leggTilPerioder(List<UngdomsytelseSatsPeriode> perioder, Clob regelInput, Clob regelSporing) {
        validerState();
        var segmenter = perioder.stream().map(it -> new LocalDateSegment<>(it.getPeriode().toLocalDateInterval(), it.satser())).toList();
        LocalDateTimeline<UngdomsytelseSatser> nyePerioder = new LocalDateTimeline<>(segmenter);
        return leggTilPerioder(nyePerioder, regelInput, regelSporing);
    }

    UngdomsytelseGrunnlagBuilder leggTilPerioder(LocalDateTimeline<UngdomsytelseSatser> nyePerioder, Clob regelInput, Clob regelSporing){
        this.perioder = this.perioder
            .union(nyePerioder, StandardCombinators::coalesceRightHandSide)
            .compress();
        this.satsBeregningRegelInput = regelInput;
        this.satsBeregningRegelSporing = regelInput;
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
        var entitetPerioder = perioder.toSegments().stream().map(UngdomsytelseGrunnlagBuilder::mapTilPeriode).toList();
        var satsPerioder = new UngdomsytelseSatsPerioder(entitetPerioder, satsBeregningRegelInput, satsBeregningRegelSporing);
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
