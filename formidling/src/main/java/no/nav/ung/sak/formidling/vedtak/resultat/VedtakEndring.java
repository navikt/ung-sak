package no.nav.ung.sak.formidling.vedtak.resultat;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.VilkårType;

import java.util.Map;

public record VedtakEndring(
    LocalDateTimeline<Map<VilkårType, VilkårEndringType>> vilkårEndringer,
    LocalDateTimeline<DagsatsEndringType> dagsatsEndringer
) {

    public boolean erUendret() {
        return dagsatsEndringer.stream().allMatch(segment -> segment.getValue() == DagsatsEndringType.UENDRET)
            && vilkårEndringer.stream().allMatch(segment -> segment.getValue().values().stream().allMatch(VilkårEndringType.UENDRET::equals));
    }

    @Override
    public String toString() {
        return "vilkårEndringer: " + vilkårEndringer.segmenter() + ", dagsatsEndringer: " + dagsatsEndringer.segmenter();
    }
}
