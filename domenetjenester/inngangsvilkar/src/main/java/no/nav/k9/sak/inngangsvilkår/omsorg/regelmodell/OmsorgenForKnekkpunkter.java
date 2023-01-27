package no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class OmsorgenForKnekkpunkter {

    private LocalDateTimeline<Utfall> tidslinje = LocalDateTimeline.empty();

    public OmsorgenForKnekkpunkter(DatoIntervallEntitet periodeTilVurdering) {
        this.tidslinje = new LocalDateTimeline<>(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato(), Utfall.IKKE_OPPFYLT);
    }

    public LocalDateTimeline<Utfall> getTidslinje() {
        return tidslinje;
    }

    public void leggTil(DatoIntervallEntitet periode, Utfall utfall) {
        this.tidslinje = tidslinje.combine(new LocalDateSegment<>(periode.toLocalDateInterval(), utfall), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
    }
}
