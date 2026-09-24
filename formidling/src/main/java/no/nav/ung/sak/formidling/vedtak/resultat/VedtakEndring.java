package no.nav.ung.sak.formidling.vedtak.resultat;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.VilkårType;

import java.util.Set;

public record VedtakEndring(
    LocalDateTimeline<Set<VilkårType>> endredeVilkår,
    LocalDateTimeline<Boolean> endretTilkjentYtelse
) {

    public boolean erUendret() {
        return endredeVilkår.isEmpty() && endretTilkjentYtelse.isEmpty();
    }

    @Override
    public String toString() {
        return "endredeVilkår: " + endredeVilkår.toSegments() + ", endretTilkjentYtelse: " + endretTilkjentYtelse.getLocalDateIntervals();
    }
}
