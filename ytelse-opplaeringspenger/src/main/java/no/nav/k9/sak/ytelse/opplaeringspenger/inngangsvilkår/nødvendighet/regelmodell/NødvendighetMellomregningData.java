package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.regelmodell;

import java.util.Objects;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

public class NødvendighetMellomregningData {

    private final LocalDateTimeline<Boolean> tidslinjeTilVurdering;
    private final LocalDateTimeline<OpplæringVurdering> opplæringVurderingTidslinje;

    public NødvendighetMellomregningData(NødvendighetVilkårGrunnlag grunnlag) {
        Objects.requireNonNull(grunnlag);

        this.tidslinjeTilVurdering = new LocalDateTimeline<>(grunnlag.getFom(), grunnlag.getTom(), Boolean.TRUE);
        this.opplæringVurderingTidslinje = grunnlag.getVurdertOpplæringPerioder();
    }

    public LocalDateTimeline<Boolean> getTidslinjeTilVurdering() {
        return tidslinjeTilVurdering;
    }

    public LocalDateTimeline<OpplæringVurdering> getOpplæringVurderingTidslinje() {
        return opplæringVurderingTidslinje.compress();
    }
}
