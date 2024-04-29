package no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class OmsorgenForKnekkpunkter {

    private LocalDateTimeline<Utfall> tidslinje;

    public OmsorgenForKnekkpunkter(DatoIntervallEntitet periodeTilVurdering) {
        this.tidslinje = new LocalDateTimeline<>(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato(), Utfall.IKKE_OPPFYLT);
    }

    public LocalDateTimeline<Utfall> getTidslinje() {
        return tidslinje;
    }

    public void leggTil(LocalDateTimeline<Utfall> vurdertTidslinje) {
        this.tidslinje = tidslinje.combine(vurdertTidslinje, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
    }
}
