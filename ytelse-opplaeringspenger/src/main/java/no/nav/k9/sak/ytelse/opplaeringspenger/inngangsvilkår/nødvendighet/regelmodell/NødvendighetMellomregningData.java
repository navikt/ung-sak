package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.regelmodell;

import java.util.List;
import java.util.Objects;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

public class NødvendighetMellomregningData {

    private final LocalDateTimeline<Boolean> tidslinjeTilVurdering;
    private LocalDateTimeline<OpplæringVurdering> opplæringVurderingTidslinje;
    private LocalDateTimeline<InstitusjonVurdering> institusjonVurderingTidslinje;

    public NødvendighetMellomregningData(NødvendighetVilkårGrunnlag grunnlag) {
        Objects.requireNonNull(grunnlag);

        this.tidslinjeTilVurdering = new LocalDateTimeline<>(grunnlag.getFom(), grunnlag.getTom(), Boolean.TRUE);
        this.opplæringVurderingTidslinje = grunnlag.getVurdertOpplæringPerioder();
        this.institusjonVurderingTidslinje = grunnlag.getVurdertInstitusjonPerioder();
    }

    public LocalDateTimeline<Boolean> getTidslinjeTilVurdering() {
        return tidslinjeTilVurdering;
    }

    public LocalDateTimeline<OpplæringVurdering> getOpplæringVurderingTidslinje() {
        return opplæringVurderingTidslinje.compress();
    }

    public LocalDateTimeline<InstitusjonVurdering> getInstitusjonVurderingTidslinje() {
        return institusjonVurderingTidslinje.compress();
    }

    List<NødvendigOpplæringPeriode> getOpplæringVurderingPerioder() {
        return opplæringVurderingTidslinje.compress()
            .stream()
            .map(segment -> new NødvendigOpplæringPeriode(segment.getFom(), segment.getTom(), segment.getValue()))
            .toList();
    }

    List<GodkjentInstitusjonPeriode> getInstitusjonVurderingPerioder() {
        return institusjonVurderingTidslinje.compress()
            .stream()
            .map(segment -> new GodkjentInstitusjonPeriode(segment.getFom(), segment.getTom(), segment.getValue()))
            .toList();
    }
}
